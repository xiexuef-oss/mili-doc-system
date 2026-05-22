package com.military.doc.modules.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.template.entity.DocTemplateCategory;
import com.military.doc.modules.template.mapper.DocTemplateCategoryMapper;
import com.military.doc.modules.template.service.DocTemplateCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocTemplateCategoryServiceImpl
        extends ServiceImpl<DocTemplateCategoryMapper, DocTemplateCategory>
        implements DocTemplateCategoryService {

    @Override
    public List<DocTemplateCategory> listActive() {
        return list(new LambdaQueryWrapper<DocTemplateCategory>()
                .eq(DocTemplateCategory::getStatus, "ACTIVE")
                .orderByAsc(DocTemplateCategory::getOrderNum));
    }

    @Override
    public List<DocTemplateCategory> getTree() {
        List<DocTemplateCategory> all = listActive();
        Map<Long, List<DocTemplateCategory>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(DocTemplateCategory::getParentId));

        List<DocTemplateCategory> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        return roots;
    }
}
