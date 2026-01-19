package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponInventoryRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.UserCouponJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 발급 비동기 처리기
 * Redis 큐에 적재된 쿠폰 발급 요청을 백그라운드에서 DB에 반영한다.
 * 
 * <p>선착순 쿠폰 발급 시 발생하는 DB 병목을 해결하기 위해 도입:
 * <ul>
 *   <li>Redis에서 즉시 확정 → 사용자 응답 빠름</li>
 *   <li>DB 저장은 비동기 배치 처리 → 락 경합 최소화</li>
 *   <li>실패 시 재시도 큐로 분리 → eventual consistency 보장</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueAsyncProcessor {

    private final CouponIssueQueueService couponIssueQueueService;
    private final UserCouponJpaRepository userCouponJpaRepository;
    private final CouponInventoryRepository couponInventoryRepository;
    private final CacheManager cacheManager;

    @Value("${coupon.issue.async-enabled:true}")
    private boolean asyncEnabled;

    @Value("${coupon.issue.async-processor.batch-size:200}")
    private int batchSize;

    /**
     * Redis 큐에서 쿠폰 발급 요청을 가져와 DB에 저장한다.
     * 
     * <p>스케줄러가 일정 주기(기본 200ms)마다 자동 실행하며,
     * 한 번에 최대 batch-size(기본 200)개까지 처리한다.
     * 
     * <p>처리 순서:
     * <ol>
     *   <li>메인 큐(coupon:issue:queue)에서 요청 꺼내기</li>
     *   <li>메인 큐가 비었으면 재시도 큐(coupon:issue:retry)에서 꺼내기</li>
     *   <li>둘 다 비었으면 종료</li>
     * </ol>
     */
    @Scheduled(fixedDelayString = "${coupon.issue.async-processor.delay-ms:200}")
    @Transactional
    public void drainQueue() {
        if (!asyncEnabled) {
            return;
        }
        for (int i = 0; i < batchSize; i++) {
            CouponIssueQueueService.IssueRequest request = couponIssueQueueService.pollIssue();
            if (request == null) {
                request = couponIssueQueueService.pollRetry();
                if (request == null) {
                    return;
                }
            }
            processRequest(request);
        }
    }

    /**
     * 개별 쿠폰 발급 요청을 DB에 저장한다.
     * 
     * <p>처리 단계:
     * <ol>
     *   <li>UserCoupon 엔티티 생성 및 저장</li>
     *   <li>CouponInventory의 잔여 수량 차감</li>
     *   <li>사용자 쿠폰 캐시 무효화 (최신 데이터 조회 보장)</li>
     * </ol>
     * 
     * <p>실패 시 재시도 큐(coupon:issue:retry)에 적재하여
     * 다음 스케줄 주기에 재처리 시도한다.
     * 
     * @param request Redis 큐에서 가져온 발급 요청 (userId, couponId, expiresAt 포함)
     */
    private void processRequest(CouponIssueQueueService.IssueRequest request) {
        try {
            UserCoupon userCoupon = UserCoupon.issue(
                    request.getUserId(),
                    request.getCouponId(),
                    request.getExpiresAt()
            );
            userCouponJpaRepository.save(userCoupon);
            couponInventoryRepository.decrementAvailable(request.getCouponId());
            evictUserCouponCache(request.getUserId(), "ISSUED");
        } catch (Exception ex) {
            log.error("쿠폰 발급 비동기 처리 실패. payload={}", request.getPayload(), ex);
            couponIssueQueueService.enqueueRetry(request.getPayload());
        }
    }

    /**
     * 사용자의 쿠폰 목록 캐시를 무효화한다.
     * 
     * <p>DB에 새 쿠폰이 추가되었으므로, 기존 캐시를 삭제하여
     * 다음 조회 시 최신 데이터를 가져오도록 한다.
     * 
     * @param userId 쿠폰을 발급받은 사용자 ID
     * @param status 무효화할 쿠폰 상태 (예: "ISSUED")
     */
    private void evictUserCouponCache(Long userId, String status) {
        Cache cache = cacheManager.getCache("userCoupons");
        if (cache == null) {
            return;
        }
        cache.evict(userId + "_" + status);
    }
}
