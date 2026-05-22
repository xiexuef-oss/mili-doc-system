package com.military.doc.modules.knowledge.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.knowledge.entity.KnowledgeCard;
import com.military.doc.modules.knowledge.service.KnowledgeCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-cards")
public class KnowledgeCardController {

    @Autowired private KnowledgeCardService knowledgeCardService;

    @GetMapping
    public Result<List<KnowledgeCard>> list(@RequestParam(required = false) String cardType,
                                             @RequestParam(required = false) String targetTable,
                                             @RequestParam(required = false) Long targetId) {
        if (targetTable != null) {
            return Result.success(knowledgeCardService.listByTarget(targetTable, targetId));
        }
        if (cardType != null) {
            return Result.success(knowledgeCardService.listByType(cardType));
        }
        return Result.success(knowledgeCardService.list());
    }

    @GetMapping("/search")
    public Result<List<KnowledgeCard>> search(@RequestParam String keyword) {
        return Result.success(knowledgeCardService.search(keyword));
    }

    @GetMapping("/tag/{tag}")
    public Result<List<KnowledgeCard>> byTag(@PathVariable String tag) {
        return Result.success(knowledgeCardService.listByTags(tag));
    }

    @PostMapping
    public Result<KnowledgeCard> create(@RequestBody KnowledgeCard card) {
        knowledgeCardService.save(card);
        return Result.success(card);
    }

    @PutMapping("/{id}")
    public Result<KnowledgeCard> update(@PathVariable Long id, @RequestBody KnowledgeCard card) {
        card.setId(id);
        knowledgeCardService.updateById(card);
        return Result.success(card);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeCardService.removeById(id);
        return Result.success();
    }
}
