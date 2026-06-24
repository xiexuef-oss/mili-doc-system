package com.military.doc.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.ai.entity.AiDocumentSection;
import com.military.doc.modules.ai.mapper.AiDocumentSectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j @Service @RequiredArgsConstructor
public class AiDocumentSectionService {
    private final AiDocumentSectionMapper sectionMapper;

    public List<AiDocumentSection> getByDocumentId(Long docId) {
        return sectionMapper.selectList(new LambdaQueryWrapper<AiDocumentSection>()
            .eq(AiDocumentSection::getDocumentId, docId).orderByAsc(AiDocumentSection::getSortOrder));
    }
    @Transactional public void updateContent(Long id, String content, String contentJson) {
        AiDocumentSection s = sectionMapper.selectById(id);
        if (s != null) { s.setContent(content); s.setContentJson(contentJson); s.setStatus("ready"); sectionMapper.updateById(s); }
    }
    @Transactional public void updateStatus(Long id, String status) {
        AiDocumentSection s = sectionMapper.selectById(id);
        if (s != null) { s.setStatus(status); sectionMapper.updateById(s); }
    }
    @Transactional public void rename(Long id, String title) {
        AiDocumentSection s = sectionMapper.selectById(id);
        if (s != null) { s.setTitle(title); sectionMapper.updateById(s); }
    }
    @Transactional public void delete(Long id) { sectionMapper.deleteById(id); }
    @Transactional
    public void move(Long id, int sortOrder, Long parentId) {
        AiDocumentSection s = sectionMapper.selectById(id);
        if (s != null) { s.setSortOrder(sortOrder); if (parentId != null) s.setParentId(parentId); sectionMapper.updateById(s); }
    }
    @Transactional
    public void setOutline(Long docId, List<CanvasPatch.SectionDTO> dtos, Long userId) {
        sectionMapper.delete(new LambdaQueryWrapper<AiDocumentSection>().eq(AiDocumentSection::getDocumentId, docId));
        for (CanvasPatch.SectionDTO dto : dtos) {
            AiDocumentSection s = new AiDocumentSection();
            s.setDocumentId(docId); s.setParentId(dto.getParentId()); s.setTitle(dto.getTitle());
            s.setLevel(dto.getLevel()); s.setSortOrder(dto.getSortOrder());
            s.setContent(dto.getContent() != null ? dto.getContent() : "");
            s.setStatus(dto.getStatus() != null ? dto.getStatus() : "empty");
            s.setCreatedBy(userId); s.setUpdatedBy(userId);
            sectionMapper.insert(s);
        }
    }
    @Transactional
    public AiDocumentSection addAfter(Long docId, String title, int level, Long afterId, Long parentId, Long userId) {
        List<AiDocumentSection> all = getByDocumentId(docId);
        int insertAt = 0;
        if (afterId != null) for (int i=0;i<all.size();i++) if (all.get(i).getId().equals(afterId)) { insertAt=i+1; break; }
        for (int i=insertAt;i<all.size();i++) { all.get(i).setSortOrder(i+2); sectionMapper.updateById(all.get(i)); }
        AiDocumentSection s = new AiDocumentSection();
        s.setDocumentId(docId); s.setParentId(parentId); s.setTitle(title);
        s.setLevel(level); s.setSortOrder(insertAt+1); s.setStatus("empty");
        s.setCreatedBy(userId); s.setUpdatedBy(userId);
        sectionMapper.insert(s); return s;
    }
}
