package com.military.doc.ai.service;

import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class DraftGenerationService {

    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final DocCatalogMapper docCatalogMapper;
    private final ProjectMapper projectMapper;

    public DraftGenerationService(ContextAssemblyService contextAssemblyService,
                                   PromptTemplateService promptTemplateService,
                                   LlmClient llmClient,
                                   DocCatalogMapper docCatalogMapper,
                                   ProjectMapper projectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.docCatalogMapper = docCatalogMapper;
        this.projectMapper = projectMapper;
    }

    public String generate(Long projectId, Long catalogId) {
        String context = assembleDraftContext(projectId, catalogId);
        if (context.isEmpty()) return "";

        String userPrompt = promptTemplateService.render("draft-generation",
            Map.of("context", context));
        String systemPrompt = "你是一位军工文档撰写专家，精通 GJB 438C、GJB 9001C 等标准。严格按照用户要求输出 Markdown 格式的文档正文。";

        log.info("Draft generation: catalogId={}, prompt {} chars", catalogId, userPrompt.length());
        return llmClient.chat(systemPrompt, userPrompt);
    }

    public void generateStream(Long projectId, Long catalogId, Consumer<String> onChunk) {
        String context = assembleDraftContext(projectId, catalogId);
        if (context.isEmpty()) return;

        String userPrompt = promptTemplateService.render("draft-generation",
            Map.of("context", context));
        String systemPrompt = "你是一位军工文档撰写专家，精通 GJB 438C、GJB 9001C 等标准。严格按照用户要求输出 Markdown 格式的文档正文。";

        log.info("Draft generation stream: catalogId={}, prompt {} chars", catalogId, userPrompt.length());
        llmClient.chatStream(systemPrompt, userPrompt, onChunk);
    }

    private String assembleDraftContext(Long projectId, Long catalogId) {
        StringBuilder ctx = new StringBuilder();

        // Project-level context
        String projectContext = contextAssemblyService.assembleContext(projectId);
        ctx.append(projectContext);

        // Catalog entry details
        DocCatalog catalog = docCatalogMapper.selectById(catalogId);
        if (catalog != null) {
            ctx.append("\n## 待生成文档\n");
            ctx.append("- 文档编号: ").append(nullToEmpty(catalog.getDocCode())).append("\n");
            ctx.append("- 文档名称: ").append(nullToEmpty(catalog.getDocName())).append("\n");
            ctx.append("- 文档类型: ").append(nullToEmpty(catalog.getDocType())).append("\n");
            ctx.append("- 是否必须: ").append(Boolean.TRUE.equals(catalog.getRequiredFlag()) ? "是" : "否").append("\n");
        }

        Project project = projectMapper.selectById(projectId);
        if (project != null && project.getProjectType() != null) {
            String docTypeName = switch (catalog != null ? nullToEmpty(catalog.getDocType()) : "") {
                case "DESIGN_DOC" -> "设计文档";
                case "TEST_DOC" -> "测试文档";
                case "MANAGEMENT_DOC" -> "管理文档";
                case "QUALITY_DOC" -> "质量文档";
                case "REVIEW_DOC" -> "评审文档";
                default -> catalog != null ? nullToEmpty(catalog.getDocType()) : "文档";
            };
            ctx.append("\n## 撰写任务\n");
            ctx.append("请为「").append(project.getProjectName()).append("」项目撰写「")
                .append(catalog != null ? nullToEmpty(catalog.getDocName()) : "")
                .append("」").append(docTypeName).append("的完整初稿。\n");
        }

        return ctx.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
