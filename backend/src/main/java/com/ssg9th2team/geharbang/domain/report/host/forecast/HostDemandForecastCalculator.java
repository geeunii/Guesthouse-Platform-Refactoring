package com.ssg9th2team.geharbang.domain.report.host.forecast;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastBaseline;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDaily;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDiagnostics;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastSummary;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class HostDemandForecastCalculator {

    private static final int BACKTEST_DAYS = 28;
    private static final int TREND_WINDOW = 14;
    private static final int LEVEL_WINDOW = 28;
    private static final int MIN_HISTORY_DAYS = 42;
    private static final double EWMA_ALPHA = 0.3;
    private static final double SHRINK_K = 5.0;
    private static final double Z_SCORE = 1.64;

    public HostDemandForecastResult generate(
            Map<LocalDate, Double> history,
            Set<LocalDate> holidays,
            LocalDate today,
            int horizonDays,
            int historyDays
    ) {
        HostDemandForecastResult result = new HostDemandForecastResult();

        double recentAvg7 = averageForLastDays(history, 7);
        double recentAvg28 = averageForLastDays(history, 28);
        double overallAvg = average(new ArrayList<>(history.values()));
        int nonZeroCount = (int) history.values().stream().filter(value -> value != null && value > 0).count();

        boolean useV2 = historyDays >= MIN_HISTORY_DAYS && overallAvg > 0 && nonZeroCount >= 7;
        double baseAverage = (recentAvg7 * 0.6) + (recentAvg28 * 0.4);

        Components components = buildComponents(history, holidays, overallAvg);
        HostForecastDiagnostics diagnostics = buildDiagnostics(history, holidays, today, useV2, baseAverage);
        double sigma = diagnostics.getBacktestDays() > 0 ? diagnosticsToSigma(history, holidays, today, useV2, baseAverage) : 0.0;

        List<HostForecastDaily> forecastDaily = new ArrayList<>();
        double predictedTotal = 0.0;
        for (int i = 1; i <= horizonDays; i++) {
            LocalDate date = today.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();
            boolean isHoliday = holidays.contains(date);
            double predicted = useV2
                    ? predict(components, dow, isHoliday, i)
                    : predictV1(baseAverage, components.weekdayFactors.getOrDefault(dow, 1.0));
            predicted = Math.max(0, predicted);
            double rounded = Math.round(predicted);
            predictedTotal += rounded;

            HostForecastDaily daily = new HostForecastDaily();
            daily.setDate(date);
            daily.setDowLabel(dow.getDisplayName(TextStyle.SHORT, Locale.KOREA));
            daily.setWeekend(dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            daily.setHoliday(isHoliday);
            daily.setPredictedValue(rounded);
            if (sigma > 0) {
                daily.setLow(roundOneDecimal(Math.max(0, predicted - (Z_SCORE * sigma))));
                daily.setHigh(roundOneDecimal(predicted + (Z_SCORE * sigma)));
            } else {
                daily.setLow(0);
                daily.setHigh(0);
            }
            forecastDaily.add(daily);
        }

        HostForecastBaseline baseline = new HostForecastBaseline();
        baseline.setRecentAvg7(roundOneDecimal(recentAvg7));
        baseline.setRecentAvg28(roundOneDecimal(recentAvg28));
        baseline.setWeekdayFactors(convertWeekdayFactors(components.weekdayFactors));
        baseline.setLevel(roundOneDecimal(components.level));
        baseline.setTrendPerDay(roundOneDecimal(components.trendPerDay));
        baseline.setHolidayFactor(roundOneDecimal(components.holidayFactor));

        HostForecastSummary summary = new HostForecastSummary();
        summary.setPredictedTotal(roundOneDecimal(predictedTotal));
        summary.setPredictedAvgPerDay(roundOneDecimal(predictedTotal / Math.max(1, horizonDays)));

        result.setModelVersion(useV2 ? "v2-seasonal-trend" : "v1-avg");
        result.setExplain(buildExplain(useV2, historyDays));
        result.setDiagnostics(diagnostics);
        result.setBaseline(baseline);
        result.setSummary(summary);
        result.setDaily(forecastDaily);
        return result;
    }

    private String buildExplain(boolean useV2, int historyDays) {
        if (useV2) {
            return String.format(Locale.KOREA, "최근 %d일 데이터 기반, 요일/공휴일/추세 반영", historyDays);
        }
        return "데이터 부족으로 최근 평균 기반(v1) 적용";
    }

    private Components buildComponents(Map<LocalDate, Double> history, Set<LocalDate> holidays, double overallAvg) {
        Components components = new Components();
        List<Double> values = new ArrayList<>(history.values());
        components.level = ewma(lastValues(values, LEVEL_WINDOW), EWMA_ALPHA);
        components.trendPerDay = linearTrend(lastValues(values, TREND_WINDOW));
        components.weekdayFactors = computeWeekdayFactors(history, overallAvg);
        components.holidayFactor = computeHolidayFactor(history, holidays, overallAvg);
        return components;
    }

    private HostForecastDiagnostics buildDiagnostics(
            Map<LocalDate, Double> history,
            Set<LocalDate> holidays,
            LocalDate today,
            boolean useV2,
            double baseAverage
    ) {
        HostForecastDiagnostics diagnostics = new HostForecastDiagnostics();
        int backtestDays = Math.min(BACKTEST_DAYS, Math.max(0, history.size() - TREND_WINDOW));
        List<Residual> residuals = backtest(history, holidays, today, backtestDays, useV2, baseAverage);
        diagnostics.setTrainingDays(history.size());
        diagnostics.setBacktestDays(residuals.size());
        diagnostics.setMae(roundOneDecimal(meanAbsolute(residuals)));
        diagnostics.setRmse(roundOneDecimal(rmse(residuals)));
        diagnostics.setMape(roundOneDecimal(mape(residuals)));
        return diagnostics;
    }

    private double diagnosticsToSigma(
            Map<LocalDate, Double> history,
            Set<LocalDate> holidays,
            LocalDate today,
            boolean useV2,
            double baseAverage
    ) {
        int backtestDays = Math.min(BACKTEST_DAYS, Math.max(0, history.size() - TREND_WINDOW));
        List<Residual> residuals = backtest(history, holidays, today, backtestDays, useV2, baseAverage);
        if (residuals.isEmpty()) return 0.0;
        double mean = residuals.stream().mapToDouble(r -> r.error).average().orElse(0.0);
        double variance = residuals.stream()
                .mapToDouble(r -> Math.pow(r.error - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private List<Residual> backtest(
            Map<LocalDate, Double> history,
            Set<LocalDate> holidays,
            LocalDate today,
            int backtestDays,
            boolean useV2,
            double baseAverage
    ) {
        if (backtestDays <= 0) return List.of();

        TreeMap<LocalDate, Double> ordered = new TreeMap<>(history);
        List<LocalDate> dates = new ArrayList<>(ordered.keySet());
        int total = dates.size();
        int startIndex = Math.max(0, total - backtestDays);
        List<Residual> residuals = new ArrayList<>();

        for (int i = startIndex; i < total; i++) {
            LocalDate date = dates.get(i);
            if (date.isAfter(today)) continue;
            LocalDate trainEnd = date.minusDays(1);
            if (!ordered.containsKey(trainEnd)) continue;
            Map<LocalDate, Double> training = ordered.headMap(trainEnd, true);
            if (training.size() < TREND_WINDOW) continue;

            double overallAvg = average(new ArrayList<>(training.values()));
            Components components = buildComponents(training, holidays, overallAvg);
            DayOfWeek dow = date.getDayOfWeek();
            boolean isHoliday = holidays.contains(date);
            double baseForTrain = baseAverage;
            if (!useV2) {
                double avg7 = averageForLastDays(training, 7);
                double avg28 = averageForLastDays(training, 28);
                baseForTrain = (avg7 * 0.6) + (avg28 * 0.4);
            }

            double predicted = useV2
                    ? predict(components, dow, isHoliday, 1)
                    : predictV1(baseForTrain, components.weekdayFactors.getOrDefault(dow, 1.0));

            double actual = ordered.getOrDefault(date, 0.0);
            residuals.add(new Residual(actual - predicted, actual, predicted));
        }

        return residuals;
    }

    private double predict(Components components, DayOfWeek dow, boolean isHoliday, int horizon) {
        double base = components.level + (components.trendPerDay * horizon);
        double weekdayFactor = components.weekdayFactors.getOrDefault(dow, 1.0);
        double holidayMultiplier = isHoliday ? components.holidayFactor : 1.0;
        return base * weekdayFactor * holidayMultiplier;
    }

    private double predictV1(double baseAverage, double weekdayFactor) {
        return baseAverage * weekdayFactor;
    }

    private Map<DayOfWeek, Double> computeWeekdayFactors(Map<LocalDate, Double> history, double overallAvg) {
        Map<DayOfWeek, List<Double>> grouped = history.entrySet().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        entry -> entry.getKey().getDayOfWeek(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(Map.Entry::getValue, java.util.stream.Collectors.toList())
                ));

        Map<DayOfWeek, Double> factors = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Double> values = grouped.getOrDefault(day, List.of());
            double avg = average(values);
            int count = values.size();
            if (overallAvg <= 0 || count == 0) {
                factors.put(day, 1.0);
            } else {
                double factor = ((avg * count) + (overallAvg * SHRINK_K)) / (overallAvg * (count + SHRINK_K));
                factors.put(day, factor);
            }
        }
        return factors;
    }

    private double computeHolidayFactor(Map<LocalDate, Double> history, Set<LocalDate> holidays, double overallAvg) {
        if (overallAvg <= 0) return 1.0;
        List<Double> holidayValues = new ArrayList<>();
        List<Double> nonHolidayValues = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : history.entrySet()) {
            if (holidays.contains(entry.getKey())) {
                holidayValues.add(entry.getValue());
            } else {
                nonHolidayValues.add(entry.getValue());
            }
        }
        if (holidayValues.size() < 3 || nonHolidayValues.isEmpty()) return 1.0;
        double holidayAvg = average(holidayValues);
        double nonHolidayAvg = average(nonHolidayValues);
        if (nonHolidayAvg <= 0) return 1.0;
        double factor = ((holidayAvg * holidayValues.size()) + (nonHolidayAvg * SHRINK_K))
                / (nonHolidayAvg * (holidayValues.size() + SHRINK_K));
        return factor;
    }

    private Map<String, Double> convertWeekdayFactors(Map<DayOfWeek, Double> factors) {
        Map<String, Double> response = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            response.put(day.name(), roundOneDecimal(factors.getOrDefault(day, 1.0)));
        }
        return response;
    }

    private List<Double> lastValues(List<Double> values, int days) {
        if (values.isEmpty()) return List.of();
        int start = Math.max(0, values.size() - days);
        return values.subList(start, values.size());
    }

    private double averageForLastDays(Map<LocalDate, Double> values, int days) {
        if (values.isEmpty()) return 0.0;
        List<Double> list = new ArrayList<>(values.values());
        int start = Math.max(0, list.size() - days);
        return average(list.subList(start, list.size()));
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double ewma(List<Double> values, double alpha) {
        if (values.isEmpty()) return 0.0;
        double result = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            result = (alpha * values.get(i)) + ((1 - alpha) * result);
        }
        return result;
    }

    private double linearTrend(List<Double> values) {
        int n = values.size();
        if (n < 2) return 0.0;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0) return 0.0;
        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }

    private double meanAbsolute(List<Residual> residuals) {
        if (residuals.isEmpty()) return 0.0;
        double sum = residuals.stream().mapToDouble(r -> Math.abs(r.error)).sum();
        return sum / residuals.size();
    }

    private double rmse(List<Residual> residuals) {
        if (residuals.isEmpty()) return 0.0;
        double sum = residuals.stream().mapToDouble(r -> r.error * r.error).sum();
        return Math.sqrt(sum / residuals.size());
    }

    private double mape(List<Residual> residuals) {
        double sum = 0.0;
        int count = 0;
        for (Residual residual : residuals) {
            if (residual.actual <= 0) continue;
            sum += Math.abs(residual.error) / residual.actual;
            count += 1;
        }
        if (count == 0) return 0.0;
        return (sum / count) * 100.0;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static class Components {
        private double level;
        private double trendPerDay;
        private Map<DayOfWeek, Double> weekdayFactors = new EnumMap<>(DayOfWeek.class);
        private double holidayFactor;
    }

    private record Residual(double error, double actual, double predicted) {
    }
}
