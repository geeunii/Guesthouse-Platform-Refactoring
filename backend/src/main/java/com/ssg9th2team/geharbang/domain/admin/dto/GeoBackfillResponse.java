package com.ssg9th2team.geharbang.domain.admin.dto;

public record GeoBackfillResponse(
        int requested,
        int processed,
        int updated,
        int skipped,
        int failed,
        long remaining
) {
}
