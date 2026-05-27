package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ConfigurationChangeRequest;
import com.military.doc.modules.project.mapper.ConfigurationChangeRequestMapper;
import com.military.doc.modules.project.service.ConfigurationChangeRequestService;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationChangeRequestServiceImpl extends ServiceImpl<ConfigurationChangeRequestMapper, ConfigurationChangeRequest>
        implements ConfigurationChangeRequestService {
}
