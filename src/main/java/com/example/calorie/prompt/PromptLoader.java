package com.example.calorie.prompt;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt 模板加载与渲染，模板放在 resources/prompts/*.md。
 */
@Component
public class PromptLoader {

    private static final List<String> NAMES = List.of("food_parse", "recipe_recommend", "advice");

    private final Map<String, String> templates = new HashMap<>();

    @PostConstruct
    void load() {
        for (String name : NAMES) {
            String path = "prompts/" + name + ".md";
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                templates.put(name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("加载 Prompt 模板失败: " + path, e);
            }
        }
    }

    public String render(String name, Map<String, String> variables) {
        String template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("未知的 Prompt 模板: " + name);
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    public String get(String name) {
        return templates.get(name);
    }
}
