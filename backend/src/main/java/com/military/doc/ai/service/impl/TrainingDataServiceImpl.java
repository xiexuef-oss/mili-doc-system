package com.military.doc.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.entity.TrainingExample;
import com.military.doc.ai.mapper.TrainingExampleMapper;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.service.TrainingDataService;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.mapper.DocVersionMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class TrainingDataServiceImpl extends ServiceImpl<TrainingExampleMapper, TrainingExample>
        implements TrainingDataService {

    private static final String SYSTEM_PROMPT =
        "你是一位军工文档撰写专家，精通 GJB 438C、GJB 9001C 等标准。严格按照用户要求输出 Markdown 格式的文档正文。";

    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final FileStorageService fileStorageService;
    private final DocVersionMapper docVersionMapper;
    private final DocCatalogMapper docCatalogMapper;
    private final ProjectMapper projectMapper;
    private final ObjectMapper objectMapper;

    public TrainingDataServiceImpl(ContextAssemblyService contextAssemblyService,
                                   PromptTemplateService promptTemplateService,
                                   FileStorageService fileStorageService,
                                   DocVersionMapper docVersionMapper,
                                   DocCatalogMapper docCatalogMapper,
                                   ProjectMapper projectMapper,
                                   ObjectMapper objectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.fileStorageService = fileStorageService;
        this.docVersionMapper = docVersionMapper;
        this.docCatalogMapper = docCatalogMapper;
        this.projectMapper = projectMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TrainingExample collect(Long docFileId, Long projectId, Long catalogId, Long userId) {
        // 1. Read the latest DocVersion content
        List<DocVersion> versions = docVersionMapper.selectList(
            new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocFileId, docFileId)
                .orderByDesc(DocVersion::getCreatedAt)
                .last("LIMIT 1")
        );

        String completion = "";
        if (!versions.isEmpty() && versions.get(0).getFileObjectId() != null) {
            completion = readFileContent(versions.get(0).getFileObjectId());
        }

        // 2. Reconstruct prompt from context + template
        String context = contextAssemblyService.assembleContext(projectId);

        // Add catalog entry details
        if (catalogId != null) {
            DocCatalog catalog = docCatalogMapper.selectById(catalogId);
            if (catalog != null) {
                context += "\n## 待生成文档\n";
                context += "- 文档编号: " + n2e(catalog.getDocCode()) + "\n";
                context += "- 文档名称: " + n2e(catalog.getDocName()) + "\n";
                context += "- 文档类型: " + n2e(catalog.getDocType()) + "\n";
            }
        }

        Project project = projectMapper.selectById(projectId);
        if (project != null && catalogId != null) {
            DocCatalog catalog = docCatalogMapper.selectById(catalogId);
            context += "\n## 撰写任务\n";
            context += "请为「" + n2e(project.getProjectName()) + "」项目撰写「"
                + (catalog != null ? n2e(catalog.getDocName()) : "") + "」的完整初稿。\n";
        }

        String prompt = promptTemplateService.render("draft-generation",
            Map.of("context", context));

        // 3. Store training example
        TrainingExample example = new TrainingExample();
        example.setProjectId(projectId);
        example.setDocFileId(docFileId);
        example.setCatalogId(catalogId);
        example.setPrompt(prompt);
        example.setCompletion(completion);
        example.setQuality("PENDING_REVIEW");
        example.setCreatedBy(userId);
        example.setCreatedAt(LocalDateTime.now());
        save(example);

        log.info("Collected training example id={}, docFileId={}, completion {} chars",
            example.getId(), docFileId, completion.length());
        return example;
    }

    @Override
    public TrainingExample approve(Long id) {
        TrainingExample example = getById(id);
        if (example != null) {
            example.setQuality("APPROVED");
            updateById(example);
            log.info("Training example {} approved", id);
        }
        return example;
    }

    @Override
    public TrainingExample reject(Long id) {
        TrainingExample example = getById(id);
        if (example != null) {
            example.setQuality("REJECTED");
            updateById(example);
            log.info("Training example {} rejected", id);
        }
        return example;
    }

    @Override
    public Page<TrainingExample> list(String quality, int page, int size) {
        LambdaQueryWrapper<TrainingExample> wrapper =
            new LambdaQueryWrapper<TrainingExample>()
                .orderByDesc(TrainingExample::getCreatedAt);
        if (quality != null && !quality.isEmpty()) {
            wrapper.eq(TrainingExample::getQuality, quality);
        }
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public String exportJsonl(String quality) {
        List<TrainingExample> examples = list(
            new LambdaQueryWrapper<TrainingExample>()
                .eq(TrainingExample::getQuality, quality != null ? quality : "APPROVED")
                .orderByAsc(TrainingExample::getCreatedAt)
        );

        StringBuilder sb = new StringBuilder();
        for (TrainingExample ex : examples) {
            Map<String, Object> record = new LinkedHashMap<>();
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", ex.getPrompt() != null ? ex.getPrompt() : ""),
                Map.of("role", "assistant", "content", ex.getCompletion() != null ? ex.getCompletion() : "")
            );
            record.put("messages", messages);
            try {
                sb.append(objectMapper.writeValueAsString(record)).append("\n");
            } catch (Exception e) {
                log.warn("Failed to serialize training example {}", ex.getId());
            }
        }

        log.info("Exported {} training examples as JSONL, {} chars", examples.size(), sb.length());
        return sb.toString();
    }

    private String readFileContent(String fileObjectId) {
        try (InputStream is = fileStorageService.download(fileObjectId);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to read file content: {}", fileObjectId, e);
            return "";
        }
    }

    private String n2e(String s) {
        return s == null ? "" : s;
    }
}
