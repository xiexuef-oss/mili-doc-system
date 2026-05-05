package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.mapper.DocVersionMapper;
import com.military.doc.modules.document.service.DocVersionService;
import org.springframework.stereotype.Service;

@Service
public class DocVersionServiceImpl extends ServiceImpl<DocVersionMapper, DocVersion> implements DocVersionService {
}
