package com.ssg9th2team.geharbang.domain.accommodation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssg9th2team.geharbang.domain.ai.vision.dto.VisionLabel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationAiSuggestionResponse {
    private final String name;
    private final String description;
    private final Double confidence;
    private final List<VisionLabel> visionLabels;
    private final String visionText;
    private final String model;
    private final TokenUsage tokenUsage;
    private final String generatedAt;

    @Getter
    @Builder
    public static class TokenUsage {
        private final Long prompt;
        private final Long completion;
        private final Long total;
    }
}
