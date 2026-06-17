package com.military.doc.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.project.entity.ProjectMasterData;

import java.util.Map;

public interface ProjectMasterDataService extends IService<ProjectMasterData> {

    ProjectMasterData getByProjectId(Long projectId);

    ProjectMasterData saveOrUpdateMasterData(Long projectId, Map<String, Object> data, Long operatorId);

    /** Get flattened view of master data for document filling */
    Map<String, Object> getFlattenedData(Long projectId);

    /** Get master data with JSONB fields parsed into objects/arrays (frontend-compatible) */
    Map<String, Object> getParsedData(Long projectId);
}
