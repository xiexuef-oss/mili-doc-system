package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import com.military.doc.modules.project.service.ProjectStageService;
import org.springframework.stereotype.Service;

@Service
public class ProjectStageServiceImpl extends ServiceImpl<ProjectStageMapper, ProjectStage> implements ProjectStageService {
}