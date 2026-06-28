package com.military.doc.modules.document.controller;

import com.military.doc.common.exception.BusinessException;
import com.military.doc.common.result.Result;
import com.military.doc.common.security.ProjectAccessGuard;
import com.military.doc.modules.document.entity.DocApprovalRecord;
import com.military.doc.modules.document.mapper.DocApprovalRecordMapper;
import com.military.doc.modules.document.service.DocApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doc-approval")
@Tag(name = "文档签审")
public class DocApprovalController {

    private final DocApprovalService approvalService;
    private final DocApprovalRecordMapper recordMapper;
    private final ProjectAccessGuard accessGuard;

    public DocApprovalController(DocApprovalService approvalService, DocApprovalRecordMapper recordMapper,
                                  ProjectAccessGuard accessGuard) {
        this.approvalService = approvalService;
        this.recordMapper = recordMapper;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/init/{docLedgerId}")
    @Operation(summary = "为文档初始化签审链")
    public Result<List<DocApprovalRecord>> initApproval(
            @PathVariable Long docLedgerId,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {
        accessGuard.requireMemberForLedger(docLedgerId, authentication);
        String docType = body != null ? (String) body.getOrDefault("docType", "TECH_DOC") : "TECH_DOC";
        return Result.success(approvalService.initApprovalFlow(docLedgerId, docType));
    }

    @GetMapping("/records/{docLedgerId}")
    @Operation(summary = "查询文档签审记录列表")
    public Result<List<DocApprovalRecord>> getRecords(@PathVariable Long docLedgerId) {
        return Result.success(approvalService.getApprovalRecords(docLedgerId));
    }

    @GetMapping("/current/{docLedgerId}")
    @Operation(summary = "获取文档当前待审批步骤")
    public Result<DocApprovalRecord> getCurrentStep(@PathVariable Long docLedgerId) {
        return Result.success(approvalService.getCurrentStep(docLedgerId));
    }

    @PutMapping("/submit/{recordId}")
    @Operation(summary = "提交签审批复")
    public Result<DocApprovalRecord> submitApproval(@PathVariable Long recordId,
                                                      @RequestBody Map<String, Object> body,
                                                      Authentication authentication) {
        DocApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) throw BusinessException.notFound("签审记录不存在: id=" + recordId);
        accessGuard.requireMemberForLedger(record.getDocLedgerId(), authentication);
        Long approverId = (Long) authentication.getPrincipal();
        String result = (String) body.getOrDefault("result", "APPROVED");
        String opinion = (String) body.getOrDefault("opinion", "");
        return Result.success(approvalService.submitApproval(recordId, approverId, result, opinion));
    }
}
