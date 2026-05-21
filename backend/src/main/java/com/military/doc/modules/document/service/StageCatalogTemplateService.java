package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.system.entity.SysDict;
import com.military.doc.modules.system.mapper.SysDictMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 基于《军工产品研制技术文件编写指南》(梅文华等编, 国防工业出版社, 2010)
 * 按研制阶段(论证L/方案F/工程研制C/设计定型S/生产定型D/批生产P)
 * 和 GJB 5882-2006 技术文件分类体系，生成各阶段文档目录清单。
 *
 * 数据来源: 该书表1.1"研制阶段形成的主要文字类文件"及第1.3节。
 * V = 应当编制, 人 = 根据需要编制, O = 适应性修改。
 */
@Slf4j
@Service
public class StageCatalogTemplateService {

    private final SysDictMapper dictMapper;
    private final DocCatalogMapper catalogMapper;

    public StageCatalogTemplateService(SysDictMapper dictMapper, DocCatalogMapper catalogMapper) {
        this.dictMapper = dictMapper;
        this.catalogMapper = catalogMapper;
    }

    /** 阶段文档定义 */
    private record StageDoc(String name, String category, boolean required) {}

    /** 论证阶段 (L) — 主要任务: 论证战术技术指标、总体技术方案 */
    private static final List<StageDoc> DOCS_L = List.of(
        new StageDoc("研制立项综合论证报告", "ACHIEVEMENT", true),
        new StageDoc("招标书", "PROCESS", true),
        new StageDoc("投标书", "PROCESS", true),
        new StageDoc("可行性论证报告", "ACHIEVEMENT", true),
        new StageDoc("生产性分析报告(论证阶段)", "MANUFACTURE", true),
        new StageDoc("研制总要求", "ACHIEVEMENT", true),
        new StageDoc("研制总要求论证工作报告", "ACHIEVEMENT", true)
    );

    /** 方案阶段 (F) — 主要任务: 系统方案设计、关键技术攻关、形成研制任务书 */
    private static final List<StageDoc> DOCS_F = List.of(
        new StageDoc("工作分解结构", "PROCESS", true),
        new StageDoc("研制合同", "PROCESS", true),
        new StageDoc("工作说明", "PROCESS", true),
        new StageDoc("技术规范", "STANDARDIZE", true),
        new StageDoc("系统规范(A类规范)", "STANDARDIZE", true),
        new StageDoc("研制规范(B类规范)", "STANDARDIZE", true),
        new StageDoc("研制计划", "PROCESS", true),
        new StageDoc("生产性分析报告(方案阶段)", "MANUFACTURE", true),
        new StageDoc("研制方案", "ACHIEVEMENT", true),
        new StageDoc("技术状态管理计划", "PROCESS", true),
        new StageDoc("接口控制文件", "PROCESS", true),
        new StageDoc("试验与评定总计划", "PROCESS", true),
        new StageDoc("研制任务书", "ACHIEVEMENT", true),
        new StageDoc("工艺总方案", "MANUFACTURE", true),
        new StageDoc("设计计算报告", "PROCESS", false),
        new StageDoc("特性分析报告", "PROCESS", false),
        new StageDoc("标准化大纲", "STANDARDIZE", true),
        new StageDoc("质量保证大纲(质量计划)", "QUALITY", true),
        new StageDoc("风险管理计划", "RISK", true),
        new StageDoc("风险分析报告", "RISK", true),
        new StageDoc("可靠性工作计划(可靠性大纲)", "RELIABILITY", true),
        new StageDoc("维修性工作计划(维修性大纲)", "MAINTAINABILITY", true),
        new StageDoc("测试性工作计划(测试性大纲)", "TESTABILITY", true),
        new StageDoc("综合保障计划", "SUPPORTABILITY", true),
        new StageDoc("安全性大纲", "SAFETY", true),
        new StageDoc("软件开发计划", "SOFTWARE", true),
        new StageDoc("软件配置管理计划", "SOFTWARE", true),
        new StageDoc("软件质量保证计划", "SOFTWARE", true),
        new StageDoc("软件需求规格说明", "SOFTWARE", true),
        new StageDoc("软件设计说明", "SOFTWARE", false),
        new StageDoc("电磁兼容性大纲", "EMC", true),
        new StageDoc("环境适应性大纲", "ENVIRONMENT", true),
        new StageDoc("人机工程要求", "ERGONOMICS", true)
    );

    /** 工程研制阶段 (C) — 主要任务: 设计、试制、试验 */
    private static final List<StageDoc> DOCS_C = List.of(
        new StageDoc("工作分解结构", "PROCESS", true),
        new StageDoc("研制合同", "PROCESS", true),
        new StageDoc("工作说明", "PROCESS", true),
        new StageDoc("技术规范", "STANDARDIZE", true),
        new StageDoc("研制计划", "PROCESS", true),
        new StageDoc("技术状态管理计划", "PROCESS", true),
        new StageDoc("接口控制文件", "PROCESS", true),
        new StageDoc("试验与评定总计划", "PROCESS", true),
        new StageDoc("研制任务书", "ACHIEVEMENT", true),
        new StageDoc("工艺总方案", "MANUFACTURE", true),
        new StageDoc("工艺规范(D类规范)", "MANUFACTURE", true),
        new StageDoc("材料规范(E类规范)", "MANUFACTURE", true),
        new StageDoc("产品规范(C类规范)", "STANDARDIZE", true),
        new StageDoc("技术说明书", "ACHIEVEMENT", true),
        new StageDoc("使用维护说明书", "ACHIEVEMENT", false),
        new StageDoc("设计计算报告", "PROCESS", true),
        new StageDoc("特性分析报告", "PROCESS", false),
        new StageDoc("生产性分析报告(工程研制阶段)", "MANUFACTURE", true),
        new StageDoc("研制试验大纲", "PROCESS", true),
        new StageDoc("研制试验报告", "PROCESS", true),
        new StageDoc("验收测试规范", "STANDARDIZE", true),
        new StageDoc("验收测试程序", "PROCESS", true),
        new StageDoc("工艺评审报告", "MANUFACTURE", true),
        new StageDoc("质量分析报告", "QUALITY", true),
        new StageDoc("可靠性工作计划(可靠性大纲)", "RELIABILITY", true),
        new StageDoc("可靠性模型", "RELIABILITY", false),
        new StageDoc("可靠性分配", "RELIABILITY", false),
        new StageDoc("FMEA报告", "RELIABILITY", false),
        new StageDoc("维修性工作计划(维修性大纲)", "MAINTAINABILITY", true),
        new StageDoc("测试性工作计划(测试性大纲)", "TESTABILITY", true),
        new StageDoc("综合保障计划", "SUPPORTABILITY", true),
        new StageDoc("安全性大纲", "SAFETY", true),
        new StageDoc("软件开发计划", "SOFTWARE", true),
        new StageDoc("软件配置管理计划", "SOFTWARE", true),
        new StageDoc("软件质量保证计划", "SOFTWARE", true),
        new StageDoc("软件需求规格说明", "SOFTWARE", true),
        new StageDoc("软件设计说明", "SOFTWARE", true),
        new StageDoc("数据库设计说明", "SOFTWARE", false),
        new StageDoc("软件测试计划", "SOFTWARE", true),
        new StageDoc("软件测试说明", "SOFTWARE", true),
        new StageDoc("软件测试报告", "SOFTWARE", true),
        new StageDoc("软件产品规格说明", "SOFTWARE", false),
        new StageDoc("软件版本说明", "SOFTWARE", false),
        new StageDoc("软件用户手册", "SOFTWARE", false),
        new StageDoc("计算机操作手册", "SOFTWARE", false),
        new StageDoc("电磁兼容性设计方栗", "EMC", false),
        new StageDoc("环境适应性设计报告", "ENVIRONMENT", false),
        new StageDoc("人机工程系统分析报告", "ERGONOMICS", false)
    );

    /** 设计定型阶段 (S) — 主要任务: 全面考核产品性能和使用要求 */
    private static final List<StageDoc> DOCS_S = List.of(
        new StageDoc("设计定型试验申请报告", "PROCESS", true),
        new StageDoc("设计定型试验大纲", "PROCESS", true),
        new StageDoc("设计定型试验大纲(部队试验)", "PROCESS", true),
        new StageDoc("设计定型试验大纲编制说明", "PROCESS", true),
        new StageDoc("设计定型试验报告", "PROCESS", true),
        new StageDoc("设计定型试验报告(部队试验)", "PROCESS", true),
        new StageDoc("重大技术问题的技术攻关报告", "PROCESS", true),
        new StageDoc("质量问题报告", "QUALITY", true),
        new StageDoc("价值工程和成本分析报告", "ACHIEVEMENT", true),
        new StageDoc("质量分析报告", "QUALITY", true),
        new StageDoc("可靠性分析评价", "RELIABILITY", true),
        new StageDoc("维修性评估报告", "MAINTAINABILITY", true),
        new StageDoc("测试性评估报告", "TESTABILITY", true),
        new StageDoc("保障性评估报告", "SUPPORTABILITY", true),
        new StageDoc("安全性评价报告", "SAFETY", true),
        new StageDoc("标准化审查报告", "STANDARDIZE", true),
        new StageDoc("研制总结", "ACHIEVEMENT", true),
        new StageDoc("设计定型申请报告", "PROCESS", true),
        new StageDoc("设计定型审查意见书", "PROCESS", true),
        new StageDoc("总体单位对设计定型的意见", "PROCESS", true),
        new StageDoc("军事代表机构对设计定型的意见", "PROCESS", true),
        new StageDoc("产品规范(C类规范)", "STANDARDIZE", true),
        new StageDoc("工艺规范(D类规范)", "MANUFACTURE", true),
        new StageDoc("材料规范(E类规范)", "MANUFACTURE", true),
        new StageDoc("技术说明书", "ACHIEVEMENT", true),
        new StageDoc("使用维护说明书", "ACHIEVEMENT", true),
        new StageDoc("工艺设计工作总结", "MANUFACTURE", true),
        new StageDoc("工艺评审报告", "MANUFACTURE", true),
        new StageDoc("工艺总结", "MANUFACTURE", true),
        new StageDoc("标准化工作报告", "STANDARDIZE", true),
        new StageDoc("部队试用申请报告", "PROCESS", true),
        new StageDoc("部队试用大纲", "PROCESS", true),
        new StageDoc("部队试用大纲编制说明", "PROCESS", false),
        new StageDoc("部队试用报告", "PROCESS", true),
        new StageDoc("技术状态更改建议", "PROCESS", true),
        new StageDoc("偏离(超差)申请", "PROCESS", true),
        new StageDoc("技术通报", "PROCESS", true),
        new StageDoc("设计定型录像片解说词", "ACHIEVEMENT", false),
        new StageDoc("软件定型测评大纲", "SOFTWARE", true),
        new StageDoc("软件定型测评报告", "SOFTWARE", true),
        new StageDoc("软件研制总结报告", "SOFTWARE", true),
        new StageDoc("软件配置管理报告", "SOFTWARE", true),
        new StageDoc("软件质量保证报告", "SOFTWARE", true),
        new StageDoc("软件用户手册", "SOFTWARE", true),
        new StageDoc("软件版本说明", "SOFTWARE", true),
        new StageDoc("环境鉴定试验报告", "ENVIRONMENT", true),
        new StageDoc("电磁兼容性试验报告", "EMC", true),
        new StageDoc("人机工程评估报告", "ERGONOMICS", true)
    );

    /** 生产定型阶段 (D) — 主要任务: 考核批量生产条件和质量稳定性 */
    private static final List<StageDoc> DOCS_D = List.of(
        new StageDoc("生产定型试验申请报告", "PROCESS", true),
        new StageDoc("生产定型试验大纲", "PROCESS", true),
        new StageDoc("生产定型试验报告", "PROCESS", true),
        new StageDoc("价值工程分析和成本核算报告", "ACHIEVEMENT", true),
        new StageDoc("生产性分析报告(生产定型阶段)", "MANUFACTURE", true),
        new StageDoc("试生产总结", "MANUFACTURE", true),
        new StageDoc("工艺和生产条件考核报告", "MANUFACTURE", true),
        new StageDoc("生产定型录像片解说词", "ACHIEVEMENT", false),
        new StageDoc("军事代表机构对生产定型的意见", "PROCESS", true),
        new StageDoc("生产定型申请报告", "PROCESS", true),
        new StageDoc("生产定型审查意见书", "PROCESS", true),
        new StageDoc("部队试用申请报告", "PROCESS", true),
        new StageDoc("部队试用大纲", "PROCESS", true),
        new StageDoc("部队试用报告", "PROCESS", true),
        new StageDoc("产品质量管理报告", "QUALITY", true),
        new StageDoc("质量分析报告", "QUALITY", true),
        new StageDoc("配套产品质量和定点供应情况报告", "QUALITY", true),
        new StageDoc("可靠性评估报告", "RELIABILITY", true),
        new StageDoc("维修性评估报告", "MAINTAINABILITY", true),
        new StageDoc("测试性评估报告", "TESTABILITY", true),
        new StageDoc("保障性评估报告", "SUPPORTABILITY", true),
        new StageDoc("安全性评价报告", "SAFETY", true),
        new StageDoc("工艺标准化审查报告", "STANDARDIZE", true),
        new StageDoc("工艺标准化大纲(工艺标准化综合要求)", "STANDARDIZE", true),
        new StageDoc("标准化工作报告", "STANDARDIZE", true),
        new StageDoc("标准化审查报告", "STANDARDIZE", true),
        new StageDoc("工艺设计工作总结", "MANUFACTURE", true),
        new StageDoc("工艺评审报告", "MANUFACTURE", true),
        new StageDoc("工艺总结", "MANUFACTURE", true),
        new StageDoc("产品规范(C类规范)", "STANDARDIZE", true),
        new StageDoc("工艺规范(D类规范)", "MANUFACTURE", true),
        new StageDoc("材料规范(E类规范)", "MANUFACTURE", true),
        new StageDoc("技术说明书", "ACHIEVEMENT", true),
        new StageDoc("使用维护说明书", "ACHIEVEMENT", true),
        new StageDoc("质量管理报告", "QUALITY", true),
        new StageDoc("军事代表机构质量监督报告", "QUALITY", true),
        new StageDoc("风险管理计划", "RISK", true),
        new StageDoc("风险分析报告", "RISK", true),
        new StageDoc("软件定型测评大纲", "SOFTWARE", true),
        new StageDoc("软件定型测评报告", "SOFTWARE", true),
        new StageDoc("软件研制总结报告", "SOFTWARE", true),
        new StageDoc("软件配置管理报告", "SOFTWARE", true),
        new StageDoc("软件质量保证报告", "SOFTWARE", true),
        new StageDoc("软件用户手册", "SOFTWARE", true)
    );

    /** 批生产阶段 (P) — 主要任务: 稳定批量生产、批次质量管理 */
    private static final List<StageDoc> DOCS_P = List.of(
        new StageDoc("批次质量记录", "QUALITY", true),
        new StageDoc("质量分析报告", "QUALITY", true),
        new StageDoc("产品质量管理报告", "QUALITY", true),
        new StageDoc("配套产品质量和定点供应情况报告", "QUALITY", true),
        new StageDoc("产品规范(C类规范)", "STANDARDIZE", true),
        new StageDoc("工艺规范(D类规范)", "MANUFACTURE", true),
        new StageDoc("材料规范(E类规范)", "MANUFACTURE", true),
        new StageDoc("技术说明书", "ACHIEVEMENT", true),
        new StageDoc("使用维护说明书", "ACHIEVEMENT", true),
        new StageDoc("工艺设计工作总结", "MANUFACTURE", true),
        new StageDoc("工艺评审报告", "MANUFACTURE", true),
        new StageDoc("工艺总结", "MANUFACTURE", true),
        new StageDoc("工艺和生产条件考核报告", "MANUFACTURE", true),
        new StageDoc("标准化工作报告", "STANDARDIZE", true),
        new StageDoc("标准化审查报告", "STANDARDIZE", true),
        new StageDoc("工艺标准化工作报告", "STANDARDIZE", false),
        new StageDoc("工艺标准化审查报告", "STANDARDIZE", false),
        new StageDoc("质量保证大纲(质量计划)", "QUALITY", true),
        new StageDoc("风险管理计划", "RISK", true),
        new StageDoc("风险分析报告", "RISK", true),
        new StageDoc("可靠性分析评价", "RELIABILITY", true),
        new StageDoc("维修性评估报告", "MAINTAINABILITY", false),
        new StageDoc("测试性评估报告", "TESTABILITY", false),
        new StageDoc("保障性评估报告", "SUPPORTABILITY", false),
        new StageDoc("安全性评价报告", "SAFETY", false),
        new StageDoc("软件用户手册", "SOFTWARE", true),
        new StageDoc("软件版本说明", "SOFTWARE", true),
        new StageDoc("归档文件清单", "ACHIEVEMENT", true),
        new StageDoc("技术总结报告", "ACHIEVEMENT", true)
    );

    /** 退役阶段 (N) — 退役技术方案、处置文件 */
    private static final List<StageDoc> DOCS_N = List.of(
        new StageDoc("退役技术方案", "ACHIEVEMENT", true),
        new StageDoc("退役处置方案", "ACHIEVEMENT", true),
        new StageDoc("退役安全性分析报告", "SAFETY", true),
        new StageDoc("退役环境影响评估报告", "ENVIRONMENT", true),
        new StageDoc("技术总结报告", "ACHIEVEMENT", true),
        new StageDoc("归档文件清单", "ACHIEVEMENT", true)
    );

    private static final Map<String, List<StageDoc>> ALL_STAGE_DOCS = Map.of(
        "L", DOCS_L, "F", DOCS_F, "C", DOCS_C,
        "S", DOCS_S, "D", DOCS_D, "P", DOCS_P, "N", DOCS_N
    );

    /**
     * 根据研制阶段生成符合《军工产品研制技术文件编写指南》表1.1的文档目录。
     */
    public List<DocCatalog> generateByStage(Long projectId, Long stageId, String stageCode,
                                             Long userId, boolean overwrite) {
        List<StageDoc> docs = ALL_STAGE_DOCS.get(stageCode);
        if (docs == null) {
            log.warn("Unknown stage code: {}", stageCode);
            return List.of();
        }

        // Load dict items for type lookup
        List<SysDict> allTypes = dictMapper.selectList(
            new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, "DOC_TYPE")
                .eq(SysDict::getStatus, "ACTIVE")
                .orderByAsc(SysDict::getOrderNum));

        // Build category-to-types map
        Map<String, List<SysDict>> catTypes = new LinkedHashMap<>();
        for (SysDict type : allTypes) {
            String parent = type.getParentCode();
            if (parent != null && !parent.isEmpty()) {
                catTypes.computeIfAbsent(parent, k -> new ArrayList<>()).add(type);
            }
        }

        // Cache category→type assignment to reuse same type for same category
        Map<String, SysDict> assignedType = new HashMap<>();

        // Delete existing catalog if overwrite
        if (overwrite) {
            catalogMapper.delete(new LambdaQueryWrapper<DocCatalog>()
                .eq(DocCatalog::getProjectId, projectId)
                .eq(DocCatalog::getStageId, stageId));
            log.info("Cleared existing catalog for project={} stage={}", projectId, stageId);
        }

        List<DocCatalog> catalogs = new ArrayList<>();
        int seq = 1;
        Map<String, Integer> catSeq = new HashMap<>();

        for (StageDoc doc : docs) {
            String catCode = doc.category();
            int catOrder = catSeq.merge(catCode, 1, Integer::sum);

            // Assign a dict type for this document
            SysDict type = assignedType.computeIfAbsent(catCode, k -> {
                List<SysDict> types = catTypes.get(k);
                return (types != null && !types.isEmpty()) ? types.get(0) : null;
            });
            // If multiple docs share same category, cycle through available types
            if (type != null && catOrder > 1) {
                List<SysDict> types = catTypes.get(catCode);
                if (types != null && types.size() > 1) {
                    type = types.get((catOrder - 1) % types.size());
                }
            }
            String typeCode = type != null ? type.getDictCode() : catCode;

            String docCode = String.format("%s-%s-%03d", stageCode, catCode, catOrder);

            DocCatalog catalog = new DocCatalog();
            catalog.setProjectId(projectId);
            catalog.setStageId(stageId);
            catalog.setDocCode(docCode);
            catalog.setDocName(doc.name());
            catalog.setDocCategory(catCode);
            catalog.setDocType(typeCode);
            catalog.setStageCode(stageCode);
            catalog.setRequiredFlag(doc.required());
            catalog.setStatus("DRAFT");
            catalog.setCreatedBy(userId);
            catalog.setCreatedAt(LocalDateTime.now());

            catalogMapper.insert(catalog);
            catalogs.add(catalog);
            seq++;
        }

        log.info("Generated {} catalog entries for project={} stage={} (stageCode={}) per military doc guide",
            catalogs.size(), projectId, stageId, stageCode);
        return catalogs;
    }
}
