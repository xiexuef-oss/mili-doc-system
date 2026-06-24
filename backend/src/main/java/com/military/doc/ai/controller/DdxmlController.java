package com.military.doc.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.service.BlockCommandEngine;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.service.DocChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DDXML block-level document operations.
 * Generate structured content as blocks, execute precise block commands.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ddxml")
@Tag(name = "DDXML块级文档")
public class DdxmlController {

    private final BlockCommandEngine blockEngine;
    private final PromptTemplateService promptService;
    private final ContextAssemblyService contextAssembly;
    private final LlmClient llmClient;
    private final DocChapterService chapterService;

    public DdxmlController(BlockCommandEngine blockEngine, PromptTemplateService promptService,
                            ContextAssemblyService contextAssembly, LlmClient llmClient,
                            DocChapterService chapterService) {
        this.blockEngine = blockEngine;
        this.promptService = promptService;
        this.contextAssembly = contextAssembly;
        this.llmClient = llmClient;
        this.chapterService = chapterService;
    }

    @PostMapping("/chapters/{chapterId}/generate")
    @Operation(summary = "用DDXML格式生成章节内容（块级结构化）")
    public Result<Map<String, Object>> generateBlocks(@PathVariable Long chapterId,
                                                       @RequestParam Long projectId) {
        DocChapter ch = chapterService.getById(chapterId);
        if (ch == null) return Result.error("NOT_FOUND", "章节不存在");

        String context = contextAssembly.assembleContext(projectId);
        String systemPrompt = promptService.getTemplate("ddxml-generation");
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = "你是结构化文档生成引擎，严格遵循DDXML协议输出内容。";
        }
        String userPrompt = systemPrompt.replace("{{context}}", context)
            + "\n\n请生成章节「" + ch.getChapterNumber() + " " + ch.getChapterTitle() + "」的完整DDXML内容。只输出DDXML，不要任何说明。";

        log.info("DDXML generation: chapterId={}, projectId={}", chapterId, projectId);
        String response = llmClient.chat(systemPrompt, userPrompt);
        if (response != null && response.startsWith("null")) response = response.replaceFirst("^(null)+", "");

        BlockCommandEngine.BlockResult result = blockEngine.createDocument(chapterId, response);
        return Result.success(Map.of("blocks", result.blocks(), "plainText", result.plainText(), "blockCount", result.blockCount()));
    }

    @PostMapping("/chapters/{chapterId}/command")
    @Operation(summary = "块级编辑指令：replace_block, text_replace, delete_block, text_delete")
    public Result<?> blockCommand(@PathVariable Long chapterId,
                                   @RequestBody Map<String, Object> body) {
        String command = (String) body.get("command");
        Integer blockId = body.get("blockId") != null ? ((Number) body.get("blockId")).intValue() : null;
        String xml = (String) body.get("xml");
        String oldText = (String) body.get("oldText");
        String newText = (String) body.get("newText");

        if (command == null) return Result.error("PARAM_ERROR", "command is required");

        try {
            return switch (command) {
                case "replace_block" -> { var r = blockEngine.replaceBlock(chapterId, blockId, xml); yield Result.success(Map.of("block", r)); }
                case "text_replace" -> { var r = blockEngine.textReplace(chapterId, blockId, oldText, newText); yield Result.success(Map.of("block", r)); }
                case "delete_block" -> { boolean ok = blockEngine.deleteBlock(chapterId, blockId); yield Result.success(Map.of("deleted", ok)); }
                case "text_delete" -> { var r = blockEngine.textDelete(chapterId, blockId, oldText); yield Result.success(Map.of("block", r)); }
                default -> Result.error("PARAM_ERROR", "unknown command: " + command);
            };
        } catch (Exception e) {
            return Result.error("COMMAND_ERROR", e.getMessage());
        }
    }

    @GetMapping("/chapters/{chapterId}/blocks")
    @Operation(summary = "获取章节的DDXML块数组")
    public Result<List<BlockCommandEngine.ContentBlock>> getBlocks(@PathVariable Long chapterId) {
        return Result.success(blockEngine.loadBlocks(chapterId));
    }

    @PostMapping("/parse")
    @Operation(summary = "解析DDXML文本，返回块数组（预览，不存储）")
    public Result<BlockCommandEngine.BlockResult> parseDdxml(@RequestBody Map<String, String> body) {
        String xml = body.get("xml");
        if (xml == null || xml.isBlank()) return Result.error("PARAM_ERROR", "xml is required");
        return Result.success(blockEngine.parseContent(xml));
    }
}
