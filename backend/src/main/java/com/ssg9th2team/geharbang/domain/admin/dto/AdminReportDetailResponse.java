package com.ssg9th2team.geharbang.domain.admin.dto;

import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewImageEntity;
import com.ssg9th2team.geharbang.domain.auth.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReportDetailResponse(
        Long reportId,
        String reason,
        String description, // 신고자 상세 코멘트 (현재 엔티티엔 없음, reason으로 대체 가능)
        Reporter reporter,
        LocalDateTime createdAt,
        String status,
        String targetType,
        Long targetId,
        TargetData targetData
) {
    public record Reporter(
            Long id,
            String nickname,
            String email
    ) {}

    public record TargetData(
            // REVIEW
            String reviewContent,
            Double reviewRating,
            List<String> reviewImages,
            String reviewAuthorName,
            
            // USER (추후 확장)
            String reportedUserNickname,
            String reportedUserEmail,
            String reportedUserStatus
    ) {}

    public static AdminReportDetailResponse from(ReviewReport report, User reporter, ReviewEntity review) {
        Reporter reporterInfo = new Reporter(
                reporter.getId(),
                reporter.getNickname(),
                reporter.getEmail()
        );

        List<String> images = review != null && review.getImages() != null
                ? review.getImages().stream().map(ReviewImageEntity::getImageUrl).toList()
                : List.of();

        TargetData targetData = new TargetData(
                review != null ? review.getContent() : null,
                review != null && review.getRating() != null ? review.getRating().doubleValue() : null,
                images,
                review != null ? review.getAuthorName() : null,
                null, null, null
        );

        return new AdminReportDetailResponse(
                report.getReportId(),
                report.getReason(),
                report.getReason(), // description이 없으므로 reason 사용
                reporterInfo,
                report.getCreatedAt(),
                report.getState(),
                "REVIEW",
                report.getReviewId(),
                targetData
        );
    }
}
