package com.ssg9th2team.geharbang.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import com.ssg9th2team.geharbang.domain.accommodation_theme.repository.AccommodationThemeRepository;
import com.ssg9th2team.geharbang.domain.recommendation.dto.AiRecommendationResponse;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.ThemeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiRecommendationService {

    private final AccommodationJpaRepository accommodationRepository;
    private final AccommodationMapper accommodationMapper;
    private final AccommodationThemeRepository accommodationThemeRepository;
    private final ReviewJpaRepository reviewRepository;
    private final AiSearchLogService searchLogService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public AiRecommendationService(
            AccommodationJpaRepository accommodationRepository,
            AccommodationMapper accommodationMapper,
            AccommodationThemeRepository accommodationThemeRepository,
            ReviewJpaRepository reviewRepository,
            AiSearchLogService searchLogService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") Executor executor) {
        this.accommodationRepository = accommodationRepository;
        this.accommodationMapper = accommodationMapper;
        this.accommodationThemeRepository = accommodationThemeRepository;
        this.reviewRepository = reviewRepository;
        this.searchLogService = searchLogService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GEMINI_MODEL:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int QUERY_LIMIT = 20;
    private static final int LOCATION_QUERY_LIMIT = 50;
    private static final Set<String> GENERIC_KEYWORDS = Set.of(
            "숙소", "호텔", "펜션", "게스트하우스", "지역", "여행", "근처", "추천", "좋은", "예쁜", "좋아", "싶어");

    /**
     * 사용자 자연어 입력을 분석하여 숙소 추천
     */
    public AiRecommendationResponse recommend(String userQuery) {
        long startTime = System.currentTimeMillis();

        // 1. Gemini API로 테마 + 키워드 분석
        AnalysisResult analysisResult = analyzeUserIntent(userQuery);
        log.info("Gemini 분석 완료: {}ms", System.currentTimeMillis() - startTime);

        // 2. 병렬 검색: 테마 + 키워드(설명/리뷰) 동시 실행
        Set<Long> matchedIds = executeParallelSearch(analysisResult);
        log.info("병렬 검색 완료: {}ms, 결과 수: {}", System.currentTimeMillis() - startTime, matchedIds.size());

        // 3. N+1 문제 해결: findAllById 사용
        List<Long> idsToFetch = matchedIds.stream()
                .limit(MAX_RECOMMENDATIONS * 2)
                .collect(Collectors.toList());

        List<Accommodation> accommodations = accommodationRepository.findAllById(idsToFetch);

        // 4. 필터링 및 정렬
        List<Accommodation> finalResults = accommodations.stream()
                .filter(acc -> acc.getApprovalStatus() == ApprovalStatus.APPROVED)
                .filter(acc -> acc.getAccommodationStatus() != null && acc.getAccommodationStatus() == 1)
                .sorted((a, b) -> {
                    Double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                    Double ratingB = b.getRating() != null ? b.getRating() : 0.0;
                    return ratingB.compareTo(ratingA);
                })
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        // 5. 숙소별 테마 정보 조회 (배치)
        List<Long> resultIds = finalResults.stream()
                .map(Accommodation::getAccommodationsId)
                .collect(Collectors.toList());

        Map<Long, List<String>> themesByAccommodation = fetchThemesForAccommodations(resultIds);

        // 6. 숙소별 이미지 URL 조회 (배치)
        Map<Long, String> imagesByAccommodation = fetchThumbnailsForAccommodations(resultIds);

        // 7. Redis에 검색 로그 저장
        searchLogService.logSearch(userQuery, analysisResult.themes(), analysisResult.confidence());

        // 8. 응답 생성
        List<AiRecommendationResponse.AccommodationSummary> summaries = finalResults.stream()
                .map(acc -> toAccommodationSummary(acc,
                        themesByAccommodation.getOrDefault(acc.getAccommodationsId(), List.of()),
                        imagesByAccommodation.get(acc.getAccommodationsId())))
                .collect(Collectors.toList());

        log.info("AI 추천 완료! 쿼리: {}, 총 소요시간: {}ms", userQuery, System.currentTimeMillis() - startTime);

        return AiRecommendationResponse.builder()
                .query(userQuery)
                .matchedThemes(analysisResult.themes())
                .reasoning(analysisResult.reasoning())
                .confidence(analysisResult.confidence())
                .accommodations(summaries)
                .build();
    }

    /**
     * 숙소별 테마 정보 배치 조회
     */
    private Map<Long, List<String>> fetchThemesForAccommodations(List<Long> accommodationIds) {
        if (accommodationIds.isEmpty()) {
            return Map.of();
        }

        List<AccommodationTheme> themes = accommodationThemeRepository.findByAccommodationIds(accommodationIds);

        return themes.stream()
                .collect(Collectors.groupingBy(
                        at -> at.getAccommodation().getAccommodationsId(),
                        Collectors.mapping(at -> at.getTheme().getThemeName(), Collectors.toList())));
    }

    /**
     * 숙소별 대표 이미지 URL 배치 조회
     */
    private Map<Long, String> fetchThumbnailsForAccommodations(List<Long> accommodationIds) {
        if (accommodationIds.isEmpty()) {
            return Map.of();
        }

        List<AccommodationImageDto> images = accommodationMapper.selectMainImagesByAccommodationIds(accommodationIds);

        return images.stream()
                .filter(img -> img.getAccommodationsId() != null && img.getImageUrl() != null)
                .collect(Collectors.toMap(
                        AccommodationImageDto::getAccommodationsId,
                        AccommodationImageDto::getImageUrl,
                        (first, second) -> first));
    }

    /**
     * 병렬 검색 실행
     */
    private Set<Long> executeParallelSearch(AnalysisResult analysisResult) {
        Set<Long> contentMatchedIds = Collections.synchronizedSet(new LinkedHashSet<>());
        Set<Long> locationMatchedIds = Collections.synchronizedSet(new LinkedHashSet<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 0. 위치 기반 검색 (비동기)
        String location = analysisResult.location();
        if (location != null && !location.isBlank()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Accommodation> locationMatched = accommodationRepository.findByLocation(location);
                locationMatched.stream()
                        .limit(LOCATION_QUERY_LIMIT)
                        .forEach(acc -> locationMatchedIds.add(acc.getAccommodationsId()));
                log.info("위치 검색 결과: {} -> {}개", location, locationMatchedIds.size());
            }, executor));
        }

        // 1. 테마 기반 검색 (비동기)
        if (!analysisResult.themes().isEmpty()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Accommodation> themeMatched = accommodationRepository
                        .findByThemeCategories(analysisResult.themes());
                themeMatched.stream()
                        .limit(QUERY_LIMIT)
                        .forEach(acc -> contentMatchedIds.add(acc.getAccommodationsId()));
            }, executor));
        }

        // 2. 키워드 기반 검색 - 설명 (비동기)
        for (String keyword : analysisResult.keywords()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Accommodation> keywordMatched = accommodationRepository.findByKeywordInDescription(keyword);
                keywordMatched.stream()
                        .limit(QUERY_LIMIT / 2)
                        .forEach(acc -> contentMatchedIds.add(acc.getAccommodationsId()));
            }, executor));
        }

        // 3. 키워드 기반 검색 - 리뷰 (비동기)
        for (String keyword : analysisResult.keywords()) {
            futures.add(CompletableFuture.runAsync(() -> {
                List<Long> reviewMatchedIds = reviewRepository.findAccommodationIdsByKeywordInContent(keyword);
                reviewMatchedIds.stream()
                        .limit(QUERY_LIMIT / 2)
                        .forEach(contentMatchedIds::add);
            }, executor));
        }

        // 모든 병렬 작업 완료 대기 (최대 5초)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("병렬 검색 일부 타임아웃: {}", e.getMessage());
        }

        // 위치가 지정된 경우: 위치 결과 우선 반환 (컨텐츠 매칭은 정렬용)
        if (!locationMatchedIds.isEmpty()) {
            // 컨텐츠 매칭 결과를 앞에 배치 (우선순위)
            Set<Long> result = new LinkedHashSet<>();

            // 1. 위치+컨텐츠 교집합을 먼저 추가 (최우선)
            for (Long id : locationMatchedIds) {
                if (contentMatchedIds.contains(id)) {
                    result.add(id);
                }
            }
            // 2. 나머지 위치 결과 추가
            result.addAll(locationMatchedIds);

            log.info("위치 기반 결과: 총 {}개 (교집합 우선: {}개)", result.size(),
                    result.stream().filter(contentMatchedIds::contains).count());
            return result;
        } else {
            return contentMatchedIds;
        }
    }

    /**
     * Gemini API를 호출하여 사용자 의도 분석
     */
    private AnalysisResult analyzeUserIntent(String userQuery) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key not configured, using fallback keyword matching");
            return fallbackKeywordMatching(userQuery);
        }

        try {
            String prompt = buildAnalysisPrompt(userQuery);
            String response = callGeminiApi(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.warn("Gemini API 호출 실패, 키워드 매칭으로 대체: {}", e.getMessage());
            return fallbackKeywordMatching(userQuery);
        }
    }

    private String buildAnalysisPrompt(String userQuery) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 숙소 추천 전문가입니다. 사용자의 여행 선호도를 분석하여 적합한 테마, 검색 키워드, 그리고 위치를 추출하세요.\n\n");
        prompt.append("테마 목록:\n");
        for (ThemeCategory category : ThemeCategory.values()) {
            prompt.append("- ").append(category.name()).append(" (").append(category.getKoreanName()).append(")\n");
        }
        prompt.append("\n제주도 주요 지역: 애월, 함덕, 성산, 중문, 서귀포, 협재, 한림, 표선, 우도, 이호, 월정리, 김녕, 세화, 한경, 대정, 안덕, 조천\n");
        prompt.append("\n사용자 입력: \"").append(userQuery).append("\"\n\n");
        prompt.append("반드시 아래 JSON 형식으로만 응답하세요:\n");
        prompt.append(
                "{\"themes\": [\"THEME1\"], \"keywords\": [\"키워드1\"], \"location\": \"지역명\", \"confidence\": 0.85, \"reasoning\": \"분석 이유\"}\n");
        prompt.append("themes: 1~2개, keywords: 1~3개, location: 지역명 (없으면 빈 문자열 \"\")\n");
        prompt.append("예시:\n");
        prompt.append(
                "- \"애월에서 놀고 싶어\" -> {\"themes\": [\"PLAY\"], \"keywords\": [\"놀이\"], \"location\": \"애월\", ...}\n");
        prompt.append(
                "- \"성산 근처 파티 숙소\" -> {\"themes\": [\"PARTY\"], \"keywords\": [\"파티\"], \"location\": \"성산\", ...}\n");
        prompt.append(
                "- \"조용히 쉬고 싶어\" -> {\"themes\": [\"VIBE\"], \"keywords\": [\"조용\", \"휴식\"], \"location\": \"\", ...}");
        return prompt.toString();
    }

    private String callGeminiApi(String prompt) throws JsonProcessingException {
        String url = String.format("%s/models/%s:generateContent?key=%s", geminiBaseUrl, geminiModel, geminiApiKey);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    private AnalysisResult parseGeminiResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode()) {
            throw new IllegalStateException("Gemini 응답에서 텍스트를 찾을 수 없습니다");
        }

        String text = textNode.asText().trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1);
            }
        }

        JsonNode analysisNode = objectMapper.readTree(text);

        List<String> themes = new ArrayList<>();
        analysisNode.path("themes").forEach(node -> themes.add(node.asText()));

        List<String> keywords = new ArrayList<>();
        analysisNode.path("keywords").forEach(node -> keywords.add(node.asText()));

        String location = analysisNode.path("location").asText("");
        double confidence = analysisNode.path("confidence").asDouble(0.5);
        String reasoning = analysisNode.path("reasoning").asText("AI 분석 결과");

        log.info("Gemini 분석: themes={}, keywords={}, location={}", themes, keywords, location);
        return new AnalysisResult(themes, keywords.stream().limit(3).collect(Collectors.toList()),
                location, confidence, reasoning);
    }

    private AnalysisResult fallbackKeywordMatching(String userQuery) {
        List<String> matchedThemes = new ArrayList<>();
        List<String> extractedKeywords = new ArrayList<>();
        String extractedLocation = "";
        String query = userQuery.toLowerCase();

        // 제주도 지역명 사전
        List<String> jejuLocations = List.of(
                "애월", "함덕", "성산", "중문", "서귀포", "협재", "한림", "표선", "우도",
                "이호", "월정리", "김녕", "세화", "한경", "대정", "안덕", "조천", "제주시");

        // 위치 추출
        for (String loc : jejuLocations) {
            if (query.contains(loc)) {
                extractedLocation = loc;
                break;
            }
        }

        Map<String, List<String>> keywordMap = Map.of(
                "NATURE", List.of("자연", "산", "숲", "풍경", "경치", "바다", "해변"),
                "VIBE", List.of("조용", "힐링", "분위기", "로맨틱", "감성"),
                "ACTIVITY", List.of("스포츠", "레저", "서핑", "등산"),
                "PARTY", List.of("파티", "모임", "축제", "술", "주점", "클럽", "포차"),
                "MEETING", List.of("커플", "친구", "가족", "헌팅", "만남", "이성", "짝", "새로운 사람"),
                "PERSONA", List.of("반려동물", "펫", "강아지"),
                "FACILITY", List.of("수영장", "바베큐", "온천"),
                "FOOD", List.of("맛집", "조식", "음식"),
                "CULTURE", List.of("문화", "역사", "전통"),
                "PLAY", List.of("게임", "오락", "놀이", "놀고"));

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (query.contains(keyword)) {
                    if (!matchedThemes.contains(entry.getKey()) && matchedThemes.size() < 2) {
                        matchedThemes.add(entry.getKey());
                    }
                    if (!extractedKeywords.contains(keyword) && extractedKeywords.size() < 3) {
                        extractedKeywords.add(keyword);
                    }
                }
            }
        }

        if (extractedKeywords.isEmpty()) {
            String[] words = userQuery.split("\\s+");
            for (String word : words) {
                // 제주 지역명과 일반적인 키워드는 제외
                if (word.length() >= 2 && extractedKeywords.size() < 3
                        && !jejuLocations.contains(word)
                        && !GENERIC_KEYWORDS.contains(word)) {
                    extractedKeywords.add(word);
                }
            }
        }

        // 일반적인 키워드 제거
        extractedKeywords.removeIf(GENERIC_KEYWORDS::contains);

        log.info("Fallback 분석: themes={}, keywords={}, location={}", matchedThemes, extractedKeywords,
                extractedLocation);
        return new AnalysisResult(matchedThemes, extractedKeywords, extractedLocation, 0.6, "키워드 매칭 결과");
    }

    private AiRecommendationResponse.AccommodationSummary toAccommodationSummary(Accommodation acc,
            List<String> themes, String thumbnailUrl) {
        return AiRecommendationResponse.AccommodationSummary.builder()
                .accommodationsId(acc.getAccommodationsId())
                .accommodationsName(acc.getAccommodationsName())
                .city(acc.getCity())
                .district(acc.getDistrict())
                .rating(acc.getRating())
                .reviewCount(acc.getReviewCount())
                .thumbnailUrl(thumbnailUrl)
                .minPrice(acc.getMinPrice())
                .themes(themes)
                .build();
    }

    private record AnalysisResult(List<String> themes, List<String> keywords, String location, Double confidence,
            String reasoning) {
    }
}
