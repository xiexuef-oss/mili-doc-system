package com.military.doc.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.project.entity.ProjectStage;

import java.util.List;
import java.util.Map;

public interface ProjectStageService extends IService<ProjectStage> {

    List<ProjectStage> initializeProjectStages(Long projectId, String initialStageCode, Long operatorId);

    Map<String, Object> requestTransition(Long projectId, Long currentStageId, Long operatorId);

    void startStage(Long projectId, Long stageId, Long operatorId);

    void completeStage(Long projectId, Long stageId, Long operatorId);

    void suspendStage(Long projectId, Long stageId, Long operatorId);

    void terminateStage(Long projectId, Long stageId, Long operatorId);

    Map<String, Object> getStageWorkbench(Long projectId, Long stageId);

    Map<String, Object> gateCheck(Long projectId, Long stageId, Long operatorId);
}
