package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.util.FileTextExtractor;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.entity.ProjectInputFile;
import com.military.doc.modules.project.mapper.ProjectInputFileMapper;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class MasterDataExtractionService {

    private final ProjectInputFileMapper inputFileMapper;
    private final ProjectMapper projectMapper;
    private final FileStorageService fileStorageService;
    private final FileTextExtractor fileTextExtractor;
    private final LlmClient llmClient;
    private final ProjectMasterDataService masterDataService;
    private final ObjectMapper objectMapper;

    private static final int PER_FILE_LIMIT = 4000;

    public MasterDataExtractionService(ProjectInputFileMapper inputFileMapper,
                                        ProjectMapper projectMapper,
                                        FileStorageService fileStorageService,
                                        FileTextExtractor fileTextExtractor,
                                        LlmClient llmClient,
                                        ProjectMasterDataService masterDataService,
                                        ObjectMapper objectMapper) {
        this.inputFileMapper = inputFileMapper;
        this.projectMapper = projectMapper;
        this.fileStorageService = fileStorageService;
        this.fileTextExtractor = fileTextExtractor;
        this.llmClient = llmClient;
        this.masterDataService = masterDataService;
        this.objectMapper = objectMapper;
    }

    /**
     * Extract master data from all input files of a project.
     */
    public Map<String, Object> extractFromInputFiles(Long projectId) {
        List<ProjectInputFile> files = inputFileMapper.selectList(
            new LambdaQueryWrapper<ProjectInputFile>()
                .eq(ProjectInputFile::getProjectId, projectId)
                .eq(ProjectInputFile::getDeleted, 0)
        );

        if (files.isEmpty()) {
            log.warn("No input files found for project {}", projectId);
            return Collections.emptyMap();
        }

        StringBuilder allText = new StringBuilder();
        for (ProjectInputFile f : files) {
            String text = extractFileText(f);
            if (!text.isEmpty()) {
                allText.append("=== ").append(f.getFileName())
                    .append(" (").append(nullToEmpty(f.getInputType())).append(") ===\n");
                if (allText.length() + text.length() > 8000) {
                    allText.append(text.substring(0, Math.min(text.length(), 4000))).append("\n...(截断)\n\n");
                } else {
                    allText.append(text).append("\n\n");
                }
            }
        }

        if (allText.isEmpty()) {
            log.warn("No extractable text from input files for project {}", projectId);
            return Collections.emptyMap();
        }

        Project project = projectMapper.selectById(projectId);
        String projectContext = project != null
            ? "项目: " + nullToEmpty(project.getProjectName()) + " (" + nullToEmpty(project.getProjectType()) + ")"
            : "";

        Map<String, Object> extracted = callLlmForExtraction(allText.toString(), projectContext);
        if (!extracted.isEmpty()) {
            masterDataService.saveOrUpdateMasterData(projectId, extracted, 0L);
            log.info("Saved extracted master data for project {}: {} keys", projectId, extracted.size());
        }
        return extracted;
    }

    /**
     * Extract master data incrementally from a single new file.
     */
    public Map<String, Object> extractFromSingleFile(Long projectId, Long fileId) {
        ProjectInputFile file = inputFileMapper.selectById(fileId);
        if (file == null) {
            log.warn("Input file not found: {}", fileId);
            return Collections.emptyMap();
        }

        String text = extractFileText(file);
        if (text.isEmpty()) return Collections.emptyMap();

        Project project = projectMapper.selectById(projectId);
        String projectContext = project != null
            ? "项目: " + nullToEmpty(project.getProjectName()) + " (" + nullToEmpty(project.getProjectType()) + ")"
            : "";

        // Also load existing data and send as context so LLM can blend
        Map<String, Object> existing = masterDataService.getFlattenedData(projectId);
        String existingJson = "{}";
        try {
            existingJson = objectMapper.writeValueAsString(existing);
        } catch (JsonProcessingException e) {
            // ignore
        }

        String prompt = "已有主数据:\n" + existingJson + "\n\n新文件内容:\n" + text;
        Map<String, Object> extracted = callLlmForExtraction(prompt, projectContext);
        if (!extracted.isEmpty()) {
            // Merge with existing
            Map<String, Object> merged = new LinkedHashMap<>(existing);
            merged.putAll(extracted);
            masterDataService.saveOrUpdateMasterData(projectId, merged, 0L);
        }
        return extracted;
    }

    private Map<String, Object> callLlmForExtraction(String fileText, String projectContext) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = projectContext + "\n\n" + fileText;

        log.info("Master data extraction: text {} chars", userPrompt.length());
        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            return parseExtractionResponse(response);
        } catch (Exception e) {
            log.error("LLM extraction failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> parseExtractionResponse(String response) {
        // Extract JSON block from markdown code fences
        String json = response;
        int start = response.indexOf("```json");
        int end = response.lastIndexOf("```");
        if (start >= 0 && end > start) {
            json = response.substring(start + 7, end).trim();
        } else {
            start = response.indexOf('{');
            end = response.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = response.substring(start, end + 1);
            }
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM extraction response as JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String buildSystemPrompt() {
        return """
            你是一位军工项目文档数据提取专家。从给定的项目输入文件(合同、技术要求、任务书等)中提取结构化主数据。

            请输出严格的JSON格式，包含以下四个字段(没有数据的字段输出空对象/数组):

            {
              "equipmentInfo": {
                "equipmentName": "产品名称",
                "equipmentModel": "产品型号",
                "equipmentCode": "产品代号",
                "taskBookCode": "任务书编号",
                "contractCode": "合同编号",
                "developerUnit": "研制单位",
                "manufacturerUnit": "承制单位",
                "chiefEngineerUnit": "总师单位",
                "chiefEngineer": "总师/主任设计师",
                "projectManager": "项目负责人",
                "projectName": "项目名称",
                "securityLevel": "密级"
              },
              "tacticalIndicators": [
                {
                  "indicatorName": "指标名称",
                  "required": "要求值(含量纲如≤0.5m)",
                  "actual": "实测值(如已知)",
                  "unit": "单位",
                  "conclusion": "满足/基本满足/不满足",
                  "remark": "备注"
                }
              ],
              "productTree": [
                {
                  "level": "系统/分系统/设备/组件",
                  "productName": "产品名称",
                  "productCode": "产品代号",
                  "parentProduct": "上级产品代号",
                  "quantity": "数量",
                  "remark": "备注"
                }
              ],
              "milestones": [
                {
                  "stageCode": "阶段代码(L/F/C/S/D/P/N)",
                  "name": "节点名称",
                  "plannedDate": "2025-12-31",
                  "actualDate": "实际完成日期(如已知)",
                  "keyDeliverables": "关键交付物",
                  "status": "未开始/进行中/已完成"
                }
              ]
            }

            要求:
            1. 只提取文件中明确出现的数据，没有的字段使用 null 或空字符串 ""，不要编造
            2. 数值和日期保持原文原样，不要编造数据
            3. 人名、单位名称使用文件中的原始表述，保留全称
            4. productTree的level使用中文: 系统/分系统/设备/组件
            5. tacticalIndicators中value字段包含量纲(如 "≤0.5m")，存入required字段
            6. 团队成员角色使用标准名称: 总设计师/副总设计师/主任设计师/主管设计师/设计师/质量师/标准化师/工艺师
            7. 只输出JSON，不要添加任何解释文字
            """;
    }

    private String extractFileText(ProjectInputFile file) {
        if (file.getFileObjectId() == null || file.getFileObjectId().isEmpty()) return "";
        try (InputStream is = fileStorageService.download(file.getFileObjectId())) {
            byte[] bytes = is.readAllBytes();
            String ext = getExtension(file.getFileName());
            // FileTextExtractor now has built-in OCR fallback for scanned PDFs
            return fileTextExtractor.extract(bytes, ext);
        } catch (Exception e) {
            log.warn("Failed to extract text from {}: {}", file.getFileName(), e.getMessage());
            return "";
        }
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
