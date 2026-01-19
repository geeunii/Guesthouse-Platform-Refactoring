package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSuggestionContext;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSuggestionRequest;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSuggestionResponse;
import com.ssg9th2team.geharbang.domain.ai.gemini.GeminiTextClient;
import com.ssg9th2team.geharbang.domain.ai.vision.VisionImageAnalyzer;
import com.ssg9th2team.geharbang.domain.ai.vision.dto.VisionLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationAiService {

    private final VisionImageAnalyzer visionImageAnalyzer;
    private final GeminiTextClient geminiTextClient;

    public AccommodationAiSuggestionResponse suggest(Long hostId, AccommodationAiSuggestionRequest request) {
        if (request == null || request.getImages() == null || request.getImages().isEmpty()) {
            throw new IllegalArgumentException("이미지 데이터가 필요합니다.");
        }
        VisionImageAnalyzer.ImageAnalysisResult analysisResult = visionImageAnalyzer.analyzeMultiple(request.getImages());
        log.debug("Generating AI suggestion for hostId={} visionEnabled={}", hostId, analysisResult.isVisionEnabled());
        String prompt = buildPrompt(request.getContext(), analysisResult);
        GeminiTextClient.TextCompletionResult aiResult = geminiTextClient.generateSuggestion(prompt, request.resolveLanguage());
        List<VisionLabel> labels = analysisResult.getLabels();
        AccommodationAiSuggestionResponse.TokenUsage usage = null;
        if (aiResult.getTokenUsage() != null) {
            usage = AccommodationAiSuggestionResponse.TokenUsage.builder()
                    .prompt(aiResult.getTokenUsage().getPromptTokens())
                    .completion(aiResult.getTokenUsage().getCompletionTokens())
                    .total(aiResult.getTokenUsage().getTotalTokens())
                    .build();
        }
        return AccommodationAiSuggestionResponse.builder()
                .name(aiResult.getName())
                .description(aiResult.getDescription())
                .confidence(aiResult.getConfidence())
                .visionLabels(labels)
                .visionText(analysisResult.getFullText())
                .model(aiResult.getModel())
                .generatedAt(aiResult.getGeneratedAt())
                .tokenUsage(usage)
                .build();
    }

    private String buildPrompt(AccommodationAiSuggestionContext context, VisionImageAnalyzer.ImageAnalysisResult analysisResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("아래 정보와 이미지를 참고하여 숙소 이름과 소개문을 제안해라.\n");
        if (context != null) {
            if (StringUtils.hasText(context.getExistingName())) {
                builder.append("- 기존 이름: ").append(context.getExistingName()).append("\n");
            }
            if (StringUtils.hasText(context.getExistingDescription())) {
                builder.append("- 기존 소개: ").append(context.getExistingDescription()).append("\n");
            }
            if (StringUtils.hasText(context.getStayType())) {
                builder.append("- 숙소 유형: ").append(context.getStayType()).append("\n");
            }
            String location = joinLocation(context.getCity(), context.getDistrict(), context.getTownship());
            if (StringUtils.hasText(location)) {
                builder.append("- 위치: ").append(location).append("\n");
            }
            if (context.getThemes() != null && !context.getThemes().isEmpty()) {
                builder.append("- 테마: ").append(String.join(", ", context.getThemes())).append("\n");
            }
        }
        if (analysisResult != null) {
            if (analysisResult.getLabels() != null && !analysisResult.getLabels().isEmpty()) {
                String labelSummary = analysisResult.getLabels().stream()
                        .limit(5)
                        .map(label -> label.getDescription() + "(" + Math.round(label.getScore() * 100) + "%)")
                        .collect(Collectors.joining(", "));
                builder.append("- Vision 라벨: ").append(labelSummary).append("\n");
            }
            if (StringUtils.hasText(analysisResult.getFullText())) {
                builder.append("- 이미지 OCR 텍스트: ").append(truncate(analysisResult.getFullText(), 400)).append("\n");
            }
            builder.append("- Vision 사용 가능 여부: ").append(analysisResult.isVisionEnabled() ? "Y" : "N").append("\n");
        }
        builder.append("결과는 간결하고 감성적인 톤으로 작성한다.");
        return builder.toString();
    }

    private String joinLocation(String city, String district, String township) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(city)) {
            builder.append(city.trim());
        }
        if (StringUtils.hasText(district)) {
            if (builder.length() > 0) builder.append(" ");
            builder.append(district.trim());
        }
        if (StringUtils.hasText(township)) {
            if (builder.length() > 0) builder.append(" ");
            builder.append(township.trim());
        }
        return builder.toString();
    }

    private String truncate(String value, int limit) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() <= limit) {
            return trimmed;
        }
        return trimmed.substring(0, limit) + "...";
    }
}
