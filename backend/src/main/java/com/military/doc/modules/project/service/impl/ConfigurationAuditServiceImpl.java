package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ConfigurationAudit;
import com.military.doc.modules.project.mapper.ConfigurationAuditMapper;
import com.military.doc.modules.project.service.ConfigurationAuditService;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationAuditServiceImpl extends ServiceImpl<ConfigurationAuditMapper, ConfigurationAudit>
        implements ConfigurationAuditService {
}
