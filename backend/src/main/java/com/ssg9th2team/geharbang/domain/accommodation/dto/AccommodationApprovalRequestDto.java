package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccommodationApprovalRequestDto {
    // 관리자 승인/반려용
    private String approvalStatus;    // APPROVED, REJECTED
    private String rejectionReason;   // 반려 시 사유
}
