package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostAiInsightSection;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDaily;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDiagnostics;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastSummary;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RuleBasedInsightComposer {

    private static final int ACTION_COUNT = 5;

    private final RuleBasedSummaryClient ruleBasedSummaryClient;

    public RuleBasedInsightComposer(RuleBasedSummaryClient ruleBasedSummaryClient) {
        this.ruleBasedSummaryClient = ruleBasedSummaryClient;
    }

    public List<HostAiInsightSection> buildReviewSections(HostReviewReportSummaryResponse summary) {
        HostReviewAiSummaryRequest request = new HostReviewAiSummaryRequest();
        request.setAccommodationId(summary.getAccommodationId());
        request.setFrom(summary.getFrom());
        request.setTo(summary.getTo());
        HostReviewAiSummaryResponse result = ruleBasedSummaryClient.generate(summary, request);

        List<HostAiInsightSection> sections = new ArrayList<>();
        sections.add(section("총평", result.getOverview()));
        sections.add(section("좋았던 점", result.getPositives()));
        sections.add(section("개선 포인트", result.getNegatives()));
        sections.add(section("다음 액션", result.getActions()));
        sections.add(section("모니터링", result.getRisks())); // "주의·리스크" -> "모니터링"으로 통일
        return sections;
    }

    public List<HostAiInsightSection> buildThemeSections(HostThemeReportResponse report) {
        List<HostThemeReportRow> rows = report.getRows() == null ? List.of() : report.getRows();
        List<HostThemeReportRow> sorted = rows.stream()
                .sorted(Comparator.comparingLong(row -> -metricValue(row, report.getMetric())))
                .collect(Collectors.toList());
        HostThemeReportRow top = sorted.isEmpty() ? null : sorted.get(0);
        HostThemeReportRow second = sorted.size() > 1 ? sorted.get(1) : null;

        List<String> overview = new ArrayList<>();
        if (sorted.isEmpty()) {
            overview.add(formatLine("테마 데이터가 부족해 요약 근거가 제한적입니다.", "테마 데이터 없음"));
        } else {
            overview.add(formatLine(
                    String.format(Locale.KOREA, "상위 테마는 '%s'이며 기간 성과를 견인합니다.", top.getThemeName()),
                    formatThemeMetric(top, report.getMetric())
            ));
            if (second != null) {
                overview.add(formatLine(
                        String.format(Locale.KOREA, "2위 테마 '%s'도 잠재 성장 여지가 있습니다.", second.getThemeName()),
                        formatThemeMetric(second, report.getMetric())
                ));
            }
        }

        List<String> positives = new ArrayList<>();
        if (top != null) {
            positives.add(formatLine(
                    String.format(Locale.KOREA, "상위 테마 '%s'를 마케팅 메시지의 중심으로 유지하세요.", top.getThemeName()),
                    formatThemeMetric(top, report.getMetric())
            ));
        } else {
            positives.add(formatLine("테마 강점 판단을 위한 데이터가 부족합니다.", "테마 데이터 없음"));
        }

        List<String> negatives = new ArrayList<>();
        if (sorted.size() >= 3) {
            HostThemeReportRow low = sorted.get(sorted.size() - 1);
            negatives.add(formatLine(
                    String.format(Locale.KOREA, "성과가 낮은 테마 '%s'는 메시지 정비가 필요합니다.", low.getThemeName()),
                    formatThemeMetric(low, report.getMetric())
            ));
        } else if (sorted.isEmpty()) {
            negatives.add(formatLine("유의미한 부정 신호 없음.", "데이터 부족"));
        } else {
            // [수정] 테마가 적더라도 개선 포인트를 비워두지 않고 일반적인 조언 추가
            negatives.add(formatLine("현재 테마 수가 적어 고객 선택의 폭이 좁을 수 있습니다.", "테마 다양성 부족"));
            negatives.add(formatLine("경쟁 숙소 대비 차별화된 테마 발굴을 고려해보세요.", "경쟁력 강화 필요"));
        }

        List<String> actions = new ArrayList<>();
        actions.add(action("상위 테마 1~2개를 메인 배너/소개 문구에 집중 반영하기.",
                top != null ? "상위 테마 '" + top.getThemeName() + "'" : "테마 데이터 부족"));
        actions.add(action("성과 낮은 테마는 사진과 설명 키워드를 재구성하기.",
                sorted.size() >= 3 ? "하위 테마 '" + sorted.get(sorted.size() - 1).getThemeName() + "'" : "테마 수 부족"));
        actions.add(action("테마별 예약/매출 변화를 월 단위로 추적하기.",
                rows.isEmpty() ? "데이터 부족" : "테마 수 " + rows.size() + "개"));
        actions.add(action("상위 테마와 연관된 체험/서비스 문구를 보강하기.",
                top != null ? formatThemeMetric(top, report.getMetric()) : "테마 데이터 부족"));
        actions.add(action("테마 노출 순서를 성과 기준으로 재정렬하기.",
                rows.isEmpty() ? "데이터 부족" : "성과 편차 존재"));

        List<String> risks = new ArrayList<>();
        if (rows.size() >= 2) {
            risks.add(formatLine("특정 테마에 성과가 집중될 경우 리스크가 있습니다.", "상위 테마 편중"));
        } else {
            risks.add(formatLine("유의미한 리스크 없음.", "테마 데이터 부족"));
        }

        return List.of(
                section("트렌드 요약", trim(overview, 2)), // "핵심 요약" -> "트렌드 요약"
                section("강점", trim(positives, 3)),
                section("보완할 점", trim(negatives, 3)), // "개선 포인트" -> "보완할 점"
                section("다음 액션", ensureCount(actions, ACTION_COUNT)),
                section("모니터링", trim(risks, 2))
        );
    }

    public List<HostAiInsightSection> buildDemandSections(HostForecastResponse forecast) {
        HostForecastSummary summary = forecast.getForecastSummary();
        HostForecastDiagnostics diagnostics = forecast.getDiagnostics();
        HostForecastDaily peak = findPeak(forecast.getForecastDaily());

        List<String> overview = new ArrayList<>();
        if (summary == null) {
            overview.add(formatLine("예측 요약 데이터가 부족해 해석이 제한됩니다.", "예측 요약 없음"));
        } else {
            overview.add(formatLine(
                    String.format(Locale.KOREA, "예측 합계 %.1f, 일평균 %.1f 수준입니다.", summary.getPredictedTotal(), summary.getPredictedAvgPerDay()),
                    "예측 요약"
            ));
            if (peak != null) {
                overview.add(formatLine(
                        String.format(Locale.KOREA, "피크는 %s(%s)로 예상됩니다.", peak.getDate(), peak.getDowLabel()),
                        String.format(Locale.KOREA, "예측 최대 %.1f", peak.getPredictedValue())
                ));
            }
        }

        List<String> positives = new ArrayList<>();
        if (diagnostics != null && diagnostics.getMape() > 0) {
            positives.add(formatLine(
                    String.format(Locale.KOREA, "예측 오차(MAPE)가 %.1f%%로 참고 가능한 수준입니다.", diagnostics.getMape()),
                    "MAPE"
            ));
        } else {
            positives.add(formatLine("오차 지표가 제한적이라 참고 지표로 활용하세요.", "MAPE 데이터 부족"));
        }

        List<String> negatives = new ArrayList<>();
        negatives.add(formatLine("수요 변동을 대비해 주간 단위 점검이 필요합니다.", "예측 기반 운영 필요"));

        List<String> actions = new ArrayList<>();
        actions.add(action("예측 피크 구간에 맞춰 청소·인력 배치를 사전 확보하기.",
                summary != null ? "예측 합계 " + summary.getPredictedTotal() : "예측 요약 없음"));
        actions.add(action("예약 리드타임에 맞춰 가격/프로모션 노출을 조정하기.",
                forecast.getTarget() != null ? "예측 타깃 " + forecast.getTarget() : "타깃 정보 부족"));
        actions.add(action("공휴일/주말 변동을 반영한 운영 체크리스트를 업데이트하기.",
                diagnostics != null ? "학습 기간 " + diagnostics.getTrainingDays() + "일" : "진단 데이터 부족"));
        actions.add(action("수요가 낮은 구간에 리뷰 이벤트/로컬 제휴를 집중하기.",
                summary != null ? "일평균 " + summary.getPredictedAvgPerDay() : "예측 요약 없음"));
        actions.add(action("예측 오차를 주간으로 리뷰해 보정 필요성을 점검하기.",
                diagnostics != null && diagnostics.getMape() > 0 ? "MAPE " + diagnostics.getMape() + "%" : "오차 데이터 부족"));

        List<String> risks = new ArrayList<>();
        if (diagnostics != null && diagnostics.getMape() > 25) {
            risks.add(formatLine("예측 오차가 높아 보수적으로 운영할 필요가 있습니다.", "MAPE"));
        } else {
            risks.add(formatLine("유의미한 리스크 없음.", "예측 오차 양호/데이터 부족"));
        }

        return List.of(
                section("수요 예측 요약", trim(overview, 2)), // "핵심 요약" -> "수요 예측 요약"
                section("기회 요인", trim(positives, 3)), // "해석 포인트" -> "기회 요인"
                section("리스크 요인", trim(negatives, 3)), // "개선 포인트" -> "리스크 요인"
                section("다음 액션", ensureCount(actions, ACTION_COUNT)), // "운영 액션" -> "다음 액션"
                section("모니터링", trim(risks, 2))
        );
    }

    private HostAiInsightSection section(String title, List<String> items) {
        HostAiInsightSection section = new HostAiInsightSection();
        section.setTitle(title);
        section.setItems(items == null || items.isEmpty()
                ? List.of(formatLine("데이터 부족으로 판단이 제한됩니다.", "데이터 부족"))
                : items);
        return section;
    }

    private List<String> trim(List<String> items, int max) {
        if (items == null || items.isEmpty()) {
            return List.of(formatLine("데이터 부족으로 판단이 제한됩니다.", "데이터 부족"));
        }
        return items.stream().limit(max).collect(Collectors.toList());
    }

    private List<String> ensureCount(List<String> items, int size) {
        List<String> result = new ArrayList<>(items);
        while (result.size() < size) {
            result.add(formatLine("운영 점검 루틴을 유지하기.", "데이터 부족"));
        }
        return result.size() > size ? result.subList(0, size) : result;
    }

    private String action(String text, String evidence) {
        String safeEvidence = (evidence == null || evidence.isBlank()) ? "데이터 부족" : evidence;
        return formatLine(trimLength(text, 70), safeEvidence);
    }

    private String formatLine(String main, String evidence) {
        String safeMain = (main == null || main.isBlank()) ? "데이터 부족" : main.trim();
        String safeEvidence = (evidence == null || evidence.isBlank()) ? "데이터 부족" : evidence.trim();
        return safeMain + "||" + safeEvidence;
    }

    private String trimLength(String text, int maxLen) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) return trimmed;
        return trimmed.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private HostForecastDaily findPeak(List<HostForecastDaily> rows) {
        if (rows == null || rows.isEmpty()) return null;
        HostForecastDaily best = null;
        for (HostForecastDaily row : rows) {
            if (row == null) continue;
            if (best == null || row.getPredictedValue() > best.getPredictedValue()) {
                best = row;
            }
        }
        return best;
    }

    private long metricValue(HostThemeReportRow row, String metric) {
        if ("revenue".equalsIgnoreCase(metric)) {
            return row.getRevenueSum() != null ? row.getRevenueSum() : 0;
        }
        return row.getReservationCount() != null ? row.getReservationCount() : 0;
    }

    private String formatThemeMetric(HostThemeReportRow row, String metric) {
        if ("revenue".equalsIgnoreCase(metric)) {
            return "매출 " + (row.getRevenueSum() != null ? row.getRevenueSum() : 0) + "원";
        }
        return "예약 " + (row.getReservationCount() != null ? row.getReservationCount() : 0) + "건";
    }
}
