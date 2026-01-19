package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedSummaryClientTest {

    private final RuleBasedSummaryClient client = new RuleBasedSummaryClient();

    @Test
    void generatesStableSummaryForPositiveSignals() {
        HostReviewReportSummaryResponse summary = new HostReviewReportSummaryResponse();
        summary.setReviewCount(20);
        summary.setAvgRating(4.6);
        summary.setTopTags(List.of(
                tag("친절해요", 10),
                tag("깨끗해요", 8),
                tag("위치 좋아요", 6)
        ));
        summary.setRatingDistribution(ratingMap(10, 6, 2, 1, 1));

        HostReviewAiSummaryResponse response = client.generate(summary, request());

        assertSizeEquals(response.getOverview(), 2);
        assertSizeEquals(response.getPositives(), 3);
        assertSizeEquals(response.getNegatives(), 3);
        assertSizeEquals(response.getActions(), 5);
        assertSizeBetween(response.getRisks(), 1, 3);
        assertContainsEvidence(response);
        assertTrue(response.getNegatives().get(0).contains("||"));
        assertActionLength(response);
    }

    @Test
    void generatesNegativesAndRisksWhenBadSignalsExist() {
        HostReviewReportSummaryResponse summary = new HostReviewReportSummaryResponse();
        summary.setReviewCount(12);
        summary.setAvgRating(3.1);
        summary.setTopTags(List.of(
                tag("조용해요", 4),
                tag("시설 좋아요", 3)
        ));
        summary.setRatingDistribution(ratingMap(2, 2, 2, 3, 3));
        summary.setRecentReviews(List.of(review("방이 더럽고 시끄러웠어요."), review("응대가 늦어요.")));

        HostReviewAiSummaryResponse response = client.generate(summary, request());

        assertSizeEquals(response.getNegatives(), 3);
        assertSizeBetween(response.getRisks(), 1, 3);
        assertFalse(response.getRisks().get(0).contains("유의미한 리스크 없음"));
        assertSizeEquals(response.getActions(), 5);
        assertContainsEvidence(response);
        assertActionLength(response);
    }

    @Test
    void generatesGuidanceWhenDataIsMissing() {
        HostReviewReportSummaryResponse summary = new HostReviewReportSummaryResponse();
        summary.setReviewCount(0);
        summary.setAvgRating(0.0);
        summary.setTopTags(List.of());
        summary.setRatingDistribution(new HashMap<>());
        summary.setRecentReviews(List.of());

        HostReviewAiSummaryResponse response = client.generate(summary, request());

        assertSizeEquals(response.getOverview(), 2);
        assertSizeEquals(response.getPositives(), 3);
        assertSizeEquals(response.getNegatives(), 3);
        assertSizeEquals(response.getActions(), 5);
        assertSizeBetween(response.getRisks(), 1, 3);
        assertContainsEvidence(response);
        assertTrue(response.getRisks().get(0).contains("현재 긴급 리스크 없음"));
        assertActionLength(response);
    }

    private HostReviewReportTagRow tag(String name, int count) {
        HostReviewReportTagRow row = new HostReviewReportTagRow();
        row.setTagName(name);
        row.setCount(count);
        return row;
    }

    private HostReviewReportRecentRow review(String content) {
        HostReviewReportRecentRow row = new HostReviewReportRecentRow();
        row.setContent(content);
        return row;
    }

    private Map<Integer, Integer> ratingMap(int five, int four, int three, int two, int one) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(5, five);
        map.put(4, four);
        map.put(3, three);
        map.put(2, two);
        map.put(1, one);
        return map;
    }

    private HostReviewAiSummaryRequest request() {
        HostReviewAiSummaryRequest request = new HostReviewAiSummaryRequest();
        request.setAccommodationId(1L);
        return request;
    }

    private void assertSizeBetween(List<String> items, int min, int max) {
        assertNotNull(items);
        assertTrue(items.size() >= min && items.size() <= max);
    }

    private void assertSizeEquals(List<String> items, int size) {
        assertNotNull(items);
        assertEquals(size, items.size());
    }

    private void assertContainsEvidence(HostReviewAiSummaryResponse response) {
        List<List<String>> sections = new ArrayList<>();
        sections.add(response.getOverview());
        sections.add(response.getPositives());
        sections.add(response.getNegatives());
        sections.add(response.getActions());
        sections.add(response.getRisks());
        for (List<String> lines : sections) {
            for (String line : lines) {
                assertNotNull(line);
                assertTrue(line.contains("||"));
            }
        }
    }

    private void assertActionLength(HostReviewAiSummaryResponse response) {
        for (String line : response.getActions()) {
            String main = line.split("\\|\\|")[0];
            assertTrue(main.length() <= 70);
        }
    }
}
