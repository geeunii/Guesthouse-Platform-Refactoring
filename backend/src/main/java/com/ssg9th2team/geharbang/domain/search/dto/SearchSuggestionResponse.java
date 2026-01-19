package com.ssg9th2team.geharbang.domain.search.dto;

public record SearchSuggestionResponse(String type, String value) {
    public static SearchSuggestionResponse accommodation(String value) {
        return new SearchSuggestionResponse("ACCOMMODATION", value);
    }

    public static SearchSuggestionResponse region(String value) {
        return new SearchSuggestionResponse("REGION", value);
    }
}
