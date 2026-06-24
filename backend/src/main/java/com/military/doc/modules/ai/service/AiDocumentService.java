package com.military.doc.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.ai.entity.*;
import com.military.doc.modules.ai.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j @Service @RequiredArgsConstructor
public class AiDocumentService {
    private final AiDocumentMapper docMapper;
    private final AiDocumentSectionMapper sectionMapper;
    private final AiDocumentVersionMapper versionMapper;
    private final ObjectMapper objectMapper;

    @Transactional public AiDocument create(Long userId, Long projectId, String title, String docType, String prompt) {
        AiDocument d = new AiDocument(); d.setUserId(userId); d.setProjectId(projectId);
        d.setTitle(title); d.setDocumentType(docType); d.setSourcePrompt(prompt); d.setStatus("draft");
        docMapper.insert(d); return d;
    }
    public AiDocument getById(Long id) { return docMapper.selectById(id); }
    @Transactional public void updateTitle(Long id, String title) { AiDocument d=docMapper.selectById(id); if(d!=null){d.setTitle(title);docMapper.updateById(d);} }
    @Transactional public void updateContent(Long id, String contentJson) {
        AiDocument d = docMapper.selectById(id);
        if (d != null) { d.setMetadata(contentJson); docMapper.updateById(d); }
    }

    @Transactional public void updateStatus(Long id, String s) { AiDocument d=docMapper.selectById(id); if(d!=null){d.setStatus(s);docMapper.updateById(d);} }
    @Transactional public void delete(Long id) { docMapper.deleteById(id); }
    public List<AiDocument> listByUser(Long uid) { return docMapper.selectList(new LambdaQueryWrapper<AiDocument>().eq(AiDocument::getUserId,uid).orderByDesc(AiDocument::getUpdatedAt)); }
    public List<AiDocument> listByProject(Long pid) { return docMapper.selectList(new LambdaQueryWrapper<AiDocument>().eq(AiDocument::getProjectId,pid).orderByDesc(AiDocument::getUpdatedAt)); }

    @Transactional public AiDocumentVersion createVersion(Long docId, Long userId, String reason) {
        AiDocument doc=docMapper.selectById(docId); if(doc==null)return null;
        List<AiDocumentSection> secs=sectionMapper.selectList(new LambdaQueryWrapper<AiDocumentSection>().eq(AiDocumentSection::getDocumentId,docId));
        try{ Map<String,Object> snap=Map.of("document",doc,"sections",secs); AiDocumentVersion v=new AiDocumentVersion();
            v.setDocumentId(docId);v.setUserId(userId);v.setTitle(doc.getTitle());v.setReason(reason);v.setSnapshot(objectMapper.writeValueAsString(snap));
            versionMapper.insert(v);return v; } catch(Exception e){log.error("version failed",e);return null;}
    }
    public List<AiDocumentVersion> listVersions(Long docId) { return versionMapper.selectList(new LambdaQueryWrapper<AiDocumentVersion>().eq(AiDocumentVersion::getDocumentId,docId).orderByDesc(AiDocumentVersion::getCreatedAt)); }

    @Transactional public AiDocument restoreVersion(Long docId, Long versionId) {
        AiDocumentVersion v=versionMapper.selectById(versionId);
        if(v==null||!v.getDocumentId().equals(docId))return null;
        try{Map<String,Object> snap=objectMapper.readValue(v.getSnapshot(),Map.class);
            sectionMapper.delete(new LambdaQueryWrapper<AiDocumentSection>().eq(AiDocumentSection::getDocumentId,docId));
            List<Map<String,Object>> sl=(List<Map<String,Object>>)snap.get("sections");
            if(sl!=null)for(Map<String,Object> s:sl){AiDocumentSection sec=objectMapper.convertValue(s,AiDocumentSection.class);sec.setId(null);sectionMapper.insert(sec);}
            return docMapper.selectById(docId);}catch(Exception e){log.error("restore failed",e);return null;}
    }
}
