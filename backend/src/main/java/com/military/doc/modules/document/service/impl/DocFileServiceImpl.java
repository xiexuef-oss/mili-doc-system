package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.mapper.DocFileMapper;
import com.military.doc.modules.document.service.DocFileService;
import org.springframework.stereotype.Service;

@Service
public class DocFileServiceImpl extends ServiceImpl<DocFileMapper, DocFile> implements DocFileService {
}
