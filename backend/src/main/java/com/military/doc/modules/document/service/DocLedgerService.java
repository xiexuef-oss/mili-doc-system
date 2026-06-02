package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.document.entity.DocLedger;

import java.util.List;

public interface DocLedgerService extends IService<DocLedger> {

    DocLedger createLedger(DocLedger ledger, Long operatorId);

    void transitionStatus(Long id, String targetStatus, Long operatorId, String remark);

    List<DocLedger> findUnreleasedByStage(Long projectId, Long stageId);

    List<DocLedger> listByProject(Long projectId, Long stageId, String lifecycleStatus);

    /**
     * 从文档目录同步创建台账条目。已存在的(同catalogId)跳过，返回新创建的条目数。
     */
    int syncFromCatalog(Long projectId, Long stageId, Long operatorId);

    /**
     * 从V2模板初始化文档章节结构
     */
    void initChaptersFromTemplate(Long docLedgerId, Long templateId, Long operatorId);

    /**
     * 从阶段文档清单同步创建台账条目。已存在的(同checklistItemId)跳过，返回新创建的条目数。
     */
    int syncFromChecklist(Long projectId, Long stageId, Long operatorId);

    /**
     * 级联删除文档台账及其关联的章节、日志、完整性检查、技术状态记录
     */
    void deleteLedger(Long id);

    /**
     * 根据checklistItemId级联删除对应的文档台账条目，返回删除数量
     */
    int deleteByChecklistItemId(Long checklistItemId);
}
