package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ConfigurationBaselineItem;
import com.military.doc.modules.project.mapper.ConfigurationBaselineItemMapper;
import com.military.doc.modules.project.service.ConfigurationBaselineItemService;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationBaselineItemServiceImpl extends ServiceImpl<ConfigurationBaselineItemMapper, ConfigurationBaselineItem>
        implements ConfigurationBaselineItemService {
}
