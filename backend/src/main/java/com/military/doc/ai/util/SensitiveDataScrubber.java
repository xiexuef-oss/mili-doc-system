package com.military.doc.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏处理器。
 * 在发送到外部 LLM 之前自动替换装备型号、战术指标、人名等敏感信息为占位符，
 * 在 LLM 返回结果后恢复原始值。
 *
 * <p>使用 ThreadLocal 存储替换映射，确保多线程安全。</p>
 */
@Slf4j
@Component
public class SensitiveDataScrubber {

    /** 是否启用脱敏（配置注入） */
    private volatile boolean enabled = true;

    // ---- 敏感信息正则模式 ----

    /** 装备型号: 2-4个大写字母 + 连字符 + 数字组合，如 J-20, HQ-9B, YJ-12A */
    private static final Pattern EQUIPMENT_CODE = Pattern.compile(
        "\\b([A-Z]{2,4}-\\d{1,4}[A-Z]?(?:-\\d{1,3})?)\\b");

    /** 战术指标数值: 射程/精度/速度/高度/重量 + 数字 + 单位 */
    private static final Pattern TACTICAL_SPEC = Pattern.compile(
        "(射程|精度|速度|飞行高度|作战半径|最大航程|有效载荷|起飞重量|推力|探测距离|跟踪距离)\\s*[:：]?\\s*(\\d+(?:\\.\\d+)?)\\s*(公里|千米|米|秒|马赫|吨|千克|kg|km|m|s|Ma|t)?");

    /** 频率/波段: 数字 + GHz/MHz/KHz */
    private static final Pattern FREQUENCY = Pattern.compile(
        "\\b(\\d+(?:\\.\\d+)?)\\s*(GHz|MHz|KHz|吉赫|兆赫|千赫)\\b");

    /** 疑似人名: 中文姓氏(1-2字) + 名(1-2字) + 头衔，如 张某某总师 */
    private static final Pattern PERSONNEL = Pattern.compile(
        "([王李张刘陈杨黄赵周吴徐孙马胡朱郭何罗高林郑梁谢唐许冯宋韩邓彭曹曾萧])([\\u4e00-\\u9fa5]{1,2})\\s*(总师|总工|主任|所长|院长|将军|司令|政委|部长)");

    /** 具体日期: YYYY年MM月DD日 格式 */
    private static final Pattern SPECIFIC_DATE = Pattern.compile(
        "(\\d{4})年(\\d{1,2})月(\\d{1,2})日");

    // ThreadLocal 存储当前请求的替换映射
    private final ThreadLocal<Map<String, String>> placeholderMap = ThreadLocal.withInitial(LinkedHashMap::new);
    private final ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 对文本进行脱敏处理，将敏感信息替换为占位符。
     * @param text 原始文本
     * @return 脱敏后的文本
     */
    public String scrub(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return text;
        }
        Map<String, String> map = placeholderMap.get();
        map.clear();
        counter.set(0);
        String result = text;

        try {
            result = scrubPattern(result, EQUIPMENT_CODE, "装备型号");
            result = scrubPattern(result, TACTICAL_SPEC, "战术指标");
            result = scrubPattern(result, FREQUENCY, "频率参数");
            result = scrubPattern(result, PERSONNEL, "人员");
            result = scrubPattern(result, SPECIFIC_DATE, "日期");

            if (!map.isEmpty()) {
                log.debug("Desensitized {} sensitive fields: {}", map.size(), map.keySet());
            }
        } catch (Exception e) {
            log.warn("Desensitization error, returning original text: {}", e.getMessage());
            return text;
        }

        return result;
    }

    /**
     * 对 LLM 返回的文本进行解敏，恢复原始敏感信息。
     * @param text LLM 返回的文本
     * @return 恢复后的文本
     */
    public String descrub(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return text;
        }
        Map<String, String> map = placeholderMap.get();
        if (map.isEmpty()) {
            return text;
        }
        String result = text;
        // 按占位符长度倒序替换，避免短占位符先匹配长占位符的子串
        List<Map.Entry<String, String>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (var entry : entries) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        log.debug("De-scrubbed {} fields in LLM response", map.size());
        return result;
    }

    /**
     * 获取本轮脱敏替换的字段类型列表（用于审计日志）。
     */
    public Set<String> getScrubbedFieldTypes() {
        Map<String, String> map = placeholderMap.get();
        if (map.isEmpty()) return Set.of();
        Set<String> types = new LinkedHashSet<>();
        for (String placeholder : map.keySet()) {
            int idx = placeholder.indexOf('-');
            if (idx > 0) {
                types.add(placeholder.substring(1, idx)); // strip leading [ and trailing -N]
            }
        }
        return types;
    }

    /** 清理 ThreadLocal，防止内存泄漏 */
    public void clear() {
        placeholderMap.remove();
        counter.remove();
    }

    // ---- private helpers ----

    private String scrubPattern(String text, Pattern pattern, String category) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        Map<String, String> map = placeholderMap.get();
        while (matcher.find()) {
            String matched = matcher.group();
            String placeholder = "[" + category + "-" + (counter.get() + 1) + "]";
            map.put(placeholder, matched);
            counter.set(counter.get() + 1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
