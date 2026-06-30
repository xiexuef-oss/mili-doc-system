package com.military.doc.modules.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.template.entity.*;
import com.military.doc.modules.template.mapper.*;
import com.military.doc.modules.template.service.TemplateStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TemplateStructureServiceImpl implements TemplateStructureService {

    @Autowired private DocTemplateChapterMapper chapterMapper;
    @Autowired private DocTemplateElementMapper elementMapper;
    @Autowired private DocTemplateChapterElementMapper relMapper;

    @Override
    public List<DocTemplateChapter> getChapterTree(Long templateId) {
        List<DocTemplateChapter> all = listChapters(templateId);
        return buildTree(all, 0L);
    }

    @Override
    public Map<String, Object> getChapterTreeAsMap(Long templateId) {
        List<DocTemplateChapter> all = listChapters(templateId);
        return Map.of(
            "flat", all,
            "tree", buildTree(all, 0L)
        );
    }

    @Override
    public List<DocTemplateChapter> listChapters(Long templateId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .orderByAsc(DocTemplateChapter::getOrderNum));
    }

    @Override
    @Transactional
    public DocTemplateChapter createChapter(DocTemplateChapter chapter) {
        if (chapter.getOrderNum() == null) {
            Long count = chapterMapper.selectCount(new LambdaQueryWrapper<DocTemplateChapter>()
                    .eq(DocTemplateChapter::getTemplateId, chapter.getTemplateId())
                    .eq(DocTemplateChapter::getParentId, chapter.getParentId() != null ? chapter.getParentId() : 0L));
            chapter.setOrderNum(count.intValue() * 10);
        }
        if (chapter.getChapterLevel() == null) chapter.setChapterLevel(1);
        if (chapter.getIsRequired() == null) chapter.setIsRequired(true);
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public DocTemplateChapter updateChapter(DocTemplateChapter chapter) {
        DocTemplateChapter existing = chapterMapper.selectById(chapter.getId());
        if (existing == null) throw BusinessException.notFound("章节不存在: " + chapter.getId());
        chapterMapper.updateById(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        Set<Long> allIds = new HashSet<>();
        allIds.add(chapterId);
        collectDescendantIds(chapterId, allIds);
        chapterMapper.deleteBatchIds(allIds);
    }

    private void collectDescendantIds(Long parentId, Set<Long> ids) {
        List<DocTemplateChapter> children = chapterMapper.selectList(
                new LambdaQueryWrapper<DocTemplateChapter>().eq(DocTemplateChapter::getParentId, parentId));
        for (DocTemplateChapter child : children) {
            ids.add(child.getId());
            collectDescendantIds(child.getId(), ids);
        }
    }

    @Override
    @Transactional
    public void deleteChaptersByTemplateId(Long templateId) {
        List<DocTemplateChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<DocTemplateChapter>()
                    .eq(DocTemplateChapter::getTemplateId, templateId));
        for (DocTemplateChapter ch : chapters) {
            chapterMapper.deleteById(ch.getId());
        }
    }

    @Override
    public DocTemplateChapter getChapterById(Long chapterId) {
        return chapterMapper.selectById(chapterId);
    }

    @Override
    @Transactional
    public void reorderChapters(Long templateId, List<Long> chapterIdsInOrder) {
        List<DocTemplateChapter> chapters = chapterMapper.selectBatchIds(chapterIdsInOrder);
        java.util.Map<Long, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < chapterIdsInOrder.size(); i++) {
            orderMap.put(chapterIdsInOrder.get(i), i * 10);
        }
        for (DocTemplateChapter ch : chapters) {
            if (ch != null && ch.getTemplateId().equals(templateId)) {
                ch.setOrderNum(orderMap.getOrDefault(ch.getId(), ch.getOrderNum()));
                chapterMapper.updateById(ch);
            }
        }
    }

    @Override
    public List<DocTemplateChapter> getRequiredChapters(Long templateId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .eq(DocTemplateChapter::getIsRequired, true)
                .orderByAsc(DocTemplateChapter::getOrderNum));
    }

    @Override
    public List<DocTemplateElement> listElements(String elementCategory) {
        LambdaQueryWrapper<DocTemplateElement> wrapper = new LambdaQueryWrapper<>();
        if (elementCategory != null && !elementCategory.isBlank()) {
            wrapper.eq(DocTemplateElement::getElementCategory, elementCategory);
        }
        wrapper.eq(DocTemplateElement::getStatus, "ACTIVE").orderByAsc(DocTemplateElement::getElementCode);
        return elementMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public DocTemplateElement createElement(DocTemplateElement element) {
        elementMapper.insert(element);
        return element;
    }

    @Override
    @Transactional
    public void deleteElement(Long elementId) {
        elementMapper.deleteById(elementId);
    }

    @Override
    @Transactional
    public void attachElement(Long chapterId, Long elementId, boolean required, int orderNum) {
        DocTemplateChapterElement rel = new DocTemplateChapterElement();
        rel.setChapterId(chapterId);
        rel.setElementId(elementId);
        rel.setIsRequired(required);
        rel.setOrderNum(orderNum);
        relMapper.insert(rel);
    }

    @Override
    @Transactional
    public void detachElement(Long chapterId, Long elementId) {
        relMapper.delete(new LambdaQueryWrapper<DocTemplateChapterElement>()
                .eq(DocTemplateChapterElement::getChapterId, chapterId)
                .eq(DocTemplateChapterElement::getElementId, elementId));
    }

    @Override
    public List<DocTemplateElement> getChapterElements(Long chapterId) {
        List<DocTemplateChapterElement> rels = relMapper.selectList(
                new LambdaQueryWrapper<DocTemplateChapterElement>()
                        .eq(DocTemplateChapterElement::getChapterId, chapterId)
                        .orderByAsc(DocTemplateChapterElement::getOrderNum));
        if (rels.isEmpty()) return List.of();
        List<Long> elementIds = rels.stream().map(DocTemplateChapterElement::getElementId).toList();
        return elementMapper.selectBatchIds(elementIds);
    }

    private List<DocTemplateChapter> buildTree(List<DocTemplateChapter> all, Long parentId) {
        List<DocTemplateChapter> result = new ArrayList<>();
        for (DocTemplateChapter ch : all) {
            if ((parentId == 0L && (ch.getParentId() == null || ch.getParentId() == 0L))
                    || (parentId > 0L && parentId.equals(ch.getParentId()))) {
                result.add(ch);
            }
        }
        return result;
    }
}
