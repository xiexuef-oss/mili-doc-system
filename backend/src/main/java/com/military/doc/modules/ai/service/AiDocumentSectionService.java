package com.military.doc.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.ai.entity.AiDocumentSection;
import com.military.doc.modules.ai.mapper.AiDocumentSectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j @Service
public class AiDocumentSectionService extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<AiDocumentSectionMapper, AiDocumentSection> {

    public AiDocumentSectionService(AiDocumentSectionMapper sectionMapper) {
        // MyBatis-Plus ServiceImpl uses @Autowired on baseMapper — no super(mapper) call needed
    }

    public List<AiDocumentSection> getByDocumentId(Long docId) {
        return this.list(new LambdaQueryWrapper<AiDocumentSection>()
            .eq(AiDocumentSection::getDocumentId, docId).orderByAsc(AiDocumentSection::getSortOrder));
    }
    @Transactional public void updateContent(Long id, String content, String contentJson) {
        AiDocumentSection s = this.getById(id);
        if (s != null) { s.setContent(content); s.setContentJson(contentJson); s.setStatus("ready"); this.updateById(s); }
    }
    @Transactional public void updateStatus(Long id, String status) {
        AiDocumentSection s = this.getById(id);
        if (s != null) { s.setStatus(status); this.updateById(s); }
    }
    @Transactional public void rename(Long id, String title) {
        AiDocumentSection s = this.getById(id);
        if (s != null) { s.setTitle(title); this.updateById(s); }
    }
    @Transactional public void delete(Long id) { this.removeById(id); }
    @Transactional
    public void move(Long id, int sortOrder, Long parentId) {
        AiDocumentSection s = this.getById(id);
        if (s != null) { s.setSortOrder(sortOrder); if (parentId != null) s.setParentId(parentId); this.updateById(s); }
    }
    @Transactional
    public void setOutline(Long docId, List<CanvasPatch.SectionDTO> dtos, Long userId) {
        this.getBaseMapper().delete(new LambdaQueryWrapper<AiDocumentSection>().eq(AiDocumentSection::getDocumentId, docId));
        for (CanvasPatch.SectionDTO dto : dtos) {
            AiDocumentSection s = new AiDocumentSection();
            s.setDocumentId(docId); s.setParentId(dto.getParentId()); s.setTitle(dto.getTitle());
            s.setLevel(dto.getLevel()); s.setSortOrder(dto.getSortOrder());
            s.setContent(dto.getContent() != null ? dto.getContent() : "");
            s.setStatus(dto.getStatus() != null ? dto.getStatus() : "empty");
            s.setCreatedBy(userId); s.setUpdatedBy(userId);
            this.save(s);
        }
    }
    @Transactional
    public AiDocumentSection addAfter(Long docId, String title, int level, Long afterId, Long parentId, Long userId) {
        List<AiDocumentSection> all = getByDocumentId(docId);
        int insertAt = 0;
        if (afterId != null) for (int i=0;i<all.size();i++) if (all.get(i).getId().equals(afterId)) { insertAt=i+1; break; }
        for (int i=insertAt;i<all.size();i++) { all.get(i).setSortOrder(i+2); this.updateById(all.get(i)); }
        AiDocumentSection s = new AiDocumentSection();
        s.setDocumentId(docId); s.setParentId(parentId); s.setTitle(title);
        s.setLevel(level); s.setSortOrder(insertAt+1); s.setStatus("empty");
        s.setCreatedBy(userId); s.setUpdatedBy(userId);
        this.save(s); return s;
    }
}
