package com.military.doc.ai.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.knowledge.entity.ChapterKnowledgeCardLink;
import com.military.doc.modules.knowledge.entity.KnowledgeCard;
import com.military.doc.modules.knowledge.mapper.ChapterKnowledgeCardLinkMapper;
import com.military.doc.modules.knowledge.mapper.KnowledgeCardMapper;
import com.military.doc.modules.project.service.ProjectMasterDataService;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.mapper.StandardMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.TemplateChapterClauseLink;
import com.military.doc.modules.template.entity.TemplateChapterFieldMapping;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.TemplateChapterClauseLinkMapper;
import com.military.doc.modules.template.mapper.TemplateChapterFieldMappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChapterWritingContextService {

    private final DocChapterMapper docChapterMapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final TemplateChapterClauseLinkMapper clauseLinkMapper;
    private final TemplateChapterFieldMappingMapper fieldMappingMapper;
    private final ChapterKnowledgeCardLinkMapper cardLinkMapper;
    private final StandardClauseMapper standardClauseMapper;
    private final StandardMapper standardMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;
    private final ProjectMasterDataService masterDataService;

    public ChapterWritingContextService(DocChapterMapper docChapterMapper,
                                        DocTemplateChapterMapper templateChapterMapper,
                                        TemplateChapterClauseLinkMapper clauseLinkMapper,
                                        TemplateChapterFieldMappingMapper fieldMappingMapper,
                                        ChapterKnowledgeCardLinkMapper cardLinkMapper,
                                        StandardClauseMapper standardClauseMapper,
                                        StandardMapper standardMapper,
                                        KnowledgeCardMapper knowledgeCardMapper,
                                        ProjectMasterDataService masterDataService) {
        this.docChapterMapper = docChapterMapper;
        this.templateChapterMapper = templateChapterMapper;
        this.clauseLinkMapper = clauseLinkMapper;
        this.fieldMappingMapper = fieldMappingMapper;
        this.cardLinkMapper = cardLinkMapper;
        this.standardClauseMapper = standardClauseMapper;
        this.standardMapper = standardMapper;
        this.knowledgeCardMapper = knowledgeCardMapper;
        this.masterDataService = masterDataService;
    }

    public ChapterWritingContext assembleForChapter(Long docChapterId, Long projectId) {
        DocChapter docChapter = docChapterMapper.selectById(docChapterId);
        if (docChapter == null) {
            log.warn("DocChapter not found: {}", docChapterId);
            return null;
        }

        ChapterWritingContext ctx = new ChapterWritingContext();
        ctx.setDocChapterId(docChapterId);
        ctx.setTemplateChapterId(docChapter.getTemplateChapterId());
        ctx.setChapterNumber(docChapter.getChapterNumber());
        ctx.setChapterTitle(docChapter.getChapterTitle());
        ctx.setChapterLevel(docChapter.getChapterLevel());

        Long templateChapterId = docChapter.getTemplateChapterId();

        // 1. Template guidance
        if (templateChapterId != null) {
            DocTemplateChapter tplChapter = templateChapterMapper.selectById(templateChapterId);
            if (tplChapter != null) {
                ctx.setTemplateDescription(tplChapter.getDescription());
                ctx.setWritingTips(tplChapter.getWritingTips());
                ctx.setSampleContent(tplChapter.getSampleContent());
            }
        }

        // 2. Applicable standard clauses
        ctx.setApplicableClauses(loadClauses(templateChapterId));

        // 3. Relevant knowledge cards
        ctx.setRelevantCards(loadCards(templateChapterId));

        // 4. Master data fields
        ctx.setRelevantFields(loadFields(projectId, templateChapterId));

        return ctx;
    }

    private List<ChapterWritingContext.StandardClauseRef> loadClauses(Long templateChapterId) {
        if (templateChapterId == null) return Collections.emptyList();

        List<TemplateChapterClauseLink> links = clauseLinkMapper.selectList(
            new LambdaQueryWrapper<TemplateChapterClauseLink>()
                .eq(TemplateChapterClauseLink::getTemplateChapterId, templateChapterId)
        );
        if (links.isEmpty()) return Collections.emptyList();

        // Batch load clauses
        Set<Long> clauseIds = links.stream().map(TemplateChapterClauseLink::getStandardClauseId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, StandardClause> clauseMap = clauseIds.isEmpty() ? Map.of() :
            standardClauseMapper.selectBatchIds(clauseIds).stream()
                .collect(Collectors.toMap(StandardClause::getId, c -> c));

        // Batch load standards
        Set<Long> standardIds = clauseMap.values().stream().map(StandardClause::getStandardId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Standard> standardMap = standardIds.isEmpty() ? Map.of() :
            standardMapper.selectBatchIds(standardIds).stream()
                .collect(Collectors.toMap(Standard::getId, s -> s));

        List<ChapterWritingContext.StandardClauseRef> refs = new ArrayList<>();
        for (TemplateChapterClauseLink link : links) {
            StandardClause clause = clauseMap.get(link.getStandardClauseId());
            if (clause == null) continue;

            ChapterWritingContext.StandardClauseRef ref = new ChapterWritingContext.StandardClauseRef();
            ref.setClauseId(clause.getId());
            ref.setClauseNumber(clause.getClauseNumber());
            ref.setClauseTitle(clause.getClauseTitle());
            ref.setClauseContent(clause.getClauseContent());
            ref.setLinkType(link.getLinkType());

            Standard standard = standardMap.get(clause.getStandardId());
            if (standard != null) {
                ref.setStandardCode(standard.getStandardCode());
                ref.setStandardName(standard.getStandardName());
            }

            refs.add(ref);
        }
        return refs;
    }

    private List<ChapterWritingContext.KnowledgeCardRef> loadCards(Long templateChapterId) {
        if (templateChapterId == null) return Collections.emptyList();

        List<ChapterKnowledgeCardLink> links = cardLinkMapper.selectList(
            new LambdaQueryWrapper<ChapterKnowledgeCardLink>()
                .eq(ChapterKnowledgeCardLink::getTemplateChapterId, templateChapterId)
        );
        if (links.isEmpty()) return Collections.emptyList();

        // Batch load cards
        Set<Long> cardIds = links.stream().map(ChapterKnowledgeCardLink::getKnowledgeCardId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, KnowledgeCard> cardMap = cardIds.isEmpty() ? Map.of() :
            knowledgeCardMapper.selectBatchIds(cardIds).stream()
                .collect(Collectors.toMap(KnowledgeCard::getId, c -> c));

        List<ChapterWritingContext.KnowledgeCardRef> refs = new ArrayList<>();
        for (ChapterKnowledgeCardLink link : links) {
            KnowledgeCard card = cardMap.get(link.getKnowledgeCardId());
            if (card == null) continue;

            ChapterWritingContext.KnowledgeCardRef ref = new ChapterWritingContext.KnowledgeCardRef();
            ref.setCardId(card.getId());
            ref.setTitle(card.getTitle());
            ref.setPlainLanguage(card.getPlainLanguage());
            ref.setGjbReference(card.getGjbReference());
            ref.setTags(card.getTags());
            refs.add(ref);
        }
        return refs;
    }

    private List<ChapterWritingContext.MasterDataFieldRef> loadFields(Long projectId, Long templateChapterId) {
        if (templateChapterId == null) return Collections.emptyList();

        List<TemplateChapterFieldMapping> mappings = fieldMappingMapper.selectList(
            new LambdaQueryWrapper<TemplateChapterFieldMapping>()
                .eq(TemplateChapterFieldMapping::getTemplateChapterId, templateChapterId)
                .orderByAsc(TemplateChapterFieldMapping::getOrderNum)
        );
        if (mappings.isEmpty()) return Collections.emptyList();

        Map<String, Object> flattened = masterDataService.getFlattenedData(projectId);

        List<ChapterWritingContext.MasterDataFieldRef> refs = new ArrayList<>();
        for (TemplateChapterFieldMapping mapping : mappings) {
            ChapterWritingContext.MasterDataFieldRef ref = new ChapterWritingContext.MasterDataFieldRef();
            ref.setMasterDataPath(mapping.getMasterDataPath());
            ref.setFieldLabel(mapping.getFieldLabel());
            ref.setRequired(Boolean.TRUE.equals(mapping.getIsRequired()));

            Object value = flattened.get(rewritePath(mapping.getMasterDataPath()));
            ref.setCurrentValue(value);
            ref.setValueStatus(value != null && !value.toString().isBlank() ? "FILLED" : "EMPTY");

            refs.add(ref);
        }
        return refs;
    }

    /**
     * Rewrite path prefixes to match flattened data keys.
     * "equipmentInfo.equipmentName" → "equipment.equipmentName"
     */
    private String rewritePath(String path) {
        if (path == null) return "";
        return path.replace("equipmentInfo.", "equipment.")
                  .replace("extendedFields.", "extended.");
    }
}
