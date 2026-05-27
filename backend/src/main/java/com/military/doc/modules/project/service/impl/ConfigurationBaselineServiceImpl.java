package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ConfigurationBaseline;
import com.military.doc.modules.project.mapper.ConfigurationBaselineMapper;
import com.military.doc.modules.project.service.ConfigurationBaselineService;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationBaselineServiceImpl extends ServiceImpl<ConfigurationBaselineMapper, ConfigurationBaseline>
        implements ConfigurationBaselineService {
}
