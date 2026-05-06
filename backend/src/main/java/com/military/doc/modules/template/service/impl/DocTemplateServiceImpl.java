package com.military.doc.modules.template.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.template.entity.DocTemplate;
import com.military.doc.modules.template.mapper.DocTemplateMapper;
import com.military.doc.modules.template.service.DocTemplateService;
import org.springframework.stereotype.Service;

@Service
public class DocTemplateServiceImpl extends ServiceImpl<DocTemplateMapper, DocTemplate> implements DocTemplateService {
}
