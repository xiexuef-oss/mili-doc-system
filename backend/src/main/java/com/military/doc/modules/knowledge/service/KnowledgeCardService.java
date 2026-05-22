package com.military.doc.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.knowledge.entity.KnowledgeCard;
import com.military.doc.modules.knowledge.mapper.KnowledgeCardMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeCardService extends ServiceImpl<KnowledgeCardMapper, KnowledgeCard> {

    public List<KnowledgeCard> listByTarget(String targetTable, Long targetId) {
        return list(new LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getTargetTable, targetTable)
                .eq(targetId != null, KnowledgeCard::getTargetId, targetId)
                .eq(KnowledgeCard::getStatus, "ACTIVE")
                .orderByAsc(KnowledgeCard::getCardType));
    }

    public List<KnowledgeCard> listByType(String cardType) {
        return list(new LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getCardType, cardType)
                .eq(KnowledgeCard::getStatus, "ACTIVE")
                .orderByAsc(KnowledgeCard::getId));
    }

    public List<KnowledgeCard> search(String keyword) {
        return list(new LambdaQueryWrapper<KnowledgeCard>()
                .and(w -> w.like(KnowledgeCard::getTitle, keyword)
                          .or().like(KnowledgeCard::getPlainLanguage, keyword)
                          .or().like(KnowledgeCard::getTags, keyword))
                .eq(KnowledgeCard::getStatus, "ACTIVE")
                .orderByAsc(KnowledgeCard::getId));
    }

    public List<KnowledgeCard> listByTags(String tag) {
        return list(new LambdaQueryWrapper<KnowledgeCard>()
                .like(KnowledgeCard::getTags, tag)
                .eq(KnowledgeCard::getStatus, "ACTIVE")
                .orderByAsc(KnowledgeCard::getId));
    }
}
