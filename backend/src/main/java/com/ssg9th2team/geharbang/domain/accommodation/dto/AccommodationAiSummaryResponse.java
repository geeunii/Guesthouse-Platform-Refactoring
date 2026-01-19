package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class AccommodationAiSummaryResponse {
    private final String accommodationName;
    private final String locationTag;
    private final List<String> keywords;
    private final String moodDescription;
    private final String tip;
    private final long reviewCount;
}
