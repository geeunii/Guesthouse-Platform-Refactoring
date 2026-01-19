package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAiSummaryClient implements AiSummaryClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Pattern NUMBERED_BULLET_MATCH = Pattern.compile("^\\d+\\..*");
    private static final Pattern NUMBERED_BULLET_PREFIX = Pattern.compile("^\\d+\\.\\s*");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiSummaryClient(
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
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        HostReviewAiSummaryResponse base = new HostReviewAiSummaryResponse();
        base.setAccommodationId(request.getAccommodationId());
        base.setFrom(summary.getFrom());
        base.setTo(summary.getTo());
        base.setGeneratedAt(ZonedDateTime.now(KST).toString());
        base.setOverview(List.of());
        base.setPositives(List.of());
        base.setNegatives(List.of());
        base.setActions(List.of());
        base.setRisks(List.of());

        if (!isConfigured()) {
            throw new HostReportAiException("OpenAI is not configured");
        }

        try {
            Map<String, Object> requestBody = buildRequest(summary, request);

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
                throw new HostReportAiException("OpenAI response missing content");
            }

            HostReviewAiSummaryResponse parsed = parseResponse(content, base);
            return parsed;
        } catch (HttpStatusCodeException ex) {
            log.warn("OpenAI summary request failed: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new HostReportAiException("OpenAI request failed", ex);
        } catch (RestClientException ex) {
            log.warn("OpenAI summary request failed: {}", ex.getMessage());
            throw new HostReportAiException("OpenAI request failed", ex);
        } catch (JsonProcessingException ex) {
            log.warn("OpenAI summary parsing failed: {}", ex.getMessage());
            throw new HostReportAiException("OpenAI response parsing failed", ex);
        }
    }

    // 변경된 DTO 생성자에 맞춰 더미 데이터 반환 (현재 사용하지 않는 메소드)
    public AccommodationAiSummaryResponse generateGuestSummary(String description, List<ReviewEntity> reviews) {
        return new AccommodationAiSummaryResponse(
                "숙소 이름",
                "위치 태그",
                List.of("#키워드1", "#키워드2"),
                "분위기 설명",
                "이용 꿀팁",
                0
        );
    }

    private Map<String, Object> buildGuestRequest(String description, List<ReviewEntity> reviews) {
        String reviewText = reviews.stream()
                .limit(10)
                .map(r -> "- " + trimContent(r.getContent()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format(Locale.KOREA,
                "너는 여행객을 위해 숙소의 매력을 핵심만 짚어주는 친절한 가이드야.\n" +
                        "아래 제공된 숙소 소개글과 실제 투숙객 리뷰를 바탕으로 숙소를 요약해줘.\n" +
                        "출력은 줄글이 아닌, 이모지와 함께 3가지 핵심 포인트(분위기, 장점, 추천 대상)로 요약해서 보여줘.\n" +
                        "마크다운 포맷은 사용하지 말고, 텍스트로만 출력해.\n\n" +
                        "[숙소 소개글]\n%s\n\n" +
                        "[최신 리뷰]\n%s",
                description != null ? description : "정보 없음",
                reviewText.isEmpty() ? "리뷰 없음" : reviewText
        );

        Map<String, Object> system = Map.of("role", "system", "content", "너는 여행객을 위한 친절한 숙소 가이드야.");
        Map<String, Object> user = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(system, user));
        body.put("temperature", 0.7);
        body.put("max_tokens", 500);
        return body;
    }

    private Map<String, Object> buildRequest(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        String accommodationName = extractAccommodationName(summary, request);
        String tagLine = summary.getTopTags() == null ? "" : summary.getTopTags().stream()
                .limit(10)
                .map(tag -> tag.getTagName() + ":" + tag.getCount())
                .collect(Collectors.joining(", "));
        String recentReviews = buildRecentReviews(summary.getRecentReviews());

        String prompt = String.format(Locale.KOREA,
                "너는 게스트하우스 호스트를 위한 운영 컨설턴트다.\n" +
                        "입력 데이터(평점/리뷰/태그/기간)만 근거로, 과장/추측 없이 실무적으로 요약해라.\n" +
                        "출력은 반드시 JSON 객체만 반환하며 코드펜스나 추가 설명을 포함하지 않는다.\n" +
                        "형식은 {\"overview\":string[],\"positives\":string[],\"negatives\":string[],\"actions\":string[],\"risks\":string[]}이다.\n\n" +
                        "[입력]\n" +
                        "- 기간: %2$s ~ %3$s\n" +
                        "- 숙소명: %1$s\n" +
                        "- 리뷰 수: %4$d\n" +
                        "- 평균 평점: %5$.2f\n" +
                        "- 별점 분포: %6$s\n" +
                        "- 상위 태그 TOP10: %7$s\n" +
                        "- 최근 리뷰 샘플(최대 10개):\n%8$s\n\n" +
                        "[출력 규칙]\n" +
                        "- overview: 1~2문장, 60자 이내\n" +
                        "- positives/negatives: 각 최대 3개, 1줄, 20단어 이내\n" +
                        "- negatives는 없으면 \"유의미한 부정 신호 없음\"을 포함\n" +
                        "- actions: 최대 5개, \"무엇을/어떻게\" 포함\n" +
                        "- risks: 최대 2개\n" +
                        "- 데이터가 부족하면 \"데이터 부족\"을 명시",
                accommodationName,
                summary.getFrom(),
                summary.getTo(),
                summary.getReviewCount() != null ? summary.getReviewCount() : 0,
                summary.getAvgRating() != null ? summary.getAvgRating() : 0.0,
                summary.getRatingDistribution(),
                tagLine,
                recentReviews
        );

        Map<String, Object> system = Map.of("role", "system", "content", "너는 호스트 숙소 리뷰 리포트 분석가다.");
        Map<String, Object> user = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(system, user));
        body.put("temperature", 0.4);
        body.put("max_tokens", 800);
        body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private String extractAccommodationName(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        if (request != null && request.getAccommodationId() == null) {
            return "전체 숙소";
        }
        if (summary.getRecentReviews() == null || summary.getRecentReviews().isEmpty()) {
            return "해당 숙소";
        }
        String name = summary.getRecentReviews().get(0).getAccommodationName();
        return name == null || name.isBlank() ? "해당 숙소" : name;
    }

    private String buildRecentReviews(List<HostReviewReportRecentRow> reviews) {
        if (reviews == null || reviews.isEmpty()) return "리뷰 데이터 없음";
        return reviews.stream()
                .limit(10)
                .map(review -> String.format("- %s점: %s", formatRating(review.getRating()), trimContent(review.getContent())))
                .collect(Collectors.joining("\n"));
    }

    private String formatRating(Double rating) {
        if (rating == null) return "0";
        return String.format(Locale.KOREA, "%.1f", rating);
    }

    private String trimContent(String content) {
        if (content == null) return "";
        String trimmed = content.replaceAll("\\s+", " ").trim();
        if (trimmed.length() > 200) {
            return trimmed.substring(0, 200) + "...";
        }
        return trimmed;
    }

    private String extractContent(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) return null;
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) return null;
        String value = content.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private HostReviewAiSummaryResponse parseResponse(String content, HostReviewAiSummaryResponse base) throws JsonProcessingException {
        String trimmed = stripCodeFence(content).trim();
        Map<String, List<String>> parsed;
        if (trimmed.startsWith("{")) {
            parsed = objectMapper.readValue(trimmed, new TypeReference<Map<String, List<String>>>() {});
        } else {
            parsed = parseMarkdownSummary(trimmed);
        }
        if (!hasRequiredFields(parsed)) {
            throw new HostReportAiException("OpenAI response missing required fields");
        }

        HostReviewAiSummaryResponse response = new HostReviewAiSummaryResponse();
        response.setAccommodationId(base.getAccommodationId());
        response.setFrom(base.getFrom());
        response.setTo(base.getTo());
        response.setGeneratedAt(base.getGeneratedAt());
        response.setOverview(parsed.getOrDefault("overview", List.of()));
        response.setPositives(parsed.getOrDefault("positives", List.of()));
        response.setNegatives(parsed.getOrDefault("negatives", List.of()));
        response.setActions(parsed.getOrDefault("actions", List.of()));
        response.setRisks(parsed.getOrDefault("risks", List.of()));
        return response;
    }

    private boolean hasRequiredFields(Map<String, List<String>> parsed) {
        if (parsed == null) return false;
        return parsed.containsKey("overview")
                && parsed.containsKey("positives")
                && parsed.containsKey("negatives")
                && parsed.containsKey("actions")
                && parsed.containsKey("risks");
    }

    private Map<String, List<String>> parseMarkdownSummary(String content) {
        Map<String, List<String>> parsed = new HashMap<>();
        String current = null;
        for (String raw : content.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("## ")) {
                current = mapHeading(line.substring(3).trim());
                continue;
            }
            if (current == null || line.isEmpty()) continue;
            String item = extractBullet(line);
            if (item == null || item.isBlank()) continue;
            parsed.computeIfAbsent(current, key -> new java.util.ArrayList<>()).add(item);
        }
        return parsed;
    }

    private String mapHeading(String heading) {
        if (heading.contains("총평")) return "overview";
        if (heading.contains("좋았던")) return "positives";
        if (heading.contains("아쉬운")) return "negatives";
        if (heading.contains("즉시") || heading.contains("액션")) return "actions";
        if (heading.contains("주의") || heading.contains("리스크")) return "risks";
        return null;
    }

    private String extractBullet(String line) {
        String normalized = line;
        if (normalized.startsWith("-")) {
            normalized = normalized.substring(1).trim();
        } else if (NUMBERED_BULLET_MATCH.matcher(normalized).matches()) {
            normalized = NUMBERED_BULLET_PREFIX.matcher(normalized).replaceFirst("");
        } else {
            return null;
        }
        if (normalized.startsWith("[ ]")) {
            normalized = normalized.substring(3).trim();
        }
        return normalized;
    }

    private String stripCodeFence(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed.replace("```", "");
    }
}
