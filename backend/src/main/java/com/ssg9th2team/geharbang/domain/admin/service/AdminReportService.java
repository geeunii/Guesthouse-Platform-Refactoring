package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportDetailResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminReportSummary;
import com.ssg9th2team.geharbang.domain.admin.log.AdminLogConstants;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReviewReportJpaRepository reportRepository;
    private final ReviewJpaRepository reviewRepository;
    private final UserRepository userRepository;
    private final AdminLogService adminLogService;

    public AdminPageResponse<AdminReportSummary> getReports(String status, String type, String query, int page, int size, String sort) {
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        List<ReviewReport> filtered = reportRepository.findAll(sorting).stream()
                .filter(report -> matchesStatus(report, status))
                .filter(report -> matchesType(type))
                .filter(report -> matchesQuery(report, query))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<AdminReportSummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();
        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    public AdminReportDetailResponse getReportDetail(Long reportId) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        
        User reporter = userRepository.findById(report.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporter not found"));

        ReviewEntity review = reviewRepository.findById(report.getReviewId()).orElse(null);

        return AdminReportDetailResponse.from(report, reporter, review);
    }

    @Transactional
    public AdminReportDetailResponse resolveReport(Long adminUserId, Long reportId, String action, String memo) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        
        String beforeState = report.getState();
        if (StringUtils.hasText(action)) {
            String newState = action.trim().toUpperCase();
            report.updateState(newState);
            
            // 리뷰 블라인드 처리 (BLIND 액션 시)
            if ("BLIND".equals(newState) || "PROCESSED".equals(newState)) {
                ReviewEntity review = reviewRepository.findById(report.getReviewId()).orElse(null);
                if (review != null) {
                    review.softDelete(); // isDeleted = true
                    reviewRepository.save(review);
                }
            }
        }
        
        ReviewReport saved = reportRepository.save(report);
        
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("before", java.util.Map.of("state", beforeState));
        metadata.put("after", java.util.Map.of("state", saved.getState()));
        metadata.put("reportId", saved.getReportId());
        if (saved.getReviewId() != null) {
            metadata.put("reviewId", saved.getReviewId());
        }
        
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_REVIEW,
                saved.getReviewId(),
                AdminLogConstants.ACTION_RESOLVE,
                memo,
                metadata
        );
        
        return getReportDetail(saved.getReportId());
    }

    private boolean matchesStatus(ReviewReport report, String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return true;
        }
        return status.trim().equalsIgnoreCase(report.getState());
    }

    private boolean matchesType(String type) {
        if (!StringUtils.hasText(type) || "all".equalsIgnoreCase(type)) {
            return true;
        }
        return "review".equalsIgnoreCase(type);
    }

    private boolean matchesQuery(ReviewReport report, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String normalized = query.toLowerCase();
        String reason = report.getReason() != null ? report.getReason().toLowerCase() : "";
        return reason.contains(normalized);
    }

    private AdminReportSummary toSummary(ReviewReport report) {
        return new AdminReportSummary(
                report.getReportId(),
                "REVIEW",
                report.getState(),
                report.getReason(),
                report.getCreatedAt()
        );
    }
}
