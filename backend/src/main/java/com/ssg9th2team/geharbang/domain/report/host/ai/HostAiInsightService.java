package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.ai.client.GeminiApiClient;
import com.ssg9th2team.geharbang.domain.report.host.dto.*;
import com.ssg9th2team.geharbang.domain.report.host.service.HostReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostAiInsightService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String CACHE_NAME = "hostAiInsight";

    private final HostReportService hostReportService;
    private final RuleBasedSummaryClient ruleBasedSummaryClient;
    private final MockAiSummaryClient mockAiSummaryClient;
    private final GeminiApiClient geminiApiClient;
    private final HostAiInsightEligibilityChecker eligibilityChecker;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final AccommodationJpaRepository accommodationRepository;

    @Value("${ai.summary.provider:RULE}")
    private String provider;

    // ================================================================================
    // 1. 메인 진입점 (Main Entry Point)
    // ================================================================================
    public HostAiInsightResponse generate(Long hostId, HostAiInsightRequest request) {
        HostAiInsightTab tab = parseTab(request.getTab());
        HostAiInsightEligibilityResult eligibility = eligibilityChecker.evaluate(tab, request, hostId);

        // [정책] 데이터가 부족해도 AI 컨설팅을 위해 항상 통과 (Mock/Rule fallback 방지)
        boolean isColdStart = eligibility.getCurrent() < 5;

        // 캐싱 확인
        String cacheKey = buildCacheKey(hostId, request, tab);
        if (!request.isForceRefresh()) {
            HostAiInsightResponse cached = getCached(cacheKey, request);
            if (cached != null) return cached;
        }

        Provider selected = parseProvider(provider);
        HostAiInsightResponse response;

        if (selected == Provider.MOCK) {
            response = buildMock(tab, request, hostId);
            response.setEngine("MOCK");
            response.setFallbackUsed(false);
        } else if (selected == Provider.GEMINI) { // 명칭 통일: OPENAI -> GEMINI
            response = generateWithGemini(tab, request, hostId, eligibility, isColdStart);
        } else {
            response = buildRule(tab, request, hostId);
            response.setEngine("RULE");
            response.setFallbackUsed(false);
        }

        applyEligibilityMeta(response, eligibility);
        putCache(cacheKey, response, request);
        return response;
    }

    // ================================================================================
    // 2. Gemini 통합 로직 (Standardized Logic)
    // ================================================================================
    private HostAiInsightResponse generateWithGemini(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId, HostAiInsightEligibilityResult eligibility, boolean isColdStart) {
        try {
            Long accommodationId = request.getAccommodationId();
            String accommodationName;
            String location;

            if (accommodationId == null) {
                // 전체 숙소 선택 시
                List<Accommodation> accs = accommodationRepository.findByUserId(hostId);
                if (accs.isEmpty()) throw new IllegalArgumentException("숙소가 없습니다.");
                accommodationName = "호스트님의 모든 숙소";
                // 대표 위치는 첫 번째 숙소 기준으로 하되, 여러 지역일 수 있음을 감안
                location = accs.get(0).getCity() + " " + accs.get(0).getDistrict();
                if (accs.size() > 1) { // 위치 정보 포맷 개선
                    location += " 외 " + (accs.size() - 1) + "곳";
                }
            } else {
                // 개별 숙소 선택 시
                Accommodation accommodation = accommodationRepository.findById(accommodationId)
                        .orElseThrow(() -> new IllegalArgumentException("숙소를 찾을 수 없습니다."));
                accommodationName = accommodation.getAccommodationsName();
                location = accommodation.getCity() + " " + accommodation.getDistrict();
            }

            Map<String, Object> aiResult;

            if (tab == HostAiInsightTab.REVIEW) {
                HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(hostId, accommodationId, request.getFrom(), request.getTo());
                // 리뷰 텍스트만 추출 (최대 50개로 증가)
                List<String> reviews = summary.getRecentReviews().stream()
                        .map(r -> r.getContent().replace("\n", " ")) // 줄바꿈 제거
                        .limit(50)
                        .collect(Collectors.toList());
                aiResult = generateReviewAnalysis(reviews, accommodationName, isColdStart);
                return mapReviewResultToResponse(aiResult); // 리뷰 전용 매핑

            } else if (tab == HostAiInsightTab.THEME) {
                HostThemeReportResponse report = hostReportService.getThemePopularity(hostId, accommodationId, request.getFrom(), request.getTo(), request.getMetric());
                
                // 상위 테마들의 성과 데이터를 요약해서 전달
                String themeDataSummary = buildThemeSummary(report.getRows(), request.getMetric());
                
                aiResult = generateThemeAnalysis(location, themeDataSummary, isColdStart, request.getMetric());
                return mapThemeResultToResponse(aiResult); // 테마 전용 매핑

            } else { // DEMAND
                int count = (int) eligibility.getCurrent();
                aiResult = generateDemandPrediction(count, location, isColdStart);
                return mapDemandResultToResponse(aiResult); // 수요 전용 매핑
            }

        } catch (Exception ex) {
            log.warn("Gemini insight failed; falling back to RULE.", ex);
            HostAiInsightResponse fallback = buildRule(tab, request, hostId);
            fallback.setEngine("RULE (Fallback)");
            fallback.setFallbackUsed(true);
            return fallback;
        }
    }

    // [Helper] 테마 데이터 요약 문자열 생성
    private String buildThemeSummary(List<HostThemeReportRow> rows, String metric) {
        if (rows == null || rows.isEmpty()) return "데이터 없음";

        boolean isRevenue = "revenue".equalsIgnoreCase(metric);
        // 상위 5개만 추출
        return rows.stream()
                .limit(5)
                .map(row -> {
                    String value = isRevenue 
                        ? (row.getRevenueSum() != null ? row.getRevenueSum() + "원" : "0원")
                        : (row.getReservationCount() != null ? row.getReservationCount() + "건" : "0건");
                    return String.format("%s(%s)", row.getThemeName(), value);
                })
                .collect(Collectors.joining(", "));
    }

    // ================================================================================
    // 3. AI 프롬프트 생성 메소드 (UI 최적화: 짧고 강렬한 데이터)
    // ================================================================================

    // [리뷰 리포트] - 접근 제어자 private으로 변경
    private Map<String, Object> generateReviewAnalysis(List<String> reviews, String accommodationName, boolean isColdStart) {
        String prompt;
        if (isColdStart || reviews.isEmpty()) {
            prompt = String.format("""
                신규 숙소 '%s'를 위한 **초기 운영 필승 법칙 3가지**를 브리핑해.
                데이터가 없으므로, 이 지역/숙소 유형에서 반드시 지켜야 할 핵심만 짚어줘.
                """, accommodationName);
        } else {
            prompt = String.format("""
                당신은 데이터를 10초 만에 브리핑하는 유능한 비서야.
                아래 리뷰 데이터를 분석해 **즉시 실행 가능한 결론**만 보고해.
                숙소 이름: '%s'
                
                [리뷰 데이터]
                %s
                
                [분석 요청 사항]
                1. **summary**: 전체 분위기를 1문장으로 요약 (명사형 종결).
                2. **pros**: 핵심 강점 2가지 (각 50자 이내).
                3. **cons**: 시급한 개선점 2가지 (각 50자 이내).
                4. **actions**: 당장 실행할 액션 2가지 (각 50자 이내).
                5. **monitoring**: 점검할 지표 2가지 (단답형).
                """, accommodationName, String.join(" | ", reviews));
        }
        return callGemini(prompt);
    }

    // [테마 리포트] - 접근 제어자 private으로 변경
    private Map<String, Object> generateThemeAnalysis(String location, String themeDataSummary, boolean isColdStart, String metric) {
        String prompt;
        String metricName = "revenue".equalsIgnoreCase(metric) ? "매출" : "예약수";
        
        if (isColdStart) {
            prompt = String.format("""
                데이터 부족 상황임. '%s' 지역 숙소의 **테마 운영 불변의 법칙 3가지**를 브리핑해.
                현재 테마 데이터: %s
                """, location, themeDataSummary);
        } else {
            String strategyFocus = "revenue".equalsIgnoreCase(metric) 
                ? "객단가 상승 전략" 
                : "노출/유입 확대 전략";

            prompt = String.format("""
                '%s' 지역 숙소 테마 성과 브리핑.
                [데이터]: %s
                [기준]: %s 중심
                
                위 데이터를 바탕으로 **%s** 관점의 핵심만 보고해.
                
                [분석 요청 사항]
                1. **summary**: 테마 트렌드 1문장 요약 (명사형 종결).
                2. **pros**: 성과 좋은 점 2가지 (각 50자 이내).
                3. **cons**: 보완할 점 2가지 (각 50자 이내).
                4. **actions**: 경쟁력 강화 액션 2가지 (각 50자 이내).
                5. **monitoring**: 관찰할 지표 2가지 (단답형).
                """, location, themeDataSummary, metricName, strategyFocus);
        }
        return callGemini(prompt);
    }

    // [수요 예측 리포트] - 접근 제어자 private으로 변경
    private Map<String, Object> generateDemandPrediction(int count, String location, boolean isColdStart) {
        String prompt;
        if (isColdStart) {
            prompt = String.format("""
                데이터 부족 상황임. '%s' 지역의 **계절별/시기별 수요 불변의 법칙 3가지**를 브리핑해.
                """, location);
        } else {
            prompt = String.format("""
                '%s' 지역 숙소 수요 예측 브리핑. (최근 예약 %d건)
                향후 수요 추세를 예측하고 **수익 관리(Revenue Management)** 결론만 보고해.
                
                [분석 요청 사항]
                1. **summary**: 수요 흐름 1문장 요약 (명사형 종결).
                2. **pros**: 기회 요인 2가지 (각 50자 이내).
                3. **cons**: 리스크 요인 2가지 (각 50자 이내).
                4. **actions**: 가격/프로모션 전략 2가지 (각 50자 이내).
                5. **monitoring**: 점검할 경쟁사 지표 2가지 (단답형).
                """, location, count);
        }
        return callGemini(prompt);
    }

    // [공통 호출 메소드]
    private Map<String, Object> callGemini(String specificPrompt) {
        String finalPrompt = specificPrompt + """
                
                [★★출력 포맷 지침(엄수)★★]
                1. **JSON 포맷**: 반드시 아래 5가지 키를 가진 JSON으로만 출력 (마크다운 금지).
                2. **분량 제한**:
                   - 모든 항목은 **개조식**으로 작성.
                   - 리스트(`pros`, `cons` 등)는 **최대 2~3개** 항목만 포함.
                   - 각 문장은 **최대 50자 이내**로 짧게 끊을 것.
                3. **어체**: 설명조 금지. '~함', '~임' 등 명사형 종결 또는 간결한 해요체 사용.
                4. **내용**: 서론/본론 없이 **결론만** 타격.
                
                {
                  "summary": "핵심 요약 (반드시 2문장 이상으로 구체적이고 풍부하게 작성할 것)",
                  "pros": ["강점1 (50자 이내)", "강점2"],
                  "cons": ["단점1 (50자 이내)", "단점2"],
                  "actions": ["액션1 (50자 이내)", "액션2"],
                  "monitoring": ["지표1 (단답형)", "지표2"]
                }
                """;

        try {
            String jsonResponse = geminiApiClient.generateContent(finalPrompt);
            // 중복 로직 제거: 마크다운 제거는 GeminiApiClient에서 처리됨
            return parseSafe(jsonResponse.trim());
        } catch (Exception e) {
            log.error("AI Generation Failed", e);
            return Map.of(
                    "summary", "현재 데이터를 분석하는 중 오류가 발생했습니다.",
                    "pros", List.of("데이터 확인 필요"),
                    "cons", List.of("데이터 확인 필요"),
                    "actions", List.of("잠시 후 다시 시도해주세요."),
                    "monitoring", List.of("시스템 상태")
            );
        }
    }

    // ================================================================================
    // 4. 안전한 파싱 & 매핑 (Safety Logic)
    // ================================================================================

    private Map<String, Object> parseSafe(String jsonString) {
        try {
            Map<String, Object> map = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});

            // [방어 로직] 키값 불일치 해결
            if (map.containsKey("overview")) map.put("summary", map.get("overview"));
            if (map.containsKey("strength")) map.put("pros", map.get("strength"));
            if (map.containsKey("weakness")) map.put("cons", map.get("weakness"));
            if (map.containsKey("improvements")) map.put("cons", map.get("improvements")); // improvements 키도 cons로 매핑
            if (map.containsKey("actionPlan")) map.put("actions", map.get("actionPlan"));
            if (map.containsKey("keywords")) map.put("monitoring", map.get("keywords"));

            // 모든 필드를 강제로 List로 변환
            map.put("pros", convertToList(map.get("pros")));
            map.put("cons", convertToList(map.get("cons")));
            map.put("actions", convertToList(map.get("actions")));
            map.put("monitoring", convertToList(map.get("monitoring")));

            map.putIfAbsent("summary", "데이터 분석을 완료했습니다.");
            return map;
        } catch (Exception e) {
            log.error("Parsing Error raw={}", jsonString, e);
            return Map.of(
                    "summary", "AI 응답을 분석하는 중 오류가 발생했습니다.",
                    "pros", List.of("분석 실패"),
                    "actions", List.of("잠시 후 다시 시도해주세요.")
            );
        }
    }

    private List<String> convertToList(Object obj) {
        if (obj == null) return new ArrayList<>();

        if (obj instanceof List<?>) {
            return ((List<?>) obj).stream()
                    .map(item -> {
                        String s = Objects.toString(item, "");
                        // 마크다운 제거 (간단히 ** 제거)
                        return s.replace("**", "");
                    })
                    .collect(Collectors.toList());
        }

        if (obj instanceof String) {
            String str = (String) obj;
            if (str.startsWith("[") && str.endsWith("]")) {
                try {
                    List<String> list = objectMapper.readValue(str, new TypeReference<List<String>>() {});
                    return list.stream().map(s -> s.replace("**", "")).collect(Collectors.toList());
                } catch (Exception e) {
                    // fallback to simple split
                    return Arrays.stream(str.substring(1, str.length() - 1).split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s.replace("**", ""))
                            .collect(Collectors.toList());
                }
            }
            return Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.replace("**", ""))
                    .collect(Collectors.toList());
        }

        return List.of(obj.toString().replace("**", ""));
    }

    // -------------------------------------------------------------------------
    // [탭별 매핑 로직] - 분리 및 제목 수정
    // -------------------------------------------------------------------------

    private HostAiInsightResponse mapReviewResultToResponse(Map<String, Object> result) {
        List<HostAiInsightSection> sections = new ArrayList<>();
        sections.add(createSection("총평", result.get("summary")));
        sections.add(createSection("좋았던 점", result.get("pros")));
        sections.add(createSection("개선 포인트", result.get("cons")));
        sections.add(createSection("다음 액션", result.get("actions")));
        sections.add(createSection("주의·리스크", result.get("monitoring"))); // 리뷰 탭은 "주의·리스크" 유지
        return buildResponse(sections);
    }

    private HostAiInsightResponse mapThemeResultToResponse(Map<String, Object> result) {
        List<HostAiInsightSection> sections = new ArrayList<>();
        sections.add(createSection("트렌드 요약", result.get("summary")));
        sections.add(createSection("강점", result.get("pros")));
        sections.add(createSection("보완할 점", result.get("cons")));
        sections.add(createSection("다음 액션", result.get("actions")));
        sections.add(createSection("모니터링", result.get("monitoring")));
        return buildResponse(sections);
    }

    private HostAiInsightResponse mapDemandResultToResponse(Map<String, Object> result) {
        List<HostAiInsightSection> sections = new ArrayList<>();
        sections.add(createSection("수요 예측 요약", result.get("summary")));
        sections.add(createSection("기회 요인", result.get("pros"))); // pros -> 기회 요인
        sections.add(createSection("리스크 요인", result.get("cons"))); // cons -> 리스크 요인
        sections.add(createSection("다음 액션", result.get("actions")));
        sections.add(createSection("모니터링", result.get("monitoring")));
        return buildResponse(sections);
    }

    private HostAiInsightResponse buildResponse(List<HostAiInsightSection> sections) {
        HostAiInsightResponse response = new HostAiInsightResponse();
        response.setGeneratedAt(ZonedDateTime.now(KST).toString());
        response.setSections(sections);
        response.setEngine("GEMINI");
        response.setFallbackUsed(false);
        return response;
    }

    private HostAiInsightSection createSection(String title, Object content) {
        HostAiInsightSection section = new HostAiInsightSection();
        section.setTitle(title);
        if (content instanceof String) {
            section.setItems(List.of(((String) content).replace("**", "")));
        } else {
            section.setItems(convertToList(content));
        }
        return section;
    }

    // --- Legacy / Utils ---

    private HostAiInsightResponse buildRule(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        RuleBasedInsightComposer composer = new RuleBasedInsightComposer(ruleBasedSummaryClient);
        List<HostAiInsightSection> sections;
        if (tab == HostAiInsightTab.REVIEW) {
            HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(hostId, request.getAccommodationId(), request.getFrom(), request.getTo());
            sections = composer.buildReviewSections(summary);
        } else if (tab == HostAiInsightTab.THEME) {
            HostThemeReportResponse report = hostReportService.getThemePopularity(hostId, request.getAccommodationId(), request.getFrom(), request.getTo(), request.getMetric());
            sections = composer.buildThemeSections(report);
        } else {
            HostForecastResponse forecast = hostReportService.getDemandForecast(hostId, request.getAccommodationId(), safeTarget(request.getTarget()), safeInt(request.getHorizonDays(), 30), safeInt(request.getHistoryDays(), 180));
            sections = composer.buildDemandSections(forecast);
        }
        return buildResponse(sections);
    }

    private HostAiInsightResponse buildMock(HostAiInsightTab tab, HostAiInsightRequest request, Long hostId) {
        RuleBasedInsightComposer composer = new RuleBasedInsightComposer(ruleBasedSummaryClient);
        if (tab == HostAiInsightTab.REVIEW) {
            HostReviewReportSummaryResponse summary = hostReportService.getReviewSummary(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo()
            );
            HostReviewAiSummaryRequest aiRequest = new HostReviewAiSummaryRequest();
            aiRequest.setAccommodationId(request.getAccommodationId());
            aiRequest.setFrom(summary.getFrom());
            aiRequest.setTo(summary.getTo());
            HostReviewAiSummaryResponse mock = mockAiSummaryClient.generate(summary, aiRequest);
            return buildResponse(mapReviewSections(mock));
        }
        if (tab == HostAiInsightTab.THEME) {
            HostThemeReportResponse report = hostReportService.getThemePopularity(
                    hostId,
                    request.getAccommodationId(),
                    request.getFrom(),
                    request.getTo(),
                    request.getMetric()
            );
            return buildResponse(composer.buildThemeSections(report));
        }
        HostForecastResponse forecast = hostReportService.getDemandForecast(
                hostId,
                request.getAccommodationId(),
                safeTarget(request.getTarget()),
                safeInt(request.getHorizonDays(), 30),
                safeInt(request.getHistoryDays(), 180)
        );
        return buildResponse(composer.buildDemandSections(forecast));
    }

    private List<HostAiInsightSection> mapReviewSections(HostReviewAiSummaryResponse response) {
        List<HostAiInsightSection> sections = new ArrayList<>();
        sections.add(section("총평", response.getOverview()));
        sections.add(section("좋았던 점", response.getPositives()));
        sections.add(section("개선 포인트", response.getNegatives()));
        sections.add(section("다음 액션", response.getActions()));
        sections.add(section("모니터링", response.getRisks())); // "주의·리스크" -> "모니터링" 변경
        return sections;
    }

    private HostAiInsightSection section(String title, List<String> items) {
        HostAiInsightSection section = new HostAiInsightSection();
        section.setTitle(title);
        section.setItems(items);
        return section;
    }

    private void applyEligibilityMeta(HostAiInsightResponse response, HostAiInsightEligibilityResult eligibility) {
        HostAiInsightMeta meta = new HostAiInsightMeta();
        meta.setStatus(eligibility.getStatus());
        meta.setCanGenerate(eligibility.isCanGenerate());
        meta.setDisabledReason(eligibility.getDisabledReason());
        meta.setWarningMessage(eligibility.getWarningMessage());
        meta.setCurrent(eligibility.getCurrent());
        meta.setMinRequired(eligibility.getMinRequired());
        meta.setRecommended(eligibility.getRecommended());
        meta.setUnitLabel(eligibility.getUnitLabel());
        response.setMeta(meta);
    }

    private HostAiInsightResponse getCached(String cacheKey, HostAiInsightRequest request) {
        if (!isCacheEligible(request)) return null;
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return null;
        HostAiInsightResponse cached = cache.get(cacheKey, HostAiInsightResponse.class);
        return cached == null ? null : copyResponse(cached);
    }

    private void putCache(String cacheKey, HostAiInsightResponse response, HostAiInsightRequest request) {
        if (!isCacheEligible(request)) return;
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return;
        cache.put(cacheKey, copyResponse(response));
    }

    private HostAiInsightResponse copyResponse(HostAiInsightResponse response) {
        HostAiInsightResponse copy = new HostAiInsightResponse();
        copy.setEngine(response.getEngine());
        copy.setFallbackUsed(response.isFallbackUsed());
        copy.setGeneratedAt(response.getGeneratedAt());
        if (response.getMeta() != null) {
            HostAiInsightMeta meta = new HostAiInsightMeta();
            meta.setStatus(response.getMeta().getStatus());
            meta.setCanGenerate(response.getMeta().isCanGenerate());
            meta.setDisabledReason(response.getMeta().getDisabledReason());
            meta.setWarningMessage(response.getMeta().getWarningMessage());
            meta.setCurrent(response.getMeta().getCurrent());
            meta.setMinRequired(response.getMeta().getMinRequired());
            meta.setRecommended(response.getMeta().getRecommended());
            meta.setUnitLabel(response.getMeta().getUnitLabel());
            copy.setMeta(meta);
        }
        if (response.getSections() != null) {
            List<HostAiInsightSection> sections = new ArrayList<>();
            for (HostAiInsightSection section : response.getSections()) {
                HostAiInsightSection copySection = new HostAiInsightSection();
                copySection.setTitle(section.getTitle());
                copySection.setItems(section.getItems() != null ? new ArrayList<>(section.getItems()) : List.of());
                sections.add(copySection);
            }
            copy.setSections(sections);
        }
        return copy;
    }

    private boolean isCacheEligible(HostAiInsightRequest request) {
        if (request == null) return false;
        LocalDate from = request.getFrom();
        LocalDate to = request.getTo();
        if (from == null || to == null) return true;
        if (to.isBefore(from)) return false;
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days == 7 || days == 30) return true;
        if (from.getDayOfMonth() == 1) {
            LocalDate lastDay = from.with(TemporalAdjusters.lastDayOfMonth());
            return lastDay.equals(to);
        }
        return false;
    }

    private String buildCacheKey(Long hostId, HostAiInsightRequest request, HostAiInsightTab tab) {
        return String.join("|",
                String.valueOf(hostId),
                String.valueOf(request.getAccommodationId()),
                String.valueOf(request.getFrom()),
                String.valueOf(request.getTo()),
                tab.name(),
                safeString(request.getMetric()),
                safeString(request.getTarget()),
                String.valueOf(request.getHorizonDays()),
                String.valueOf(request.getHistoryDays())
        );
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int safeInt(Integer value, int fallback) {
        if (value == null || value <= 0) return fallback;
        return value;
    }

    private String safeTarget(String target) {
        return Objects.equals(target, "revenue") ? "revenue" : "reservations";
    }

    private HostAiInsightTab parseTab(String raw) {
        if (raw == null || raw.isBlank()) return HostAiInsightTab.REVIEW;
        try {
            return HostAiInsightTab.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return HostAiInsightTab.REVIEW;
        }
    }

    private Provider parseProvider(String raw) {
        if (raw == null || raw.isBlank()) return Provider.RULE;
        try {
            return Provider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Provider.RULE;
        }
    }

    private enum Provider {
        RULE,
        OPENAI, // Legacy support
        GEMINI, // New standard
        MOCK
    }
}
