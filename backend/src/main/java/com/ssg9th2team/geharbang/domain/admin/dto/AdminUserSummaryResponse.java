package com.ssg9th2team.geharbang.domain.admin.dto;

public record AdminUserSummaryResponse(
        long totalUsers,
        long totalGuests,
        long totalHosts,
        long pendingHostApprovals,
        long suspendedUsers
) {
}
