package com.ssg9th2team.geharbang.domain.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    // [수정] LKM KEY
    @Value("${gemini.api.key.1}")
    private String apiKey1;

    // [수정] KHG KEY
    @Value("${gemini.api.key.2}")
    private String apiKey2;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper;

    // [중요] new RestTemplate() 삭제 -> 생성자 주입으로 변경 (502 에러 방지)
    private final RestTemplate restTemplate;

    // 랜덤 선택을 위한 객체
    private final Random random = new Random();

    public String generateContent(String promptText) {
        // [로직 추가] 요청할 때마다 키 랜덤 선택 (50:50 확률)
        String selectedKey = random.nextBoolean() ? apiKey1 : apiKey2;

        try {
            // [수정] 선택된 키를 URL 뒤에 붙임
            String finalUrl = apiUrl.trim() + "?key=" + selectedKey.trim();

            // 디버깅용 로그 (키 앞 5자리만 출력해서 확인)
            log.info("Gemini Request with Key: {}... | URL: {}", selectedKey.substring(0, 5), finalUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            GeminiRequest requestDto = new GeminiRequest(List.of(
                    new Content(List.of(new Part(promptText)))
            ));
            String requestBody = objectMapper.writeValueAsString(requestDto);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(finalUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseAndSanitize(response.getBody());
            } else {
                throw new RuntimeException("Gemini API Error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Gemini Client Error (Key: " + selectedKey.substring(0, 5) + "...", e);
            throw new RuntimeException("Gemini Call Failed", e);
        }
    }

    private String parseAndSanitize(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode()) return "{}"; // NPE 방지

            String text = textNode.asText();
            // 마크다운 코드 블록 제거
            if (text.startsWith("```json")) text = text.substring(7);
            if (text.startsWith("```")) text = text.substring(3);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);

            return text.trim();
        } catch (Exception e) {
            log.error("Parsing Error", e);
            return "{}";
        }
    }

    // --- DTO ---
    @Data
    static class GeminiRequest {
        private final List<Content> contents;
    }

    @Data
    static class Content {
        private final List<Part> parts;
    }

    @Data
    static class Part {
        private final String text;
    }
}