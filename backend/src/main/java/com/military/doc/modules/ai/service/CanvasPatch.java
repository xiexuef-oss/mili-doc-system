package com.military.doc.modules.ai.service;

import lombok.Data;
import java.util.List;
import java.util.Map;

public class CanvasPatch {
    @Data
    public static class Patch { private String type; private Object payload; }
    @Data public static class CreateDocumentPayload { public Long documentId; public String title,description,documentType,sourcePrompt,status; }
    @Data public static class SetOutlinePayload { public Long documentId; public List<SectionDTO> sections; }
    @Data public static class SectionDTO { public Long id,parentId; public String title; public int level,sortOrder; public String content,status; public Map<String,Object> contentJson; }
    @Data public static class AddSectionPayload { public Long documentId,afterSectionId,parentId; public SectionDTO section; }
    @Data public static class DeleteSectionPayload { public Long documentId,sectionId; }
    @Data public static class RenameSectionPayload { public Long documentId,sectionId; public String title; }
    @Data public static class UpdateContentPayload { public Long documentId,sectionId; public String content; public Map<String,Object> contentJson; }
    @Data public static class AppendContentPayload { public Long documentId,sectionId; public String delta; }
    @Data public static class UpdateStatusPayload { public Long documentId,sectionId; public String status; }
    @Data public static class MoveSectionPayload { public Long documentId,sectionId,parentId; public int sortOrder; }
    @Data public static class SetTitlePayload { public Long documentId; public String title; }
    @Data public static class SetStatusPayload { public Long documentId; public String status; }
}
