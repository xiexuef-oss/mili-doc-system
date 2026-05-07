package com.military.doc.modules.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.military.doc.modules.knowledge.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {
}
