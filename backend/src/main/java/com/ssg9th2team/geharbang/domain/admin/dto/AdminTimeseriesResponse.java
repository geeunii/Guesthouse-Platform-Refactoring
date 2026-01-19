package com.ssg9th2team.geharbang.domain.admin.dto;

import java.util.List;

public record AdminTimeseriesResponse(
        String metric,
        List<AdminTimeseriesPoint> points
) {
}
