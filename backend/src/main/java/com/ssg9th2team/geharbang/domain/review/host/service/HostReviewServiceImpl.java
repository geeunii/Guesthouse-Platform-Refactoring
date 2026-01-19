package com.ssg9th2team.geharbang.domain.review.host.service;

import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReplyResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReplyRow;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewSummaryResponse;
import com.ssg9th2team.geharbang.domain.review.host.repository.mybatis.HostReviewMapper;
import com.ssg9th2team.geharbang.domain.review.host.repository.mybatis.HostReviewReplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HostReviewServiceImpl implements HostReviewService {

    private final HostReviewMapper hostReviewMapper;
    private final HostReviewReplyMapper hostReviewReplyMapper;
    private final ReviewReportJpaRepository reviewReportRepository;

    @Override
    @Transactional(readOnly = true)
    public HostReviewSummaryResponse getSummary(Long hostId) {
        HostReviewSummaryResponse summary = hostReviewMapper.selectSummary(hostId);
        if (summary == null) {
            HostReviewSummaryResponse empty = new HostReviewSummaryResponse();
            empty.setAvgRating(0.0);
            empty.setReviewCount(0);
            return empty;
        }
        if (summary.getAvgRating() == null) summary.setAvgRating(0.0);
        if (summary.getReviewCount() == null) summary.setReviewCount(0);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostReviewResponse> getReviews(Long hostId, Long accommodationId, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = safePage * safeSize;
        String normalizedSort = "oldest".equalsIgnoreCase(sort) ? "oldest" : "latest";
        return hostReviewMapper.selectReviews(hostId, accommodationId, offset, safeSize, normalizedSort);
    }

    @Override
    public HostReviewReplyResponse upsertReply(Long hostId, Long reviewId, String content) {
        String trimmed = content != null ? content.trim() : "";
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Reply content is required");
        }
        validateOwnership(hostId, reviewId);

        HostReviewReplyRow existing = hostReviewReplyMapper.selectReplyByReviewId(reviewId);
        LocalDateTime updatedAt;
        if (existing == null) {
            hostReviewReplyMapper.insertReply(reviewId, hostId, trimmed);
            updatedAt = LocalDateTime.now();
        } else {
            hostReviewReplyMapper.updateReply(existing.getReplyId(), trimmed);
            updatedAt = LocalDateTime.now();
        }
        return new HostReviewReplyResponse(reviewId, trimmed, updatedAt);
    }

    @Override
    public void reportReview(Long hostId, Long reviewId, String reason) {
        String trimmed = reason != null ? reason.trim() : "";
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Report reason is required");
        }
        validateOwnership(hostId, reviewId);

        ReviewReport report = ReviewReport.builder()
                .reviewId(reviewId)
                .userId(hostId)
                .reason(trimmed)
                .state("WAIT")
                .build();
        reviewReportRepository.save(report);
    }

    private void validateOwnership(Long hostId, Long reviewId) {
        Long review = hostReviewMapper.selectReviewIdForHost(hostId, reviewId);
        if (review == null) {
            throw new AccessDeniedException("Not allowed to manage this review");
        }
    }
}
