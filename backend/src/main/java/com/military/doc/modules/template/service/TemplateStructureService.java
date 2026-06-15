package com.military.doc.modules.template.service;

import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateElement;

import java.util.List;
import java.util.Map;

public interface TemplateStructureService {

    /** Get full chapter tree for a template */
    List<DocTemplateChapter> getChapterTree(Long templateId);

    /** Get chapter with children recursively */
    Map<String, Object> getChapterTreeAsMap(Long templateId);

    /** Get all chapters for a template (flat list) */
    List<DocTemplateChapter> listChapters(Long templateId);

    /** Create a chapter under parent */
    DocTemplateChapter createChapter(DocTemplateChapter chapter);

    /** Update a chapter */
    DocTemplateChapter updateChapter(DocTemplateChapter chapter);

    /** Delete a chapter and its children */
    void deleteChapter(Long chapterId);

    /** Delete all chapters for a template */
    void deleteChaptersByTemplateId(Long templateId);

    /** Get a single chapter by ID */
    DocTemplateChapter getChapterById(Long chapterId);

    /** Reorder chapters */
    void reorderChapters(Long templateId, List<Long> chapterIdsInOrder);

    /** Get required chapters only */
    List<DocTemplateChapter> getRequiredChapters(Long templateId);

    /** --- Elements --- */
    List<DocTemplateElement> listElements(String elementCategory);

    DocTemplateElement createElement(DocTemplateElement element);

    void deleteElement(Long elementId);

    /** Attach element to chapter */
    void attachElement(Long chapterId, Long elementId, boolean required, int orderNum);

    /** Detach element from chapter */
    void detachElement(Long chapterId, Long elementId);

    /** Get elements attached to a chapter */
    List<DocTemplateElement> getChapterElements(Long chapterId);
}
