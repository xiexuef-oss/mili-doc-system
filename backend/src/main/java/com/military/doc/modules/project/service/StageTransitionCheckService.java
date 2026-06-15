package com.military.doc.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.project.entity.StageTransitionCheck;

import java.util.Map;

public interface StageTransitionCheckService extends IService<StageTransitionCheck> {

    /**
     * Run comprehensive stage transition check.
     *
     * @param projectId   project ID
     * @param fromStageId current stage ID
     * @param toStageId   target stage ID
     * @param operatorId  operator user ID
     * @return detailed check result with pass/blockers/warnings/metrics
     */
    Map<String, Object> runFullCheck(Long projectId, Long fromStageId, Long toStageId, Long operatorId);
}
