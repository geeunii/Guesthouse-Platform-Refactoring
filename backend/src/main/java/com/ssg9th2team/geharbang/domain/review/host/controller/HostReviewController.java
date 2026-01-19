package com.ssg9th2team.geharbang.domain.review.host.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.global.common.annotation.CurrentUser;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReplyRequest;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReplyResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewReportRequest;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewResponse;
import com.ssg9th2team.geharbang.domain.review.host.dto.HostReviewSummaryResponse;
import com.ssg9th2team.geharbang.domain.review.host.service.HostReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/host/reviews")
@RequiredArgsConstructor
public class HostReviewController {

    private final HostReviewService hostReviewService;

    @GetMapping("/summary")
    public HostReviewSummaryResponse summary(@CurrentUser User user) {
        if (user == null) {
            throw new com.ssg9th2team.geharbang.global.exception.AccessDeniedException("User not authenticated");
        }
        return hostReviewService.getSummary(user.getId());
    }

    @GetMapping
    public List<HostReviewResponse> list(
            @RequestParam(required = false) Long accommodationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @CurrentUser User user) {
        if (user == null) {
            throw new com.ssg9th2team.geharbang.global.exception.AccessDeniedException("User not authenticated");
        }
        return hostReviewService.getReviews(user.getId(), accommodationId, page, size, sort);
    }

    @PostMapping("/{reviewId}/reply")
    public HostReviewReplyResponse upsertReply(
            @PathVariable Long reviewId,
            @RequestBody HostReviewReplyRequest request,
            @CurrentUser User user) {
        if (user == null) {
            throw new com.ssg9th2team.geharbang.global.exception.AccessDeniedException("User not authenticated");
        }
        return hostReviewService.upsertReply(user.getId(), reviewId, request != null ? request.getContent() : null);
    }

    @PostMapping("/{reviewId}/report")
    public void reportReview(
            @PathVariable Long reviewId,
            @RequestBody HostReviewReportRequest request,
            @CurrentUser User user) {
        if (user == null) {
            throw new com.ssg9th2team.geharbang.global.exception.AccessDeniedException("User not authenticated");
        }
        hostReviewService.reportReview(user.getId(), reviewId, request != null ? request.getReason() : null);
    }
}
