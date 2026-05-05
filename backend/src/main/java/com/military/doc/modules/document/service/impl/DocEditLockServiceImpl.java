package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocEditLock;
import com.military.doc.modules.document.mapper.DocEditLockMapper;
import com.military.doc.modules.document.service.DocEditLockService;
import org.springframework.stereotype.Service;

@Service
public class DocEditLockServiceImpl extends ServiceImpl<DocEditLockMapper, DocEditLock> implements DocEditLockService {
}
