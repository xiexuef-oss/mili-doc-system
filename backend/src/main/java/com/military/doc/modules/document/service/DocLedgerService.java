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
}
