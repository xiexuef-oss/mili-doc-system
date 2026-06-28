package com.military.doc.modules.document.controller;

import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.CompletenessCheckResult;
import com.military.doc.modules.document.service.CompletenessCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/completeness")
public class CompletenessCheckController {

    @Autowired private CompletenessCheckService checkService;

    @PostMapping("/check")
    public Result<CompletenessCheckResult> check(@RequestParam Long projectId,
                                                  @RequestParam Long docLedgerId,
                                                  Authentication authentication) {
        Long operatorId = (Long) authentication.getPrincipal();
        return Result.success(checkService.checkDocument(projectId, docLedgerId, operatorId));
    }

    @GetMapping("/history/{docLedgerId}")
    public Result<List<CompletenessCheckResult>> history(@PathVariable Long docLedgerId) {
        return Result.success(checkService.getHistory(docLedgerId));
    }

    @GetMapping("/project/{projectId}/summary")
    public Result<Map<String, Object>> projectSummary(@PathVariable Long projectId) {
        return Result.success(checkService.getProjectSummary(projectId));
    }
}
