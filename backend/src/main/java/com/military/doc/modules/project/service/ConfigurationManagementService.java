package com.military.doc.modules.project.service;

import com.military.doc.modules.project.entity.*;

public interface ConfigurationManagementService {

    ConfigurationBaseline createBaseline(Long projectId, Long stageId, String baselineType, Long operatorId);

    void approveBaseline(Long baselineId, Long operatorId);

    void setBaselineEffective(Long baselineId, Long operatorId);

    ConfigurationChangeRequest createChangeRequest(ConfigurationChangeRequest request, Long operatorId);

    void processChangeRequest(Long id, String action, Long operatorId);

    ConfigurationAudit conductAudit(Long projectId, Long stageId, String auditType, Long operatorId);

    void completeAudit(Long auditId, String auditResult, String auditOpinion, Long operatorId);
}
