package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.project.service.ProjectService;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
}