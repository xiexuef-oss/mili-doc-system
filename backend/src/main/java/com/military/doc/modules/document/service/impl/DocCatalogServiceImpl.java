package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.service.DocCatalogService;
import org.springframework.stereotype.Service;

@Service
public class DocCatalogServiceImpl extends ServiceImpl<DocCatalogMapper, DocCatalog> implements DocCatalogService {
}
