package com.ssg9th2team.geharbang.domain.admin.dto;

import java.time.LocalDateTime;

public record AdminUserSummary(
        Long userId,
        String email,
        String role,
        String phone,
        Boolean hostApproved,
        Boolean suspended,
        LocalDateTime createdAt
) {
}
