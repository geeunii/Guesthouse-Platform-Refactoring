package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAiGuestSummaryClient implements GuestSummaryClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiGuestSummaryClient(
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-4o-mini}") String model,
            @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.summary.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${ai.summary.read-timeout-ms:8000}") int readTimeoutMs
    ) {
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

    @Override
    public AccommodationAiSummaryResponse generate(Accommodation accommodation, List<ReviewEntity> reviews, List<String> topTags, int minPrice, boolean hasDormitory) {
        if (!isConfigured()) {
            throw new GuestSummaryAiException("OpenAI is not configured");
        }

        try {
            Map<String, Object> requestBody = buildRequest(accommodation, reviews, topTags, minPrice, hasDormitory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    entity,
                    String.class
            );

            String content = extractContent(response.getBody());
            if (content == null || content.isBlank()) {
                throw new GuestSummaryAiException("OpenAI response missing content");
            }

            return parseResponse(content, accommodation);
        } catch (HttpStatusCodeException ex) {
            log.warn("OpenAI guest summary request failed: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GuestSummaryAiException("OpenAI request failed", ex);
        } catch (RestClientException ex) {
            log.warn("OpenAI guest summary request failed: {}", ex.getMessage());
            throw new GuestSummaryAiException("OpenAI request failed", ex);
        } catch (JsonProcessingException ex) {
            log.warn("OpenAI guest summary parsing failed: {}", ex.getMessage());
            throw new GuestSummaryAiException("OpenAI response parsing failed", ex);
        }
    }

    private Map<String, Object> buildRequest(Accommodation accommodation, List<ReviewEntity> reviews, List<String> topTags, int minPrice, boolean hasDormitory) {
        String reviewText = reviews.stream()
                .limit(10)
                .map(r -> "- " + trimContent(r.getContent()))
                .collect(Collectors.joining("\n"));
        
        String tags = String.join(", ", topTags);

        String prompt = String.format(Locale.KOREA,
                "너는 여행객을 위해 숙소의 매력을 핵심만 짚어주는 친절한 가이드야.\n" +
                        "입력 데이터를 바탕으로 JSON 형식으로 요약해줘.\n" +
                        "출력 필드: locationTag, keywords(배열), moodDescription, tip\n" +
                        "moodDescription은 HTML 태그(<strong>, <br>)를 사용하여 강조해줘.\n\n" +
                        "[숙소 정보]\n" +
                        "- 이름: %s\n" +
                        "- 주소: %s\n" +
                        "- 최저가: %d원\n" +
                        "- 도미토리 유무: %s\n" +
                        "- 상위 태그: %s\n" +
                        "- 소개글: %s\n\n" +
                        "[최신 리뷰]\n%s",
                accommodation.getAccommodationsName(),
                accommodation.getCity() + " " + accommodation.getDistrict(),
                minPrice,
                hasDormitory ? "있음" : "없음",
                tags,
                accommodation.getAccommodationsDescription(),
                reviewText
        );

        Map<String, Object> system = Map.of("role", "system", "content", "너는 여행객을 위한 친절한 숙소 가이드야. JSON으로만 응답해.");
        Map<String, Object> user = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(system, user));
        body.put("temperature", 0.7);
        body.put("max_tokens", 800);
        body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private String trimContent(String content) {
        if (content == null) return "";
        String trimmed = content.replaceAll("\\s+", " ").trim();
        if (trimmed.length() > 100) {
            return trimmed.substring(0, 100) + "...";
        }
        return trimmed;
    }

    private String extractContent(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) return null;
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) return null;
        return content.asText();
    }

    private AccommodationAiSummaryResponse parseResponse(String content, Accommodation accommodation) throws JsonProcessingException {
        // 내부 DTO를 사용하여 안전하게 파싱
        OpenAiResponseDto dto = objectMapper.readValue(content, OpenAiResponseDto.class);
        
        String locationTag = dto.getLocationTag() != null ? dto.getLocationTag() : "제주 여행의 중심";
        List<String> keywords = dto.getKeywords() != null ? dto.getKeywords() : List.of("#제주감성");
        String moodDescription = dto.getMoodDescription() != null ? dto.getMoodDescription() : "편안한 휴식을 제공하는 숙소입니다.";
        String tip = dto.getTip() != null ? dto.getTip() : "인기 숙소이니 예약을 서두르세요!";

        return new AccommodationAiSummaryResponse(
                accommodation.getAccommodationsName(),
                locationTag,
                keywords,
                moodDescription,
                tip,
                0 // Service에서 채움
        );
    }

    // JSON 파싱용 내부 DTO
    @Getter
    @NoArgsConstructor
    private static class OpenAiResponseDto {
        private String locationTag;
        private List<String> keywords;
        private String moodDescription;
        private String tip;
    }
}
