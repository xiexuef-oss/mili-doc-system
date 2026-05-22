package com.military.doc.modules.template.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.template.entity.DocTemplateV2;

import java.util.List;

public interface DocTemplateV2Service extends IService<DocTemplateV2> {
    List<DocTemplateV2> listByCategory(Long categoryId);
}
