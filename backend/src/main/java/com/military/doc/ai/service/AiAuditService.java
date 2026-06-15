package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.ai.entity.AiAuditLog;
import com.military.doc.ai.mapper.AiAuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI API 调用审计服务 — 记录、查询、统计。
 */
@Slf4j
@Service
public class AiAuditService {

    private final AiAuditLogMapper mapper;

    public AiAuditService(AiAuditLogMapper mapper) {
        this.mapper = mapper;
    }

    public void logCall(AiAuditLog entry) {
        try {
            mapper.insert(entry);
            log.debug("Audit log saved: id={}, taskType={}, provider={}, success={}",
                entry.getId(), entry.getTaskType(), entry.getProvider(), entry.getSuccess());
        } catch (Exception e) {
            log.warn("Failed to save AI audit log (non-fatal): {}", e.getMessage());
        }
    }

    public Map<String, Object> queryLogs(Long projectId, String taskType,
                                          LocalDateTime from, LocalDateTime to,
                                          int page, int size) {
        var qw = new LambdaQueryWrapper<AiAuditLog>();
        if (projectId != null) qw.eq(AiAuditLog::getProjectId, projectId);
        if (taskType != null && !taskType.isBlank()) qw.eq(AiAuditLog::getTaskType, taskType);
        if (from != null) qw.ge(AiAuditLog::getCreatedAt, from);
        if (to != null) qw.le(AiAuditLog::getCreatedAt, to);
        qw.orderByDesc(AiAuditLog::getCreatedAt);

        Page<AiAuditLog> result = mapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    public Map<String, Object> getStats(Long projectId) {
        var qw = new LambdaQueryWrapper<AiAuditLog>();
        if (projectId != null) qw.eq(AiAuditLog::getProjectId, projectId);

        var all = mapper.selectList(qw);
        long total = all.size();
        long success = all.stream().filter(a -> Boolean.TRUE.equals(a.getSuccess())).count();
        long failed = total - success;

        double avgLatency = all.stream()
            .filter(a -> a.getLatencyMs() != null)
            .mapToInt(AiAuditLog::getLatencyMs)
            .average().orElse(0);

        // 按提供商统计
        Map<String, Long> byProvider = new LinkedHashMap<>();
        for (var l : all) {
            byProvider.merge(l.getProvider(), 1L, Long::sum);
        }

        // 按任务类型统计
        Map<String, Long> byTask = new LinkedHashMap<>();
        for (var l : all) {
            byTask.merge(l.getTaskType(), 1L, Long::sum);
        }

        // 按位置统计
        Map<String, Long> byLocality = new LinkedHashMap<>();
        for (var l : all) {
            String loc = l.getLocality() != null ? l.getLocality() : "UNKNOWN";
            byLocality.merge(loc, 1L, Long::sum);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCalls", total);
        stats.put("successCount", success);
        stats.put("failedCount", failed);
        stats.put("successRate", total > 0 ? (double) success / total : 0);
        stats.put("avgLatencyMs", Math.round(avgLatency));
        stats.put("byProvider", byProvider);
        stats.put("byTaskType", byTask);
        stats.put("byLocality", byLocality);
        return stats;
    }
}
