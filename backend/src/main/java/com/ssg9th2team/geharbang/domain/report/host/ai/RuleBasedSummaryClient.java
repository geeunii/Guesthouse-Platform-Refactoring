package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RuleBasedSummaryClient implements AiSummaryClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int OVERVIEW_COUNT = 2;
    private static final int POSITIVE_COUNT = 3;
    private static final int NEGATIVE_COUNT = 3;
    private static final int ACTION_COUNT = 5;
    private static final int RISK_MAX = 3;
    private static final int ACTION_MAX_LEN = 70;
    private static final String SEPARATOR = "||";

    private static final List<String> COMMUNICATION_TAGS = List.of("친절", "응대", "소통", "안내", "서비스");
    private static final List<String> CLEAN_TAGS = List.of("깨끗", "청결", "청소", "정돈", "침구", "욕실");
    private static final List<String> NOISE_TAGS = List.of("조용", "소음", "시끄");
    private static final List<String> FACILITY_TAGS = List.of("시설", "난방", "온수", "와이파이", "샤워", "침대");
    private static final List<String> LOCATION_TAGS = List.of("위치", "교통", "접근", "역", "주차");

    private static final Map<String, String> NEGATIVE_KEYWORDS = new LinkedHashMap<>();

    static {
        NEGATIVE_KEYWORDS.put("불친절", "응대");
        NEGATIVE_KEYWORDS.put("기다림", "응대");
        NEGATIVE_KEYWORDS.put("늦", "응대");
        NEGATIVE_KEYWORDS.put("불편", "이용 편의");
        NEGATIVE_KEYWORDS.put("더럽", "청결");
        NEGATIVE_KEYWORDS.put("곰팡", "청결");
        NEGATIVE_KEYWORDS.put("벌레", "청결");
        NEGATIVE_KEYWORDS.put("냄새", "냄새");
        NEGATIVE_KEYWORDS.put("시끄", "소음");
        NEGATIVE_KEYWORDS.put("소음", "소음");
        NEGATIVE_KEYWORDS.put("추움", "온도");
        NEGATIVE_KEYWORDS.put("냉", "온도");
        NEGATIVE_KEYWORDS.put("좁", "공간");
        NEGATIVE_KEYWORDS.put("낡", "시설");
        NEGATIVE_KEYWORDS.put("고장", "시설");
        NEGATIVE_KEYWORDS.put("문제", "시설");
        NEGATIVE_KEYWORDS.put("환불", "환불");
    }

    @Override
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        ReviewSignals signals = buildSignals(summary);

        List<String> overview = buildOverview(signals);
        List<String> positives = buildPositives(signals);
        List<String> negatives = buildNegatives(signals);
        List<String> actions = buildActions(signals);
        List<String> risks = buildRisks(signals);

        HostReviewAiSummaryResponse response = new HostReviewAiSummaryResponse();
        response.setAccommodationId(request.getAccommodationId());
        response.setFrom(summary.getFrom());
        response.setTo(summary.getTo());
        response.setGeneratedAt(ZonedDateTime.now(KST).toString());
        response.setOverview(ensureExactSize(overview, OVERVIEW_COUNT));
        response.setPositives(ensureExactSize(positives, POSITIVE_COUNT));
        response.setNegatives(ensureExactSize(negatives, NEGATIVE_COUNT));
        response.setActions(ensureExactSize(actions, ACTION_COUNT));
        response.setRisks(trimMax(risks, RISK_MAX));
        return response;
    }

    private ReviewSignals buildSignals(HostReviewReportSummaryResponse summary) {
        int reviewCount = summary.getReviewCount() != null ? summary.getReviewCount() : 0;
        double avgRating = summary.getAvgRating() != null ? summary.getAvgRating() : 0.0;
        List<TagStat> tags = toTagStats(summary.getTopTags());
        Map<String, String> negativeTopics = extractNegativeTopics(summary.getRecentReviews());
        double lowRatingRatio = calculateLowRatingRatio(reviewCount, summary.getRatingDistribution());
        return new ReviewSignals(reviewCount, avgRating, tags, negativeTopics, lowRatingRatio);
    }

    private List<TagStat> toTagStats(List<HostReviewReportTagRow> rows) {
        if (rows == null) return List.of();
        return rows.stream()
                .filter(tag -> tag.getTagName() != null && !tag.getTagName().isBlank())
                .map(tag -> new TagStat(tag.getTagName(), tag.getCount() != null ? tag.getCount() : 0))
                .sorted((a, b) -> Integer.compare(b.count(), a.count()))
                .collect(Collectors.toList());
    }

    private Map<String, String> extractNegativeTopics(List<HostReviewReportRecentRow> reviews) {
        Map<String, String> topics = new LinkedHashMap<>();
        if (reviews == null) return topics;
        for (HostReviewReportRecentRow review : reviews) {
            String content = review.getContent();
            if (content == null || content.isBlank()) continue;
            String normalized = content.toLowerCase(Locale.KOREA);
            for (Map.Entry<String, String> entry : NEGATIVE_KEYWORDS.entrySet()) {
                if (normalized.contains(entry.getKey()) && !topics.containsKey(entry.getValue())) {
                    topics.put(entry.getValue(), entry.getKey());
                }
            }
        }
        return topics;
    }

    private double calculateLowRatingRatio(int reviewCount, Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return reviewCount > 0 ? 0.0 : 0.0;
        }
        int total = ratingDistribution.values().stream().mapToInt(value -> value != null ? value : 0).sum();
        if (total == 0) {
            total = reviewCount;
        }
        int low = 0;
        low += ratingDistribution.getOrDefault(1, 0);
        low += ratingDistribution.getOrDefault(2, 0);
        low += ratingDistribution.getOrDefault(3, 0);
        return total > 0 ? (double) low / total : 0.0;
    }

    private List<String> buildOverview(ReviewSignals signals) {
        List<String> overview = new ArrayList<>();
        if (signals.reviewCount() == 0) {
            overview.add(formatLine("리뷰 데이터가 부족해 요약 근거가 제한적입니다.", "리뷰 0건"));
            overview.add(formatLine("기간을 넓혀 추세를 확인하는 것을 권장합니다.", "데이터 부족"));
            return overview;
        }
        overview.add(formatLine(
                String.format(Locale.KOREA, "리뷰 %d건, 평균 평점 %.1f점입니다.", signals.reviewCount(), signals.avgRating()),
                String.format(Locale.KOREA, "리뷰 %d건, 평균 평점 %.1f점", signals.reviewCount(), signals.avgRating())
        ));
        TagStat topTag = firstTag(signals.tags());
        if (topTag != null) {
            overview.add(formatLine(
                    String.format(Locale.KOREA, "상위 태그는 '%s' 중심으로 형성됩니다.", topTag.name()),
                    String.format(Locale.KOREA, "태그 '%s' %d건", topTag.name(), topTag.count())
            ));
        } else {
            overview.add(formatLine("상위 태그 데이터가 부족합니다.", "태그 데이터 부족"));
        }
        return overview;
    }

    private List<String> buildPositives(ReviewSignals signals) {
        List<String> positives = new ArrayList<>();
        for (TagStat tag : signals.tags()) {
            positives.add(formatLine(
                    String.format(Locale.KOREA, "태그 '%s' 강점을 유지하세요.", tag.name()),
                    String.format(Locale.KOREA, "태그 '%s' %d건", tag.name(), tag.count())
            ));
            if (positives.size() >= POSITIVE_COUNT) break;
        }
        if (positives.size() < POSITIVE_COUNT && signals.reviewCount() > 0 && signals.avgRating() > 0) {
            positives.add(formatLine(
                    String.format(Locale.KOREA, "평균 평점 %.1f점 흐름을 유지 중입니다.", signals.avgRating()),
                    String.format(Locale.KOREA, "평균 평점 %.1f점", signals.avgRating())
            ));
        }
        while (positives.size() < POSITIVE_COUNT) {
            positives.add(formatLine("강점 판단을 위한 데이터가 부족합니다.", "데이터 부족"));
        }
        return positives;
    }

    private List<String> buildNegatives(ReviewSignals signals) {
        List<String> negatives = new ArrayList<>();
        for (Map.Entry<String, String> entry : signals.negativeTopics().entrySet()) {
            negatives.add(formatLine(
                    String.format(Locale.KOREA, "%s 관련 불만 신호가 보입니다.", entry.getKey()),
                    String.format(Locale.KOREA, "최근 리뷰 키워드 '%s'", entry.getValue())
            ));
            if (negatives.size() >= NEGATIVE_COUNT) break;
        }
        if (signals.lowRatingRatio() >= 0.2 && negatives.size() < NEGATIVE_COUNT) {
            negatives.add(formatLine(
                    "저평점 비중이 높아 개선 필요성이 있습니다.",
                    String.format(Locale.KOREA, "저평점(<=3) 비율 %.1f%%", signals.lowRatingRatio() * 100)
            ));
        }
        while (negatives.size() < NEGATIVE_COUNT) {
            negatives.add(formatLine("관찰 포인트를 주간 단위로 점검하세요.", "부정 키워드 미감지"));
        }
        return negatives;
    }

    private List<String> buildActions(ReviewSignals signals) {
        List<String> actions = new ArrayList<>();
        actions.add(formatAction(
                "체크인/안내 메시지 템플릿을 정리하고 응대 톤을 통일하기.",
                firstEvidence(
                        evidenceFromTag(signals.tags(), COMMUNICATION_TAGS),
                        evidenceFromNegative(signals.negativeTopics(), Set.of("응대", "이용 편의")),
                        evidenceFromReviewCount(signals.reviewCount())
                )
        ));
        actions.add(formatAction(
                "객실·욕실 청결 체크리스트를 운영하고 비품 상태를 점검하기.",
                firstEvidence(
                        evidenceFromTag(signals.tags(), CLEAN_TAGS),
                        evidenceFromNegative(signals.negativeTopics(), Set.of("청결", "냄새")),
                        evidenceFromReviewCount(signals.reviewCount())
                )
        ));
        actions.add(formatAction(
                "야간 소음/이용 규칙을 체크인 시 재안내하고 공용공간 공지를 강화하기.",
                firstEvidence(
                        evidenceFromTag(signals.tags(), NOISE_TAGS),
                        evidenceFromNegative(signals.negativeTopics(), Set.of("소음")),
                        evidenceFromReviewCount(signals.reviewCount())
                )
        ));
        actions.add(formatAction(
                "상위 태그를 소개 문구와 사진 캡션에 반영하기.",
                firstEvidence(
                        evidenceFromTopTag(signals.tags()),
                        evidenceFromTag(signals.tags(), LOCATION_TAGS),
                        "상위 태그 데이터 부족"
                )
        ));
        actions.add(formatAction(
                "체크아웃 직후 리뷰 요청과 불만 키워드 주간 모니터링을 운영하기.",
                firstEvidence(
                        evidenceFromReviewCount(signals.reviewCount()),
                        evidenceFromLowRating(signals.lowRatingRatio()),
                        "리뷰 데이터 부족"
                )
        ));
        return actions;
    }

    private List<String> buildRisks(ReviewSignals signals) {
        List<String> risks = new ArrayList<>();
        if (signals.lowRatingRatio() >= 0.3) {
            risks.add(formatLine(
                    "저평점 비중이 높아 평점 관리 리스크가 있습니다.",
                    String.format(Locale.KOREA, "저평점(<=3) 비율 %.1f%%", signals.lowRatingRatio() * 100)
            ));
        }
        if (signals.negativeTopics().containsKey("환불")) {
            risks.add(formatLine("환불/분쟁 리스크가 발생할 수 있습니다.", "최근 리뷰 키워드 '환불'"));
        }
        if (signals.negativeTopics().containsKey("응대") && risks.size() < RISK_MAX) {
            risks.add(formatLine(
                    "응대 품질 저하 리스크가 있습니다.",
                    "최근 리뷰 키워드 '" + signals.negativeTopics().get("응대") + "'"
            ));
        }
        if (risks.isEmpty()) {
            risks.add(formatLine("현재 긴급 리스크 없음.", "부정 키워드 미감지"));
            risks.add(formatLine("저평점 리뷰 키워드를 주간 모니터링하세요.", "리스크 없음"));
            risks.add(formatLine("청결/소음/응대 관련 언급 증가를 확인하세요.", "리스크 없음"));
        }
        return trimMax(risks, RISK_MAX);
    }

    private List<String> ensureExactSize(List<String> items, int size) {
        List<String> result = new ArrayList<>(items);
        while (result.size() < size) {
            result.add(formatLine("데이터 부족으로 판단이 제한됩니다.", "데이터 부족"));
        }
        if (result.size() > size) {
            return result.subList(0, size);
        }
        return result;
    }

    private List<String> trimMax(List<String> items, int max) {
        if (items.size() <= max) return items;
        return items.subList(0, max);
    }

    private String formatAction(String main, String evidence) {
        String trimmed = trimLength(main, ACTION_MAX_LEN);
        return formatLine(trimmed, evidence);
    }

    private String formatLine(String main, String evidence) {
        String safeMain = (main == null || main.isBlank()) ? "데이터 부족" : main.trim();
        String safeEvidence = (evidence == null || evidence.isBlank()) ? "데이터 부족" : evidence.trim();
        return safeMain + SEPARATOR + safeEvidence;
    }

    private String trimLength(String text, int maxLen) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) return trimmed;
        return trimmed.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private TagStat firstTag(List<TagStat> tags) {
        return tags.isEmpty() ? null : tags.get(0);
    }

    private String evidenceFromTopTag(List<TagStat> tags) {
        TagStat tag = firstTag(tags);
        if (tag == null) return null;
        return String.format(Locale.KOREA, "태그 '%s' %d건", tag.name(), tag.count());
    }

    private String evidenceFromTag(List<TagStat> tags, List<String> keywords) {
        for (TagStat tag : tags) {
            for (String keyword : keywords) {
                if (containsIgnoreCase(tag.name(), keyword)) {
                    return String.format(Locale.KOREA, "태그 '%s' %d건", tag.name(), tag.count());
                }
            }
        }
        return null;
    }

    private String evidenceFromNegative(Map<String, String> topics, Set<String> targetTopics) {
        for (String topic : targetTopics) {
            if (topics.containsKey(topic)) {
                return String.format(Locale.KOREA, "최근 리뷰 키워드 '%s'", topics.get(topic));
            }
        }
        return null;
    }

    private String evidenceFromReviewCount(int reviewCount) {
        if (reviewCount <= 0) return null;
        return String.format(Locale.KOREA, "리뷰 %d건", reviewCount);
    }

    private String evidenceFromLowRating(double ratio) {
        if (ratio <= 0) return null;
        return String.format(Locale.KOREA, "저평점(<=3) 비율 %.1f%%", ratio * 100);
    }

    private String firstEvidence(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "데이터 부족";
    }

    private boolean containsIgnoreCase(String source, String token) {
        return source.toLowerCase(Locale.KOREA).contains(token.toLowerCase(Locale.KOREA));
    }

    private record TagStat(String name, int count) {
    }

    private record ReviewSignals(
            int reviewCount,
            double avgRating,
            List<TagStat> tags,
            Map<String, String> negativeTopics,
            double lowRatingRatio
    ) {
    }
}
