package com.military.doc.modules.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import com.military.doc.modules.template.service.DocTemplateV2Service;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocTemplateV2ServiceImpl
        extends ServiceImpl<DocTemplateV2Mapper, DocTemplateV2>
        implements DocTemplateV2Service {

    @Override
    public List<DocTemplateV2> listByCategory(Long categoryId) {
        return list(new LambdaQueryWrapper<DocTemplateV2>()
                .eq(DocTemplateV2::getCategoryId, categoryId)
                .eq(DocTemplateV2::getStatus, "ACTIVE")
                .orderByAsc(DocTemplateV2::getTemplateCode));
    }
}
