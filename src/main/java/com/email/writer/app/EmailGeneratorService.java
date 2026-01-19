package com.email.writer.app;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient webClient) {
        this.webClient = webClient;
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        try {
            String prompt = buildPrompt(emailRequest);

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", prompt)
                        )
                    )
                )
            );

            String response = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(40))
                    .block();

            if (response == null || response.isEmpty()) {
                return "⚠️ Empty response from AI.";
            }

            return extractResponseContent(response);

        } catch (WebClientResponseException.Unauthorized e) {
            return "🔑 Invalid API key. Please check configuration.";

        } catch (WebClientResponseException.TooManyRequests e) {
            return "📛 API quota limit reached. Try later.";

        } catch (WebClientResponseException.ServiceUnavailable e) {
            return "🚫 AI server is currently down.";

        } catch (WebClientRequestException e) {
            return "🌐 Network issue or AI server unreachable.";

        } catch (Exception e) {
            return "⚠️ Unknown error: " + e.getMessage();
        }
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            JsonNode candidates = rootNode.path("candidates");

            if (!candidates.isArray() || candidates.size() == 0) {
                return "⚠️ No AI candidates found.";
            }

            JsonNode content = candidates.get(0).path("content").path("parts");

            if (!content.isArray() || content.size() == 0) {
                return "⚠️ AI response format changed.";
            }

            return content.get(0).path("text").asText("⚠️ No text generated.");

        } catch (Exception e) {
            return "⚠️ Error parsing AI response.";
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply. Do not include subject. ");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone. ");
        }

        prompt.append("\nOriginal email:\n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}