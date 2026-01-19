package com.email.writer.app;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                            .map(body -> new RuntimeException("Gemini API Error: " + body))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(40))   // ⏱ timeout fix
                    .block();

            if (response == null || response.isEmpty()) {
                return "AI did not return any response. Please try again.";
            }

            return extractResponseContent(response);

        } catch (Exception e) {
            return "AI service temporarily unavailable. Please try again later.";
        }
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            JsonNode candidates = rootNode.path("candidates");

            if (!candidates.isArray() || candidates.size() == 0) {
                return "AI did not return any valid response.";
            }

            JsonNode firstCandidate = candidates.get(0);
            if (firstCandidate == null) {
                return "Empty AI response.";
            }

            JsonNode content = firstCandidate.path("content");
            JsonNode parts = content.path("parts");

            if (!parts.isArray() || parts.size() == 0) {
                return "AI response format changed.";
            }

            return parts.get(0).path("text").asText("No text generated.");

        } catch (Exception e) {
            return "Error processing AI response.";
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
