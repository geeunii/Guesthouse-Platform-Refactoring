package com.ssg9th2team.geharbang.domain.search.dto;

public record SearchResolveResponse(Long accommodationsId, String accommodationsName) {
    public static SearchResolveResponse of(Long accommodationsId, String accommodationsName) {
        return new SearchResolveResponse(accommodationsId, accommodationsName);
    }
}
