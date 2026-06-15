package com.military.doc.modules.project.constant;

import java.util.List;

/**
 * GJB 2993 规定的装备类型及其研制阶段划分。
 * 不同类型装备的阶段划分不同，所需文档清单也不同。
 */
public enum EquipmentType {
    常规武器("常规武器", "论证→方案→工程研制→设计定型→生产定型",
        List.of("ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION","MAINTENANCE")),
    战略武器("战略武器", "论证→方案→工程研制→定型",
        List.of("ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","MAINTENANCE")),
    人造卫星("人造卫星", "论证→方案→初样→正样→使用改进",
        List.of("ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","PRODUCTION","MAINTENANCE")),
    电子装备("电子装备", "论证→方案→工程研制→设计定型",
        List.of("ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","MAINTENANCE"));

    private final String label;
    private final String stageFlow;
    private final List<String> stageCodes;

    EquipmentType(String label, String stageFlow, List<String> stageCodes) {
        this.label = label;
        this.stageFlow = stageFlow;
        this.stageCodes = stageCodes;
    }

    public String getLabel() { return label; }
    public String getStageFlow() { return stageFlow; }
    public List<String> getStageCodes() { return stageCodes; }

    public static EquipmentType fromLabel(String label) {
        for (EquipmentType t : values()) {
            if (t.label.equals(label)) return t;
        }
        return null;
    }

    public static final List<EquipmentType> ALL = List.of(values());
}
