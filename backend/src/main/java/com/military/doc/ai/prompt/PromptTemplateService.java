package com.military.doc.ai.prompt;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PromptTemplateService {

    private final Map<String, String> templates = new ConcurrentHashMap<>();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    @PostConstruct
    void loadTemplates() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:prompts/*.md");
            for (Resource res : resources) {
                String filename = res.getFilename();
                if (filename == null) continue;
                String name = filename.replace(".md", "");
                String content = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                templates.put(name, content);
                log.info("Loaded prompt template: {} ({} chars)", name, content.length());
            }
            log.info("Loaded {} prompt templates", templates.size());
        } catch (IOException e) {
            log.error("Failed to load prompt templates", e);
        }
    }

    public String getTemplate(String name) {
        return templates.get(name);
    }

    public String render(String templateName, Map<String, String> variables) {
        String template = templates.get(templateName);
        if (template == null) {
            log.warn("Prompt template not found: {}", templateName);
            return "";
        }
        return renderString(template, variables);
    }

    public String renderString(String template, Map<String, String> variables) {
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = variables.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
