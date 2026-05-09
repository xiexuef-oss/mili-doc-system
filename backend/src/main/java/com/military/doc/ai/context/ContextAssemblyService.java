package com.military.doc.ai.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.ai.util.FileTextExtractor;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.entity.ProjectInputFile;
import com.military.doc.modules.project.mapper.ProjectInputFileMapper;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.mapper.StandardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContextAssemblyService {

    private final ProjectMapper projectMapper;
    private final ProjectInputFileMapper inputFileMapper;
    private final StandardMapper standardMapper;
    private final StandardClauseMapper standardClauseMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final FileStorageService fileStorageService;
    private final FileTextExtractor fileTextExtractor;

    private static final int MAX_CLAUSE_LENGTH = 500;
    private static final int MAX_INPUT_FILE_LENGTH = 3000;

    public ContextAssemblyService(ProjectMapper projectMapper,
                                  ProjectInputFileMapper inputFileMapper,
                                  StandardMapper standardMapper,
                                  StandardClauseMapper standardClauseMapper,
                                  KnowledgeBaseMapper knowledgeBaseMapper,
                                  FileStorageService fileStorageService,
                                  FileTextExtractor fileTextExtractor) {
        this.projectMapper = projectMapper;
        this.inputFileMapper = inputFileMapper;
        this.standardMapper = standardMapper;
        this.standardClauseMapper = standardClauseMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.fileStorageService = fileStorageService;
        this.fileTextExtractor = fileTextExtractor;
    }

    public String assembleContext(Long projectId) {
        StringBuilder ctx = new StringBuilder();

        // 1. Project info
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            log.warn("Project not found: {}", projectId);
            return "";
        }
        ctx.append("## 项目信息\n");
        ctx.append("- 项目编号: ").append(project.getProjectCode()).append("\n");
        ctx.append("- 项目名称: ").append(project.getProjectName()).append("\n");
        ctx.append("- 项目类型: ").append(project.getProjectType()).append("\n");
        ctx.append("- 密级: ").append(project.getSecurityLevel()).append("\n");
        ctx.append("- 适用标准: ").append(nullToEmpty(project.getApplicableStandards())).append("\n\n");

        // 2. Input files
        List<ProjectInputFile> inputFiles = inputFileMapper.selectList(
            new LambdaQueryWrapper<ProjectInputFile>()
                .eq(ProjectInputFile::getProjectId, projectId)
                .orderByAsc(ProjectInputFile::getUploadedAt)
        );
        if (!inputFiles.isEmpty()) {
            ctx.append("## 输入文件\n");
            for (ProjectInputFile f : inputFiles) {
                ctx.append("### ").append(f.getFileName())
                    .append(" (").append(nullToEmpty(f.getInputType())).append(")\n");
                String text = extractFileText(f.getFileObjectId(), f.getFileName());
                if (text.length() > MAX_INPUT_FILE_LENGTH) {
                    text = text.substring(0, MAX_INPUT_FILE_LENGTH) + "\n...(已截断)";
                }
                ctx.append(text).append("\n\n");
            }
        }

        // 3. Applicable standards and clauses
        String standardsStr = project.getApplicableStandards();
        if (standardsStr != null && !standardsStr.isBlank()) {
            List<String> standardCodes = parseStandardCodes(standardsStr);
            List<Standard> standards = findStandards(standardCodes);
            if (!standards.isEmpty()) {
                ctx.append("## 适用标准条款\n");
                for (Standard std : standards) {
                    ctx.append("### ").append(std.getStandardCode())
                        .append(" ").append(nullToEmpty(std.getStandardName())).append("\n");
                    List<StandardClause> clauses = standardClauseMapper.selectList(
                        new LambdaQueryWrapper<StandardClause>()
                            .eq(StandardClause::getStandardId, std.getId())
                            .orderByAsc(StandardClause::getOrderNum)
                    );
                    for (StandardClause clause : clauses) {
                        ctx.append("- **").append(clause.getClauseNumber()).append("** ")
                            .append(nullToEmpty(clause.getClauseTitle())).append("\n");
                        String content = nullToEmpty(clause.getClauseContent());
                        if (content.length() > MAX_CLAUSE_LENGTH) {
                            content = content.substring(0, MAX_CLAUSE_LENGTH) + "...";
                        }
                        if (!content.isBlank()) {
                            ctx.append("  ").append(content).append("\n");
                        }
                    }
                    ctx.append("\n");
                }
            }
        }

        // 4. Knowledge base
        List<KnowledgeBase> kbArticles = knowledgeBaseMapper.selectList(
            new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getStatus, "ACTIVE")
                .orderByDesc(KnowledgeBase::getCreatedAt)
                .last("LIMIT 5")
        );
        if (!kbArticles.isEmpty()) {
            ctx.append("## 相关知识库文章\n");
            for (KnowledgeBase kb : kbArticles) {
                ctx.append("- **").append(nullToEmpty(kb.getTitle())).append("** (")
                    .append(nullToEmpty(kb.getCategory())).append(")\n");
                String content = nullToEmpty(kb.getContent());
                if (content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }
                if (!content.isBlank()) {
                    ctx.append("  ").append(content).append("\n");
                }
            }
            ctx.append("\n");
        }

        String result = ctx.toString();
        log.info("Assembled context for project {}: {} chars", projectId, result.length());
        return result;
    }

    private String extractFileText(String fileObjectId, String filename) {
        if (fileObjectId == null || fileObjectId.isEmpty()) return "";
        try (InputStream is = fileStorageService.download(fileObjectId)) {
            byte[] bytes = is.readAllBytes();
            String ext = getExtension(filename);
            return fileTextExtractor.extract(bytes, ext);
        } catch (IOException e) {
            log.warn("Failed to extract text from file {}: {}", fileObjectId, e.getMessage());
            return "[无法提取文件内容]";
        }
    }

    private List<String> parseStandardCodes(String standardsStr) {
        return Arrays.stream(standardsStr.split("[,;，；\\s]+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private List<Standard> findStandards(List<String> codes) {
        if (codes.isEmpty()) return List.of();
        List<Standard> results = new ArrayList<>();
        for (String code : codes) {
            List<Standard> found = standardMapper.selectList(
                new LambdaQueryWrapper<Standard>()
                    .like(Standard::getStandardCode, code)
                    .eq(Standard::getStatus, "ACTIVE")
                    .last("LIMIT 1")
            );
            results.addAll(found);
        }
        return results;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
