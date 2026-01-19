package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccommodationAiSuggestionContext {
    private String city;
    private String district;
    private String township;
    private String stayType;
    private String existingName;
    private String existingDescription;
    private List<String> themes;
}
