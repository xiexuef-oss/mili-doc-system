package com.military.doc.ai.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.ai.config.EmbeddingProperties;
import com.military.doc.ai.util.FileTextExtractor;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.entity.ProjectInputFile;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ProjectInputFileMapper;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.mapper.StandardMapper;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateCategory;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.DocTemplateCategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContextAssemblyService {

    private final ProjectMapper projectMapper;
    private final ProjectInputFileMapper inputFileMapper;
    private final ProjectStageMapper projectStageMapper;
    private final StandardMapper standardMapper;
    private final StandardClauseMapper standardClauseMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocTemplateV2Mapper templateV2Mapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final DocTemplateCategoryMapper templateCategoryMapper;
    private final FileStorageService fileStorageService;
    private final FileTextExtractor fileTextExtractor;
    private final VectorIndexService vectorIndexService;
    private final EmbeddingProperties embeddingProperties;
    private final ProjectMasterDataService masterDataService;
    private SmartTruncationService smartTruncationService; // setter injected

    // 内网部署，模型上下文窗口充足，不再硬编码截断
    // 仅在 assembleContext 总长度超过阈值时按比例截断
    private static final int TOTAL_CONTEXT_WARNING_CHARS = 50000;

    public void setSmartTruncationService(SmartTruncationService svc) {
        this.smartTruncationService = svc;
    }

    public ContextAssemblyService(ProjectMapper projectMapper,
                                  ProjectInputFileMapper inputFileMapper,
                                  ProjectStageMapper projectStageMapper,
                                  StandardMapper standardMapper,
                                  StandardClauseMapper standardClauseMapper,
                                  KnowledgeBaseMapper knowledgeBaseMapper,
                                  DocTemplateV2Mapper templateV2Mapper,
                                  DocTemplateChapterMapper templateChapterMapper,
                                  DocTemplateCategoryMapper templateCategoryMapper,
                                  FileStorageService fileStorageService,
                                  FileTextExtractor fileTextExtractor,
                                  VectorIndexService vectorIndexService,
                                  EmbeddingProperties embeddingProperties,
                                  ProjectMasterDataService masterDataService) {
        this.projectMapper = projectMapper;
        this.inputFileMapper = inputFileMapper;
        this.projectStageMapper = projectStageMapper;
        this.standardMapper = standardMapper;
        this.standardClauseMapper = standardClauseMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.templateV2Mapper = templateV2Mapper;
        this.templateChapterMapper = templateChapterMapper;
        this.templateCategoryMapper = templateCategoryMapper;
        this.fileStorageService = fileStorageService;
        this.fileTextExtractor = fileTextExtractor;
        this.vectorIndexService = vectorIndexService;
        this.embeddingProperties = embeddingProperties;
        this.masterDataService = masterDataService;
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
                ctx.append(text).append("\n\n");
            }
        }

        // 3. Master data
        try {
            Map<String, Object> masterData = masterDataService.getFlattenedData(projectId);
            if (!masterData.isEmpty()) {
                ctx.append("## 项目主数据\n");
                for (Map.Entry<String, Object> entry : masterData.entrySet()) {
                    Object value = entry.getValue();
                    if (value == null) continue;
                    String str = value instanceof String ? (String) value : value.toString();
                    ctx.append("- **").append(entry.getKey()).append("**: ").append(str).append("\n");
                }
                ctx.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to load master data for project {}: {}", projectId, e.getMessage());
        }

        // 4. Applicable standards and clauses (semantic RAG or exact match)
        if (embeddingProperties.isSemanticRagEnabled()) {
            String queryText = buildQueryText(project);
            List<SemanticMatch> clauses = vectorIndexService.searchSimilarClauses(queryText, 20);
            if (!clauses.isEmpty()) {
                ctx.append("## 适用标准条款（语义检索）\n");
                for (SemanticMatch m : clauses) {
                    ctx.append("- **").append(nullToEmpty(m.getClauseNumber())).append("** ")
                        .append(nullToEmpty(m.getClauseTitle()))
                        .append(" [").append(nullToEmpty(m.getStandardCode())).append("]")
                        .append(" (相关性: ").append(String.format("%.2f", m.getSimilarity())).append(")\n");
                    String content = nullToEmpty(m.getClauseContent());
                    if (!content.isBlank()) {
                        ctx.append("  ").append(content).append("\n");
                    }
                }
                ctx.append("\n");
            }
        } else {
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
                            if (!content.isBlank()) {
                                ctx.append("  ").append(content).append("\n");
                            }
                        }
                        ctx.append("\n");
                    }
                }
            }
        }

        // 5. Template library — all active templates with chapter counts
        List<DocTemplateV2> activeTemplates = templateV2Mapper.selectList(
            new LambdaQueryWrapper<DocTemplateV2>()
                .eq(DocTemplateV2::getStatus, "ACTIVE")
                .orderByAsc(DocTemplateV2::getTemplateCode));
        if (!activeTemplates.isEmpty()) {
            ctx.append("## 模板库\n");
            // Load categories for display
            var categories = templateCategoryMapper.selectList(
                new LambdaQueryWrapper<DocTemplateCategory>().eq(DocTemplateCategory::getStatus, "ACTIVE"));
            var catMap = categories.stream().collect(Collectors.toMap(
                DocTemplateCategory::getId, DocTemplateCategory::getCategoryName, (a, b) -> a));

            for (DocTemplateV2 tpl : activeTemplates) {
                String catName = catMap.getOrDefault(tpl.getCategoryId(), "未分类");
                // Count chapters
                Long chapterCount = templateChapterMapper.selectCount(
                    new LambdaQueryWrapper<DocTemplateChapter>()
                        .eq(DocTemplateChapter::getTemplateId, tpl.getId()));
                ctx.append("- **").append(nullToEmpty(tpl.getTemplateCode())).append("** ")
                    .append(nullToEmpty(tpl.getTemplateName()))
                    .append(" [").append(catName).append("]");
                if (chapterCount > 0) {
                    ctx.append(" (").append(chapterCount).append("章)");
                }
                if (tpl.getGjbStandardRef() != null && !tpl.getGjbStandardRef().isBlank()) {
                    ctx.append(" 引用: ").append(tpl.getGjbStandardRef());
                }
                if (tpl.getApplicableProjectType() != null && !tpl.getApplicableProjectType().isBlank()) {
                    ctx.append(" 适用: ").append(tpl.getApplicableProjectType());
                }
                ctx.append("\n");
            }
            ctx.append("\n");
        }

        // 6. Knowledge base (semantic RAG or recent)
        if (embeddingProperties.isSemanticRagEnabled()) {
            String queryText = buildQueryText(project);
            List<SemanticMatch> articles = vectorIndexService.searchSimilarKnowledge(queryText, 5);
            if (!articles.isEmpty()) {
                ctx.append("## 相关知识库文章（语义检索）\n");
                for (SemanticMatch m : articles) {
                    ctx.append("- **").append(nullToEmpty(m.getClauseTitle())).append("** (")
                        .append(nullToEmpty(m.getCategory())).append(")")
                        .append(" 相关性: ").append(String.format("%.2f", m.getSimilarity())).append("\n");
                    String content = nullToEmpty(m.getClauseContent());
                    if (!content.isBlank()) {
                        ctx.append("  ").append(content).append("\n");
                    }
                }
                ctx.append("\n");
            }
        } else {
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
                    if (!content.isBlank()) {
                        ctx.append("  ").append(content).append("\n");
                    }
                }
                ctx.append("\n");
            }
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

    @org.springframework.cache.annotation.Cacheable("standards")
    private List<Standard> findStandards(List<String> codes) {
        if (codes.isEmpty()) return List.of();
        List<Standard> allActive = standardMapper.selectList(
            new LambdaQueryWrapper<Standard>().eq(Standard::getStatus, "ACTIVE"));
        java.util.Set<String> codeSet = new java.util.HashSet<>(codes);
        return allActive.stream()
            .filter(s -> s.getStandardCode() != null
                && codeSet.stream().anyMatch(c -> s.getStandardCode().contains(c)))
            .collect(Collectors.toList());
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    public String assembleContext(Long projectId, Long stageId) {
        String baseContext = assembleContext(projectId);
        if (stageId == null || baseContext.isEmpty()) return baseContext;

        ProjectStage stage = projectStageMapper.selectById(stageId);
        if (stage == null) return baseContext;

        StringBuilder ctx = new StringBuilder(baseContext);
        ctx.append("## 当前阶段补充信息\n");
        ctx.append("- 阶段名称: ").append(nullToEmpty(stage.getStageName())).append("\n");
        ctx.append("- 阶段目标: ").append(nullToEmpty(stage.getStageGoal())).append("\n");
        return ctx.toString();
    }

    private String buildQueryText(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append(nullToEmpty(project.getProjectName())).append(" ");
        sb.append(nullToEmpty(project.getProjectType())).append(" ");
        sb.append(nullToEmpty(project.getApplicableStandards()));
        return sb.toString().trim();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
