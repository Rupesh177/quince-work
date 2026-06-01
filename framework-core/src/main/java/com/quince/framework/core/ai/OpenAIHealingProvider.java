package com.quince.framework.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quince.framework.core.healing.ElementIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class OpenAIHealingProvider implements AIHealingProvider {

    private static final Logger logger = LogManager.getLogger(OpenAIHealingProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;

    public OpenAIHealingProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Optional<By> heal(By original,
                             ElementIntent intent,
                             String domSnapshot) {

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("OpenAI API key not configured. Skipping AI healing.");
            return Optional.empty();
        }

        try {
            String prompt = buildPrompt(original, intent, domSnapshot);

            String xpath = callOpenAI(prompt);

            if (xpath == null || xpath.isBlank()) {
                logger.warn("AI healing returned empty XPath");
                return Optional.empty();
            }

            logger.info("AI healing suggested XPath: {}", xpath);

            return Optional.of(By.xpath(xpath));

        } catch (Exception e) {
            logger.warn("AI healing failed", e);
            return Optional.empty();
        }
    }

    private String callOpenAI(String prompt) throws Exception {
        String escapedPrompt = MAPPER.writeValueAsString(prompt);

        String payload = """
                {
                  "model": "gpt-4.1-mini",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are a Selenium locator healing engine. Return ONLY one valid XPath. No markdown. No explanation."
                    },
                    {
                      "role": "user",
                      "content": %s
                    }
                  ],
                  "temperature": 0
                }
                """.formatted(escapedPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.warn("OpenAI healing request failed. Status={}, Body={}",
                    response.statusCode(),
                    response.body());
            return "";
        }

        JsonNode json = MAPPER.readTree(response.body());

        return json.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("")
                .trim();
    }

    private String buildPrompt(By original,
                               ElementIntent intent,
                               String domSnapshot) {

        return """
                Failed Locator:
                %s
                
                Element Intent:
                %s
                
                DOM:
                %s
                
                Return ONLY XPath.
                """
                .formatted(
                        original,
                        intent,
                        domSnapshot
                );
    }
}