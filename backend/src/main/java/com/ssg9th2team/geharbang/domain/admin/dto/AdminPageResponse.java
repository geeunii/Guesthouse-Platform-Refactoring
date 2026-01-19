package com.ssg9th2team.geharbang.domain.admin.dto;

import java.util.List;

public record AdminPageResponse<T>(
        List<T> items,
        AdminPageMeta page
) {
    public static <T> AdminPageResponse<T> of(List<T> items, int page, int size, long totalElements, int totalPages) {
        return new AdminPageResponse<>(items, new AdminPageMeta(page, size, totalElements, totalPages));
    }
}
