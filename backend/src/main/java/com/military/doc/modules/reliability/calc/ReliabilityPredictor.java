package com.military.doc.modules.reliability.calc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.reliability.entity.RelGjb299dCache;
import com.military.doc.modules.reliability.mapper.RelGjb299dCacheMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GJB/Z 299D-2024 元器件应力分析可靠性预计引擎。
 * 
 * 计算流程:
 * 1. 逐器件查 rel_gjb299d_cache 获取 λ_b 和各 π 系数
 * 2. λ_p = λ_b × π_E × π_Q × π_T × π_S × π_L × π_C × π_A × π_K × π_CVC × ...
 *    (不同器件类别公式不同，此处给出通用形式)
 * 3. λ_total = Σ(λ_p_i × quantity_i)
 * 4. MTBF = 1 / λ_total (× 10^6 因为 λ 单位为 10⁻⁶/h)
 */
@Slf4j
@Component
public class ReliabilityPredictor {

    private final RelGjb299dCacheMapper cacheMapper;

    public ReliabilityPredictor(RelGjb299dCacheMapper cacheMapper) {
        this.cacheMapper = cacheMapper;
    }

    // ===== 环境类别常量（GJB/Z 299D 表6） =====
    public static final String ENV_G_FIX = "G_FIX";     // 地面固定
    public static final String ENV_G_MOB = "G_MOB";     // 地面移动
    public static final String ENV_N_FIX = "N_FIX";     // 舰船固定
    public static final String ENV_N_MOB = "N_MOB";     // 舰船移动
    public static final String ENV_A_IF = "A_IF";       // 机载 inhabited fighter
    public static final String ENV_A_UF = "A_UF";       // 机载 uninhabited fighter
    public static final String ENV_S_F = "S_F";         // 航天飞行
    public static final String ENV_M_F = "M_F";         // 导弹飞行

    /**
     * 元器件应力分析法预计。
     * @param items BOM 清单中的器件列表
     * @param environment 环境类别 (如 G_FIX, A_IF)
     * @return 预计结果
     */
    public PredictionResult predictByStressAnalysis(
            List<PredictionInputItem> items, String environment) {

        double totalLambda = 0.0;
        List<ItemPredictionResult> itemResults = new ArrayList<>();

        for (PredictionInputItem item : items) {
            ItemPredictionResult ir = predictSingleItem(item, environment);
            itemResults.add(ir);
            totalLambda += ir.lambdaP * item.quantity;
        }

        double mtbf = totalLambda > 0 ? 1_000_000.0 / totalLambda : Double.POSITIVE_INFINITY;

        return new PredictionResult(totalLambda, mtbf, itemResults);
    }

    /**
     * 预计单个器件。
     */
    private ItemPredictionResult predictSingleItem(PredictionInputItem item, String environment) {
        double lambdaB = lookupLambdaB(item.category, item.subtype, item.temperature, item.stressRatio);
        double piE = lookupPi(item.category, item.subtype, "pi_E", Map.of("env", environment));
        double piQ = lookupPi(item.category, item.subtype, "pi_Q", Map.of("quality", item.qualityLevel));
        double piT = lookupPi(item.category, item.subtype, "pi_T", Map.of("T", item.temperature));
        double piS = item.stressRatio > 0
            ? lookupPi(item.category, item.subtype, "pi_S", Map.of("S", item.stressRatio))
            : 1.0;

        // 复杂度系数（仅微电路、存储器类器件）
        double piL = (isIntegratedCircuit(item.category))
            ? lookupPi(item.category, item.subtype, "pi_L",
                buildComplexityKey(item))
            : 1.0;

        // 封装复杂度（仅混合电路）
        double piC = ("混合集成电路".equals(item.subtype))
            ? lookupPi(item.category, item.subtype, "pi_C", Collections.emptyMap())
            : 1.0;

        // 通用公式（不同类型器件可能去掉不适用系数）
        double lambdaP = lambdaB * piE * piQ * piT * piS * piL * piC;

        return new ItemPredictionResult(
            item.partName, item.category, item.subtype,
            item.quantity, lambdaB, piE, piQ, piT, piS, piL, piC, lambdaP);
    }

    /**
     * 查表获取基本失效率 λ_b (10⁻⁶/h)。
     * @param category 器件大类
     * @param subtype 子类型
     * @param temperature 工作温度(℃)
     * @param stressRatio 电应力比 S
     */
    public double lookupLambdaB(String category, String subtype, double temperature, double stressRatio) {
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("T", Math.round(temperature));
        keys.put("S", Math.round(stressRatio * 100.0) / 100.0);  // 保留2位
        return lookupCache(category, subtype, "lambda_b", keys);
    }

    /**
     * 查表获取 π 系数。
     * @param category 器件大类
     * @param subtype 子类
     * @param factorType 系数类型: pi_E, pi_Q, pi_T, pi_S, pi_L, pi_C...
     * @param conditions 查表条件键值对
     */
    public double lookupPi(String category, String subtype, String factorType,
                            Map<String, Object> conditions) {
        return lookupCache(category, subtype, factorType, conditions);
    }

    /**
     * 从 rel_gjb299d_cache 查表。
     * 键值对序列化为 JSONB 后进行匹配。
     */
    private double lookupCache(String category, String subtype, String paramName,
                                Map<String, Object> keys) {
        if (cacheMapper == null) {
            log.warn("Cache mapper not available, returning 0 for {}/{}/{}", category, subtype, paramName);
            return 0.0;
        }

        // 构建 JSONB 查询条件
        // PostgreSQL JSONB 查询: key_values @> '{"T":25,"S":0.5}'::jsonb
        StringBuilder jsonbFilter = new StringBuilder("{");
        boolean first = true;
        for (var entry : keys.entrySet()) {
            if (!first) jsonbFilter.append(", ");
            jsonbFilter.append("\"").append(entry.getKey()).append("\":");
            Object v = entry.getValue();
            if (v instanceof Double || v instanceof Float) {
                jsonbFilter.append(v);
            } else if (v instanceof Integer || v instanceof Long) {
                jsonbFilter.append(v);
            } else {
                jsonbFilter.append("\"").append(v).append("\"");
            }
            first = false;
        }
        jsonbFilter.append("}");

        try {
            var query = new LambdaQueryWrapper<RelGjb299dCache>()
                .eq(RelGjb299dCache::getPartCategory, category)
                .eq(RelGjb299dCache::getParamName, paramName)
                .apply("key_values @> '" + jsonbFilter + "'::jsonb");
            if (subtype != null && !subtype.isEmpty()) {
                query.eq(RelGjb299dCache::getPartSubtype, subtype);
            }
            query.last("LIMIT 1");

            RelGjb299dCache cache = cacheMapper.selectOne(query);
            if (cache != null) {
                return cache.getResultValue();
            }
        } catch (Exception e) {
            log.warn("Cache lookup failed for {}/{}/{} with keys {}",
                category, subtype, paramName, jsonbFilter, e);
        }

        log.debug("Cache miss: {}/{}/{} keys={}", category, subtype, paramName, jsonbFilter);
        return 0.0;
    }

    private boolean isIntegratedCircuit(String category) {
        return category != null && (
            category.contains("微电路") || category.contains("集成电路")
            || category.contains("IC") || category.contains("存储器")
            || category.contains("微处理器"));
    }

    private Map<String, Object> buildComplexityKey(PredictionInputItem item) {
        Map<String, Object> keys = new LinkedHashMap<>();
        if (item.gateCount != null && item.gateCount > 0) {
            keys.put("NG", item.gateCount);
        } else if (item.transistorCount != null && item.transistorCount > 0) {
            keys.put("NT", item.transistorCount);
        } else if (item.bitCount != null && item.bitCount > 0) {
            keys.put("NB", item.bitCount);
        }
        return keys;
    }

    // ===== 数据类 =====

    @Data
    public static class PredictionInputItem {
        String partName;
        String category;
        String subtype;
        int quantity = 1;
        String qualityLevel;
        double temperature = 25.0;
        double stressRatio;
        // 复杂度参数（各类器件选填）
        Integer gateCount;        // 门数 NG (数字IC)
        Integer transistorCount;  // 晶体管数 NT (模拟IC/微处理器)
        Integer bitCount;         // 位数 NB (存储器)
    }

    @Data
    public static class ItemPredictionResult {
        String partName;
        String category;
        String subtype;
        int quantity;
        double lambdaB;
        double piE, piQ, piT, piS, piL, piC;
        double lambdaP;

        public ItemPredictionResult(String partName, String category, String subtype,
                                     int quantity, double lambdaB,
                                     double piE, double piQ, double piT,
                                     double piS, double piL, double piC,
                                     double lambdaP) {
            this.partName = partName;
            this.category = category;
            this.subtype = subtype;
            this.quantity = quantity;
            this.lambdaB = lambdaB;
            this.piE = piE;
            this.piQ = piQ;
            this.piT = piT;
            this.piS = piS;
            this.piL = piL;
            this.piC = piC;
            this.lambdaP = lambdaP;
        }
    }

    @Data
    public static class PredictionResult {
        double totalFailureRate;
        double mtbf;
        List<ItemPredictionResult> items;

        public PredictionResult(double totalFailureRate, double mtbf,
                                 List<ItemPredictionResult> items) {
            this.totalFailureRate = totalFailureRate;
            this.mtbf = mtbf;
            this.items = items;
        }

        /** 序列化为 JSON，供 LLM 上下文注入 */
        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"totalFailureRate\":").append(String.format("%.6f", totalFailureRate))
              .append(",\"mtbf\":").append(String.format("%.0f", mtbf))
              .append(",\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                var it = items.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"partName\":\"").append(it.partName)
                  .append("\",\"category\":\"").append(it.category)
                  .append("\",\"quantity\":").append(it.quantity)
                  .append(",\"lambdaP\":").append(String.format("%.6f", it.lambdaP))
                  .append("}");
            }
            sb.append("]}");
            return sb.toString();
        }
    }
}
