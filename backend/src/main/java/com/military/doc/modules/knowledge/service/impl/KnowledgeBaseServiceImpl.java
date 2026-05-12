package com.military.doc.modules.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.ai.context.VectorIndexService;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.military.doc.modules.knowledge.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Autowired
    private VectorIndexService vectorIndexService;

    @Override
    public boolean save(KnowledgeBase entity) {
        boolean result = super.save(entity);
        if (result && entity.getId() != null) {
            reindexAsync(entity.getId());
        }
        return result;
    }

    @Override
    public boolean updateById(KnowledgeBase entity) {
        boolean result = super.updateById(entity);
        if (result && entity.getId() != null) {
            reindexAsync(entity.getId());
        }
        return result;
    }

    @Async
    void reindexAsync(Long kbId) {
        try {
            vectorIndexService.reindexKnowledge(kbId);
        } catch (Exception e) {
            log.warn("Async reindex knowledge {} failed: {}", kbId, e.getMessage());
        }
    }
}
