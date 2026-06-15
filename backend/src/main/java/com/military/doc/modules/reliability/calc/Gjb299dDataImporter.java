package com.military.doc.modules.reliability.calc;

import com.military.doc.modules.reliability.entity.RelGjb299dCache;
import com.military.doc.modules.reliability.mapper.RelGjb299dCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GJB/Z 299D-2024 数据导入器。
 * 
 * 从已修正的 DOCX 中提取 834 个表格的数值数据，
 * 结构化写入 rel_gjb299d_cache 表，供 ReliabilityPredictor 快速查表。
 *
 * 由于 DOCX 表格解析复杂度高（834张表，格式多样），
 * 本导入器提供两种模式：
 * 1. 批量模式：从预处理的 JSON/CSV 文件批量导入
 * 2. 单条模式：逐条插入缓存记录，供手工录入或脚本调用
 *
 * 批量导入数据应由预处理脚本从 DOCX 提取后生成。
 * 典型命令: python extract_299d_tables.py → import_299d.json → 本导入器
 */
@Slf4j
@Component
public class Gjb299dDataImporter {

    private final RelGjb299dCacheMapper cacheMapper;

    public Gjb299dDataImporter(RelGjb299dCacheMapper cacheMapper) {
        this.cacheMapper = cacheMapper;
    }

    /**
     * 批量导入缓存记录。
     * @param entries 预处理的缓存条目列表
     * @return 成功导入数量
     */
    public int batchImport(List<CacheEntry> entries) {
        int count = 0;
        for (CacheEntry entry : entries) {
            try {
                RelGjb299dCache cache = new RelGjb299dCache();
                cache.setPartCategory(entry.partCategory);
                cache.setPartSubtype(entry.partSubtype);
                cache.setSectionRef(entry.sectionRef);
                cache.setParamName(entry.paramName);
                cache.setKeyValues(toJsonString(entry.keyValues));
                cache.setResultValue(entry.resultValue);
                cache.setTableRef(entry.tableRef);
                cache.setTableRowIndex(entry.tableRowIndex);
                cache.setNotes(entry.notes);
                cacheMapper.insert(cache);
                count++;
            } catch (Exception e) {
                // Duplicate key → skip
                log.debug("Skip duplicate: {}/{}/{}/{}",
                    entry.partCategory, entry.partSubtype, entry.paramName, entry.keyValues);
            }
        }
        log.info("Imported {} cache entries (skipped {} duplicates)",
            count, entries.size() - count);
        return count;
    }

    /**
     * 导入单条缓存记录。
     */
    public void importOne(String partCategory, String partSubtype, String sectionRef,
                           String paramName, Map<String, Object> keyValues,
                           double resultValue, String tableRef, Integer rowIndex) {
        RelGjb299dCache cache = new RelGjb299dCache();
        cache.setPartCategory(partCategory);
        cache.setPartSubtype(partSubtype);
        cache.setSectionRef(sectionRef);
        cache.setParamName(paramName);
        cache.setKeyValues(toJsonString(keyValues));
        cache.setResultValue(resultValue);
        cache.setTableRef(tableRef);
        cache.setTableRowIndex(rowIndex);
        cacheMapper.insert(cache);
    }

    /**
     * 清除指定类别的所有缓存（重新导入前使用）。
     */
    public int clearCategory(String partCategory) {
        // MyBatis-Plus delete by map
        Map<String, Object> map = new HashMap<>();
        map.put("part_category", partCategory);
        return cacheMapper.deleteByMap(map);
    }

    /**
     * 获取缓存统计。
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", cacheMapper.selectCount(null));
        // Could add per-category counts via GROUP BY query
        return stats;
    }

    // ===== 辅助方法 =====

    private String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object v = entry.getValue();
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else if (v instanceof Double) {
                sb.append(String.format("%.4f", (Double) v));
            } else {
                sb.append(v);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    // ===== 数据类 =====

    public static class CacheEntry {
        public String partCategory;
        public String partSubtype;
        public String sectionRef;
        public String paramName;
        public Map<String, Object> keyValues = new LinkedHashMap<>();
        public double resultValue;
        public String tableRef;
        public Integer tableRowIndex;
        public String notes;

        public CacheEntry() {}

        public CacheEntry(String partCategory, String partSubtype, String paramName,
                           Map<String, Object> keyValues, double resultValue, String tableRef) {
            this.partCategory = partCategory;
            this.partSubtype = partSubtype;
            this.paramName = paramName;
            this.keyValues = keyValues;
            this.resultValue = resultValue;
            this.tableRef = tableRef;
        }

        /** Helper for creating entries with T (temperature) key */
        public static CacheEntry lambdaB(String category, String subtype, String section,
                                          double temp, double stress, double value, String table) {
            Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("T", temp);
            keys.put("S", stress);
            CacheEntry e = new CacheEntry(category, subtype, "lambda_b", keys, value, table);
            e.sectionRef = section;
            return e;
        }

        /** Helper for pi_T (temperature stress coefficient) */
        public static CacheEntry piT(String category, String subtype, String section,
                                      double temp, double value, String table) {
            Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("T", temp);
            CacheEntry e = new CacheEntry(category, subtype, "pi_T", keys, value, table);
            e.sectionRef = section;
            return e;
        }

        /** Helper for pi_E (environment coefficient) */
        public static CacheEntry piE(String category, String subtype, String section,
                                      String env, double value, String table) {
            Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("env", env);
            CacheEntry e = new CacheEntry(category, subtype, "pi_E", keys, value, table);
            e.sectionRef = section;
            return e;
        }

        /** Helper for pi_Q (quality coefficient) */
        public static CacheEntry piQ(String category, String subtype, String section,
                                      String qualityLevel, double value, String table) {
            Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("quality", qualityLevel);
            CacheEntry e = new CacheEntry(category, subtype, "pi_Q", keys, value, table);
            e.sectionRef = section;
            return e;
        }

        /** Helper for C1/C2 complexity coefficients (数字/MOS IC) */
        public static CacheEntry complexityC(String category, String subtype, String section,
                                              String paramName, int gateCount, double value, String table) {
            Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("NG", gateCount);
            CacheEntry e = new CacheEntry(category, subtype, paramName, keys, value, table);
            e.sectionRef = section;
            return e;
        }
    }
}
