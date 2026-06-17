package com.military.doc.modules.project.constant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema registry for different equipment types.
 * Each equipment type has different required fields in project master data.
 */
public class MasterDataSchemaRegistry {

    private static final Map<String, Object> COMMON_EQUIPMENT_INFO = Map.of(
        "fields", List.of(
            field("equipmentName", "产品名称", "text", true, null),
            field("equipmentModel", "产品型号", "text", true, null),
            field("equipmentCategory", "产品类别", "select", true,
                List.of("常规武器", "战略武器", "卫星", "电子对抗", "通信导航", "指挥控制", "其他")),
            field("militaryUse", "军事用途", "richtext", false, null),
            field("systemBoundary", "系统边界描述", "richtext", false, null)
        )
    );

    private static final Map<String, Object> COMMON_TACTICAL_INDICATORS = Map.of(
        "fields", List.of(
            field("indicatorName", "指标名称", "text", true, null),
            field("requirementValue", "要求值", "text", true, null),
            field("measuredValue", "实测值", "text", false, null),
            field("unit", "单位", "text", false, null),
            field("testMethod", "验证方法", "select", false,
                List.of("试验", "演示", "检查", "分析", "仿真")),
            field("conclusion", "符合性结论", "select", false,
                List.of("符合", "基本符合", "不符合", "待验证"))
        )
    );

    private static final Map<String, Object> COMMON_PRODUCT_TREE = Map.of(
        "fields", List.of(
            field("nodeName", "产品名称", "text", true, null),
            field("nodeCode", "产品代号", "text", true, null),
            field("nodeLevel", "层级", "select", true,
                List.of("系统", "分系统", "设备", "组件", "零件")),
            field("parentNodeCode", "上级产品代号", "text", false, null),
            field("isKeyItem", "是否关键件", "boolean", false, null),
            field("responsibleUnit", "责任单位", "text", false, null)
        )
    );

    private static final Map<String, Object> COMMON_TEAM = Map.of(
        "fields", List.of(
            field("role", "角色", "select", true,
                List.of("总设计师", "副总设计师", "主任设计师", "主管设计师", "设计师",
                       "工艺师", "质量师", "标准化师", "可靠性工程师", "项目管理人员")),
            field("name", "姓名", "text", true, null),
            field("organization", "所属单位", "text", true, null),
            field("contactPhone", "联系电话", "text", false, null),
            field("duties", "职责描述", "text", false, null)
        )
    );

    private static final Map<String, Object> COMMON_MILESTONES = Map.of(
        "fields", List.of(
            field("milestoneName", "里程碑名称", "text", true, null),
            field("plannedDate", "计划日期", "date", true, null),
            field("actualDate", "实际日期", "date", false, null),
            field("stageCode", "对应阶段", "select", true,
                List.of("L", "F", "C", "S", "D", "P", "N")),
            field("deliverables", "交付物", "richtext", false, null),
            field("acceptanceCriteria", "完成标准", "richtext", false, null)
        )
    );

    public static Map<String, Object> getSchema(String equipmentType) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("equipmentInfo", COMMON_EQUIPMENT_INFO);
        schema.put("tacticalIndicators", COMMON_TACTICAL_INDICATORS);
        schema.put("productTree", COMMON_PRODUCT_TREE);
        schema.put("milestones", COMMON_MILESTONES);
        return schema;
    }

    public static Map<String, Object> getDefaultMasterData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("equipmentInfo", Map.of("equipmentName", "", "equipmentModel", "", "equipmentCategory", ""));
        data.put("tacticalIndicators", List.of());
        data.put("productTree", List.of());
        data.put("milestones", List.of());
        data.put("extendedFields", Map.of());
        return data;
    }

    private static Map<String, Object> field(String name, String label, String type, boolean required, Object options) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("label", label);
        f.put("type", type);
        f.put("required", required);
        if (options != null) f.put("options", options);
        return f;
    }
}
