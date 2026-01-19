package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponInventoryService {

    private final CouponInventoryRepository couponInventoryRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${coupon.issue.skip-db-finalize:false}")
    private boolean skipDbFinalize;

    @Value("${coupon.issue.async-enabled:true}")
    private boolean asyncEnabled;
    
    private static final String COUPON_STOCK_KEY_PREFIX = "coupon:stock:";

    /**
     * 선착순 제한이 있는 쿠폰이면 하루 수량을 확인하고 1장 차감한다.
     * Redis + DB 하이브리드 전략:
     * 1단계: Redis DECR로 빠른 사전 검증 (99.9% 필터링)
     * 2단계: DB 비관적 락으로 최종 확정 (0.1만 접근)
     */
    @Transactional
    public boolean consumeSlotIfLimited(Long couponId) {
        // 선착순 쿠폰이 아니면 바로 통과
        if (!couponInventoryRepository.existsByCouponId(couponId)) {
            return true;
        }

        // 1단계: Redis 사전 검증 (원자적 연산)
        String redisKey = COUPON_STOCK_KEY_PREFIX + couponId;
        
        try {
            // Redis에서 해당 쿠폰 재고 값을 1 감소
            Long remaining = redisTemplate.opsForValue().decrement(redisKey);
            // 남은 쿠폰이 없거나 쿠폰 남은 수량이 0보다 작다면 -> 100명중 50명은 팅김
            // decrement는 -1 하는건데 쿠폰 개수가 0 -> -1  되면 다시 incremnet로 +1 시킴 ->  쿠폰 0 개
            if (remaining == null || remaining < 0) {
                if (remaining != null && remaining < 0) {
                    redisTemplate.opsForValue().increment(redisKey);
                }
                log.debug("쿠폰 {} Redis 재고 부족: remaining={}", couponId, remaining);
                return false;
            }

            // Redis 통과 여부와 관계없이 skip 설정에 따라 DB 검증 수행
            if (skipDbFinalize) {
                return true;
            }

            // Redis 통과 - 2단계: DB <- 최종 확정 (비동기 모드에서도 수행)
            return couponInventoryRepository.findWithLockByCouponId(couponId)
                    .map(inventory -> {
                        inventory.resetIfNeeded(LocalDate.now());

                        if (!inventory.hasAvailable()) {
                            // DB와 Redis 불일치 발견 - Redis 동기화
                            log.warn("쿠폰 {} Redis-DB 불일치 감지. Redis 0으로 초기화", couponId);
                            redisTemplate.opsForValue().set(redisKey, "0");
                            return false;
                        }

                        // DB에서 차감 성공
                        inventory.consumeOne();
                        log.debug("쿠폰 {} 발급 성공 - DB 재고: {}, Redis 재고: {}",
                                couponId, inventory.getAvailableToday(), remaining);
                        return true;
                    })
                    .orElse(true); // 선착순 제한이 없는 쿠폰
                    
        } catch (Exception e) {
            log.error("Redis 오류 발생. DB 락으로 폴백: couponId={}", couponId, e);
            // Redis 장애 시 기존 DB 락 방식으로 폴백
            return fallbackToDbLock(couponId);
        }
    }

    /**
     * Redis 장애 시 기존 DB 비관적 락 방식으로 폴백
     */
    @Transactional
    public boolean fallbackToDbLock(Long couponId) {
        return couponInventoryRepository.findWithLockByCouponId(couponId)
                .map(inventory -> {
                    inventory.resetIfNeeded(LocalDate.now());
                    if (!inventory.hasAvailable()) {
                        return false;
                    }
                    inventory.consumeOne();
                    return true;
                })
                .orElse(true);
    }

    /**
     * 매일 00시에 모든 선착순 쿠폰 재고를 초기화한다.
     * Redis도 함께 초기화.
     */
    @Transactional
    public int resetDailyInventories() {
        int updatedCount = couponInventoryRepository.resetAllInventories(LocalDate.now());
        
        // Redis 재고도 초기화
        initializeAllRedisStock();
        
        log.info("일일 쿠폰 재고 초기화 완료: {}건, Redis 동기화 완료", updatedCount);
        return updatedCount;
    }

    /**
     * 모든 선착순 쿠폰의 Redis 재고를 DB와 동기화
     * 애플리케이션 시작 시 또는 스케줄러에서 호출
     */
    /**
     * 모든 선착순 쿠폰의 Redis 재고를 DB와 동기화
     * 애플리케이션 시작 시 또는 스케줄러에서 호출
     * [MEDIUM] 확장성 고려: Stream 사용
     */
    @Transactional
    public void initializeAllRedisStock() {
        try (Stream<CouponInventory> stream = couponInventoryRepository.streamAll()) {
            stream.forEach(inventory -> {
                // 필요 시 DB 상태 업데이트 (Dirty Checking)
                inventory.resetIfNeeded(LocalDate.now());
                
                String redisKey = COUPON_STOCK_KEY_PREFIX + inventory.getCouponId();
                redisTemplate.opsForValue().set(redisKey, String.valueOf(inventory.getAvailableToday()));
                
                log.debug("Redis 재고 초기화: couponId={}, stock={}", 
                        inventory.getCouponId(), inventory.getAvailableToday());
            });
        }
        
        log.info("Redis 쿠폰 재고 초기화 완료 (Stream 처리)");
    }

    /**
     * 특정 쿠폰의 Redis 재고를 DB와 동기화
     */
    public void syncRedisStock(Long couponId) {
        couponInventoryRepository.findByCouponId(couponId).ifPresent(inventory -> {
            inventory.resetIfNeeded(LocalDate.now());
            String redisKey = COUPON_STOCK_KEY_PREFIX + couponId;
            redisTemplate.opsForValue().set(redisKey, String.valueOf(inventory.getAvailableToday()));
            log.info("쿠폰 {} Redis 재고 동기화: {}", couponId, inventory.getAvailableToday());
        });
    }

    public void restoreRedisSlot(Long couponId) {
        String redisKey = COUPON_STOCK_KEY_PREFIX + couponId;
        redisTemplate.opsForValue().increment(redisKey);
    }
}
