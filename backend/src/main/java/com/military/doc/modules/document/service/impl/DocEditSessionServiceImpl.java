package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocEditSession;
import com.military.doc.modules.document.mapper.DocEditSessionMapper;
import com.military.doc.modules.document.service.DocEditSessionService;
import org.springframework.stereotype.Service;

@Service
public class DocEditSessionServiceImpl extends ServiceImpl<DocEditSessionMapper, DocEditSession> implements DocEditSessionService {
}
