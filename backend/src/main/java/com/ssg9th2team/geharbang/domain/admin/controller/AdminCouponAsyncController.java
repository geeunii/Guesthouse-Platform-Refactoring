package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import com.ssg9th2team.geharbang.domain.coupon.service.CouponIssueQueueService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons/async")
@RequiredArgsConstructor
public class AdminCouponAsyncController {

    private final CouponIssueQueueService couponIssueQueueService;

    @Value("${coupon.issue.async-enabled:true}")
    private boolean asyncEnabled;

    @Value("${coupon.issue.async-processor.batch-size:200}")
    private int batchSize;

    @Value("${coupon.issue.async-processor.delay-ms:200}")
    private long delayMs;

    @GetMapping("/queues")
    public QueueStatusResponse getQueueStatus(@AdminId Long adminId) {
        return new QueueStatusResponse(
                asyncEnabled,
                batchSize,
                delayMs,
                couponIssueQueueService.getQueueSize(),
                couponIssueQueueService.getRetrySize()
        );
    }

    @PostMapping("/retry/requeue")
    public RequeueResponse requeueRetry(
            @AdminId Long adminId,
            @RequestParam(defaultValue = "1000") int limit
    ) {
        long moved = couponIssueQueueService.requeueRetry(limit);
        return new RequeueResponse(
                moved,
                couponIssueQueueService.getQueueSize(),
                couponIssueQueueService.getRetrySize()
        );
    }

    @DeleteMapping("/retry")
    public QueueSizeResponse clearRetry(@AdminId Long adminId) {
        couponIssueQueueService.clearRetryQueue();
        return new QueueSizeResponse(
                couponIssueQueueService.getQueueSize(),
                couponIssueQueueService.getRetrySize()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class QueueStatusResponse {
        private final boolean asyncEnabled;
        private final int batchSize;
        private final long delayMs;
        private final long queueSize;
        private final long retrySize;
    }

    @Getter
    @AllArgsConstructor
    public static class RequeueResponse {
        private final long moved;
        private final long queueSize;
        private final long retrySize;
    }

    @Getter
    @AllArgsConstructor
    public static class QueueSizeResponse {
        private final long queueSize;
        private final long retrySize;
    }
}
