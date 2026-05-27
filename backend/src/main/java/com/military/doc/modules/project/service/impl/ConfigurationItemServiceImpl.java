package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.project.entity.ConfigurationItem;
import com.military.doc.modules.project.mapper.ConfigurationItemMapper;
import com.military.doc.modules.project.service.ConfigurationItemService;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationItemServiceImpl extends ServiceImpl<ConfigurationItemMapper, ConfigurationItem>
        implements ConfigurationItemService {
}
