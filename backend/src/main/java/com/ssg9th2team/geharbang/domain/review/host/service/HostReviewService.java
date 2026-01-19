package com.ssg9th2team.geharbang.domain.review.host.service;

import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReplyResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewSummaryResponse;

import java.util.List;

public interface HostReviewService {
    HostReviewSummaryResponse getSummary(Long hostId);

    List<HostReviewResponse> getReviews(Long hostId, Long accommodationId, int page, int size, String sort);

    HostReviewReplyResponse upsertReply(Long hostId, Long reviewId, String content);

    void reportReview(Long hostId, Long reviewId, String reason);
}
