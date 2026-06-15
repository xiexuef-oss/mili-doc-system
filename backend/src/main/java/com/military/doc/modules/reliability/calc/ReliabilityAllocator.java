package com.military.doc.modules.reliability.calc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 可靠性分配方法库。
 * 实现 5 种 GJB 450B-2021 工作项目 302 定义的分配方法。
 */
@Slf4j
@Component
public class ReliabilityAllocator {

    /**
     * 等分配法：每个单元分配相同的失效率。
     * λ_i = λ_sys / n
     * MTBF_i = n × MTBF_sys
     */
    public AllocationResult equalAllocation(double systemMtbf, int nUnits) {
        double systemLambda = 1_000_000.0 / systemMtbf;
        double allocatedLambda = systemLambda / nUnits;
        double allocatedMtbf = 1_000_000.0 / allocatedLambda;

        List<AllocationItem> items = new ArrayList<>();
        for (int i = 0; i < nUnits; i++) {
            items.add(new AllocationItem(
                "单元" + (i + 1), "SUBSYSTEM",
                allocatedLambda, allocatedMtbf, 1.0 / nUnits));
        }

        return new AllocationResult("EQUAL", systemMtbf, systemLambda, items,
            validateAllocation(systemLambda, items));
    }

    /**
     * 评分分配法：按复杂度、技术成熟度、工作时间、环境严酷度评分分配。
     * C_i = (K_co × K_ma × K_dt × K_en)_i
     * λ_i = (C_i / ΣC) × λ_sys
     */
    public AllocationResult scoringAllocation(double systemMtbf, List<ScoringUnit> units) {
        double systemLambda = 1_000_000.0 / systemMtbf;

        // 计算每个单元的评分积
        double totalScore = 0;
        for (var u : units) {
            u.compositeScore = u.complexity * u.maturity * u.dutyTime * u.environment;
            totalScore += u.compositeScore;
        }

        List<AllocationItem> items = new ArrayList<>();
        for (var u : units) {
            double ratio = u.compositeScore / totalScore;
            double allocatedLambda = ratio * systemLambda;
            double allocatedMtbf = 1_000_000.0 / allocatedLambda;
            items.add(new AllocationItem(u.name, u.level, allocatedLambda, allocatedMtbf, ratio));
        }

        return new AllocationResult("SCORING", systemMtbf, systemLambda, items,
            validateAllocation(systemLambda, items));
    }

    /**
     * AGREE 分配法：考虑单元重要度和复杂度。
     * λ_i = -ln(R*_sys) × n_i / (N × ω_i × t_i)
     * 其中: n_i=单元元器件数, N=总元器件数, ω_i=重要度[0,1], t_i=工作时间(h)
     */
    public AllocationResult agreeAllocation(double systemMtbf, List<AgreeUnit> units,
                                             double missionTime) {
        double systemLambda = 1_000_000.0 / systemMtbf;
        double R_sys = Math.exp(-systemLambda * missionTime / 1_000_000.0);
        double negLnR = -Math.log(R_sys);

        int totalParts = units.stream().mapToInt(u -> u.partCount).sum();

        List<AllocationItem> items = new ArrayList<>();
        double totalLambda = 0;
        for (var u : units) {
            double allocatedLambda = negLnR * u.partCount
                / (totalParts * u.importance * u.dutyTime);
            totalLambda += allocatedLambda;
            double allocatedMtbf = allocatedLambda > 0 ? 1_000_000.0 / allocatedLambda : Double.POSITIVE_INFINITY;
            items.add(new AllocationItem(u.name, u.level, allocatedLambda, allocatedMtbf,
                allocatedLambda / systemLambda));
        }

        return new AllocationResult("AGREE", systemMtbf, systemLambda, items,
            validateAllocation(systemLambda, items));
    }

    /**
     * ARINC 分配法：基于相似产品现场失效率数据加权分配。
     * λ_i = (λ_field_i / Σλ_field) × λ_sys
     */
    public AllocationResult arincAllocation(double systemMtbf, List<ArincUnit> units) {
        double systemLambda = 1_000_000.0 / systemMtbf;

        double totalFieldLambda = units.stream()
            .mapToDouble(u -> u.fieldLambda).sum();

        List<AllocationItem> items = new ArrayList<>();
        for (var u : units) {
            double ratio = u.fieldLambda / totalFieldLambda;
            double allocatedLambda = ratio * systemLambda;
            double allocatedMtbf = 1_000_000.0 / allocatedLambda;
            items.add(new AllocationItem(u.name, u.level, allocatedLambda, allocatedMtbf, ratio));
        }

        return new AllocationResult("ARINC", systemMtbf, systemLambda, items,
            validateAllocation(systemLambda, items));
    }

    /**
     * 比例分配法：按各单元预计故障率比例分配。
     * λ_i = (λ_pred_i / Σλ_pred) × λ_sys
     */
    public AllocationResult proportionalAllocation(double systemMtbf,
                                                    List<ProportionalUnit> units) {
        double systemLambda = 1_000_000.0 / systemMtbf;

        double totalPredLambda = units.stream()
            .mapToDouble(u -> u.predictedLambda).sum();

        List<AllocationItem> items = new ArrayList<>();
        for (var u : units) {
            double ratio = u.predictedLambda / totalPredLambda;
            double allocatedLambda = ratio * systemLambda;
            double allocatedMtbf = 1_000_000.0 / allocatedLambda;
            items.add(new AllocationItem(u.name, u.level, allocatedLambda, allocatedMtbf, ratio));
        }

        return new AllocationResult("PROPORTIONAL", systemMtbf, systemLambda, items,
            validateAllocation(systemLambda, items));
    }

    /**
     * 验证分配结果：Σλ_i ≤ λ_sys（含浮点容差）。
     */
    private boolean validateAllocation(double systemLambda, List<AllocationItem> items) {
        double sum = items.stream().mapToDouble(i -> i.allocatedLambda).sum();
        return sum <= systemLambda * 1.001;  // 0.1% tolerance
    }

    // ===== 输入数据类 =====

    @Data
    public static class ScoringUnit {
        public String name;
        public String level = "SUBSYSTEM";
        public double complexity = 1.0;    // 复杂度因子
        public double maturity = 1.0;      // 技术成熟度因子
        public double dutyTime = 1.0;      // 工作时间因子
        public double environment = 1.0;   // 环境严酷度因子
        double compositeScore;              // 综合评分（自动计算）
    }

    @Data
    public static class AgreeUnit {
        public String name;
        public String level = "SUBSYSTEM";
        public int partCount;              // 元器件数量
        public double importance;          // 重要度 [0,1]
        public double dutyTime;            // 工作时间(h)
    }

    @Data
    public static class ArincUnit {
        public String name;
        public String level = "SUBSYSTEM";
        public double fieldLambda;         // 相似产品现场失效率
    }

    @Data
    public static class ProportionalUnit {
        public String name;
        public String level = "SUBSYSTEM";
        public double predictedLambda;     // 预计失效率
    }

    // ===== 输出数据类 =====

    @Data
    public static class AllocationItem {
        String unitName;
        String unitLevel;
        double allocatedLambda;
        double allocatedMtbf;
        double allocationRatio;

        public AllocationItem(String unitName, String unitLevel, double allocatedLambda,
                               double allocatedMtbf, double allocationRatio) {
            this.unitName = unitName;
            this.unitLevel = unitLevel;
            this.allocatedLambda = allocatedLambda;
            this.allocatedMtbf = allocatedMtbf;
            this.allocationRatio = allocationRatio;
        }
    }

    @Data
    public static class AllocationResult {
        String method;
        double systemMtbf;
        double systemLambda;
        List<AllocationItem> items;
        boolean verified;

        public AllocationResult(String method, double systemMtbf, double systemLambda,
                                 List<AllocationItem> items, boolean verified) {
            this.method = method;
            this.systemMtbf = systemMtbf;
            this.systemLambda = systemLambda;
            this.items = items;
            this.verified = verified;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"method\":\"").append(method)
              .append("\",\"systemMtbf\":").append(String.format("%.0f", systemMtbf))
              .append(",\"systemLambda\":").append(String.format("%.6f", systemLambda))
              .append(",\"verified\":").append(verified)
              .append(",\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                var it = items.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"unitName\":\"").append(it.unitName)
                  .append("\",\"level\":\"").append(it.unitLevel)
                  .append("\",\"mtbf\":").append(String.format("%.0f", it.allocatedMtbf))
                  .append(",\"lambda\":").append(String.format("%.6f", it.allocatedLambda))
                  .append(",\"ratio\":").append(String.format("%.4f", it.allocationRatio))
                  .append("}");
            }
            sb.append("]}");
            return sb.toString();
        }
    }
}
