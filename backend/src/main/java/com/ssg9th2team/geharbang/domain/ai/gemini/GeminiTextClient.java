package com.ssg9th2team.geharbang.domain.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import com.ssg9th2team.geharbang.domain.ai.gemini.exception.GeminiApiException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiTextClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiTextClient(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-1.5-flash}") String model,
            @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${ai.summary.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${ai.summary.read-timeout-ms:8000}") int readTimeoutMs) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public TextCompletionResult generateSuggestion(String prompt, String languageTag) {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }
        try {
            Map<String, Object> generationConfig = Map.of(
                    "temperature", 0.5);

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", buildPrompt(prompt, languageTag))))),
                    "generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseResponse(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.warn("Gemini text request failed: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GeminiApiException("Gemini request failed", ex);
        } catch (RestClientException ex) {
            log.warn("Gemini text request failed: {}", ex.getMessage());
            throw new GeminiApiException("Gemini request failed", ex);
        } catch (JsonProcessingException ex) {
            log.warn("Gemini text parsing failed: {}", ex.getMessage());
            throw new GeminiApiException("Gemini response parsing failed", ex);
        }
    }

    private String buildPrompt(String contextPrompt, String languageTag) {
        return "당신은 게스트하우스 기획 담당자입니다. 아래 힌트를 참고하여 숙소 이름과 소개문을 JSON으로 작성하세요.\n"
                + "출력 형식은 {\"name\":string,\"description\":string,\"confidence\":number} 입니다.\n"
                + "confidence는 0과 1 사이 숫자입니다.\n"
                + "소개문(description)은 최소 8문장 이상으로 상세하게 작성하세요. 숙소의 분위기, 위치적 장점, 주변 관광지, 편의시설, 특별한 서비스, 추천 대상, 계절별 매력 등을 풍부하게 포함해주세요.\n"
                + "응답 언어: " + languageTag + "\n\n"
                + contextPrompt;
    }

    private TextCompletionResult parseResponse(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Gemini response is empty");
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode candidateNode = root.path("candidates").path(0);
        JsonNode contentNode = candidateNode.path("content").path("parts").path(0).path("text");
        if (contentNode.isMissingNode()) {
            throw new IllegalStateException("Gemini response missing content");
        }
        String payloadText = extractJsonPayload(contentNode.asText());
        SuggestionPayload payload = objectMapper.readValue(payloadText, SuggestionPayload.class);
        JsonNode usageNode = root.path("usageMetadata");
        long promptTokens = usageNode.path("promptTokenCount").asLong(0);
        long completionTokens = usageNode.path("candidatesTokenCount").asLong(0);
        long totalTokens = usageNode.path("totalTokenCount").asLong(promptTokens + completionTokens);
        return new TextCompletionResult(
                payload.getName(),
                payload.getDescription(),
                payload.getConfidence(),
                model,
                new TokenUsage(promptTokens, completionTokens, totalTokens),
                ZonedDateTime.now(KST).toString());
    }

    private String extractJsonPayload(String responseText) {
        if (responseText == null) {
            throw new IllegalStateException("Gemini response text is null");
        }
        String trimmed = responseText.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }

    @Getter
    public static class TextCompletionResult {
        private final String name;
        private final String description;
        private final Double confidence;
        private final String model;
        private final TokenUsage tokenUsage;
        private final String generatedAt;

        public TextCompletionResult(String name, String description, Double confidence, String model,
                TokenUsage tokenUsage, String generatedAt) {
            this.name = name;
            this.description = description;
            this.confidence = confidence;
            this.model = model;
            this.tokenUsage = tokenUsage;
            this.generatedAt = generatedAt;
        }
    }

    @Getter
    public static class TokenUsage {
        private final long promptTokens;
        private final long completionTokens;
        private final long totalTokens;

        public TokenUsage(long promptTokens, long completionTokens, long totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class SuggestionPayload {
        private String name;
        private String description;
        private Double confidence;
    }
}
