package com.ssg9th2team.geharbang.domain.accommodation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccommodationAiSuggestionRequest {

    @NotEmpty(message = "이미지 데이터는 필수입니다.")
    private List<String> images;

    private String language = "ko";

    @Valid
    private AccommodationAiSuggestionContext context;

    public String resolveLanguage() {
        if (language == null || language.isBlank()) {
            return "ko";
        }
        return language.trim();
    }
}
