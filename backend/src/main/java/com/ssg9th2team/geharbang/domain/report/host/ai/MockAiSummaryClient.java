package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class MockAiSummaryClient implements AiSummaryClient {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        double avgRating = summary.getAvgRating() != null ? summary.getAvgRating() : 0.0;
        int reviewCount = summary.getReviewCount() != null ? summary.getReviewCount() : 0;
        List<HostReviewReportTagRow> topTags = summary.getTopTags() != null ? summary.getTopTags() : List.of();

        List<String> overview = new ArrayList<>();
        if (avgRating >= 4.5) {
            overview.add("전반적으로 평점이 매우 높아 고객 만족도가 높습니다.");
        } else if (avgRating >= 4.0) {
            overview.add("평점이 안정적이며 큰 불만 없이 운영되고 있습니다.");
        } else if (avgRating >= 3.5) {
            overview.add("평점이 평균 수준으로, 개선 포인트 점검이 필요합니다.");
        } else {
            overview.add("평점 개선이 시급합니다. 주요 불만 요소를 우선 해결하세요.");
        }
        overview.add(String.format(Locale.KOREA, "리뷰 수는 %d건으로 집계되었습니다.", reviewCount));
        if (!topTags.isEmpty()) {
            overview.add("대표 태그는 " + topTags.get(0).getTagName() + " 입니다.");
        }

        List<String> positives = new ArrayList<>();
        if (!topTags.isEmpty()) {
            String tagLine = topTags.stream()
                    .limit(3)
                    .map(HostReviewReportTagRow::getTagName)
                    .collect(Collectors.joining(", "));
            positives.add("긍정 태그: " + tagLine);
        }
        positives.add("리뷰 작성 비율을 유지해 신뢰도를 높이고 있습니다.");
        if (avgRating >= 4.0) {
            positives.add("평점이 안정적이라 추천 유입 가능성이 높습니다.");
        }

        List<String> negatives = new ArrayList<>();
        if (avgRating < 4.0) {
            negatives.add("평점이 목표 대비 낮아 개선 포인트 점검이 필요합니다.");
        }
        if (reviewCount < 5) {
            negatives.add("리뷰 수가 적어 평점 변동성이 큽니다.");
        }
        negatives.add("최근 리뷰에서 반복되는 불만 키워드를 모니터링하세요.");

        List<String> actions = new ArrayList<>();
        actions.add("체크아웃 직후 리뷰 요청 메시지를 자동화하세요.");
        actions.add("상위 태그를 활용한 숙소 소개 문구를 강화하세요.");
        actions.add("부정 리뷰가 언급한 항목을 현장 점검 리스트에 추가하세요.");

        List<String> risks = new ArrayList<>();
        if (reviewCount < 10) {
            risks.add("리뷰 표본이 작아 성수기 변동에 민감할 수 있습니다.");
        }
        risks.add("평점 하락 시 예약 전환율이 급격히 떨어질 수 있습니다.");

        HostReviewAiSummaryResponse response = new HostReviewAiSummaryResponse();
        response.setAccommodationId(request.getAccommodationId());
        response.setFrom(summary.getFrom());
        response.setTo(summary.getTo());
        response.setGeneratedAt(ZonedDateTime.now(KST).toString());
        response.setOverview(overview);
        response.setPositives(positives);
        response.setNegatives(negatives);
        response.setActions(actions);
        response.setRisks(risks);
        return response;
    }
}
