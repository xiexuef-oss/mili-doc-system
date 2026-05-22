package com.military.doc.modules.template.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.template.entity.DocTemplateCategory;

import java.util.List;

public interface DocTemplateCategoryService extends IService<DocTemplateCategory> {
    List<DocTemplateCategory> listActive();
    List<DocTemplateCategory> getTree();
}
