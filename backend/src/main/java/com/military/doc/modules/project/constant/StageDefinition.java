package com.military.doc.modules.project.constant;

import java.util.List;

public record StageDefinition(String code, String name, int order, String defaultBaselineType, String description) {

    public static final List<StageDefinition> ALL = List.of(
        new StageDefinition("ARGUMENTATION",  "论证阶段",   1, "FUNCTIONAL_BASELINE", "识别技术状态项，建立功能基线(FBL)"),
        new StageDefinition("SCHEME",         "方案阶段",   2, "ALLOCATED_BASELINE",  "建立分配基线(ABL)"),
        new StageDefinition("PROTOTYPE",      "初样阶段",   3, "ALLOCATED_BASELINE",  "初样设计制造、性能验证试验"),
        new StageDefinition("FORMAL_SAMPLE",  "正样阶段",   4, "PRODUCT_BASELINE",    "正样设计制造、鉴定试验"),
        new StageDefinition("FINALIZATION",   "定型阶段",   5, "PRODUCT_BASELINE",    "状态鉴定，建立产品基线(PBL)"),
        new StageDefinition("PRODUCTION",     "生产阶段",   6, "PRODUCT_BASELINE",    "按合同生产交付"),
        new StageDefinition("MAINTENANCE",    "使用维护阶段", 7, null,                  "使用、维修、退役报废")
    );

    public static StageDefinition findByCode(String code) {
        return ALL.stream().filter(s -> s.code().equals(code)).findFirst().orElse(null);
    }

    public static StageDefinition findByOrder(int order) {
        return ALL.stream().filter(s -> s.order() == order).findFirst().orElse(null);
    }
}
