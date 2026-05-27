package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ProjectInputFile;
import com.military.doc.modules.project.mapper.ProjectInputFileMapper;
import com.military.doc.modules.project.service.ProjectInputFileService;
import org.springframework.stereotype.Service;

@Service
public class ProjectInputFileServiceImpl extends ServiceImpl<ProjectInputFileMapper, ProjectInputFile>
        implements ProjectInputFileService {
}
