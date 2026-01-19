package com.ssg9th2team.geharbang.domain.main.dto;

public record PublicPageMeta(
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
