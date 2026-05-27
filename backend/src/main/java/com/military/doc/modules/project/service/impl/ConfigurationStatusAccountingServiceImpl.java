package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ConfigurationStatusAccounting;
import com.military.doc.modules.project.mapper.ConfigurationStatusAccountingMapper;
import com.military.doc.modules.project.service.ConfigurationStatusAccountingService;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationStatusAccountingServiceImpl extends ServiceImpl<ConfigurationStatusAccountingMapper, ConfigurationStatusAccounting>
        implements ConfigurationStatusAccountingService {
}
