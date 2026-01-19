package com.ssg9th2team.geharbang.domain.admin.dto;

public record AdminPageMeta(
        int number,
        int size,
        long totalElements,
        int totalPages
) {
}
