package com.ssg9th2team.geharbang.domain.coupon.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 쿠폰 발급 요청 큐 관리 서비스
 * Redis List를 활용하여 쿠폰 발급 요청을 큐에 적재하고, 비동기 처리기가 꺼내갈 수 있도록 한다.
 * 
 * <p>큐 구조:
 * <ul>
 *   <li>메인 큐(coupon:issue:queue): 신규 발급 요청을 적재</li>
 *   <li>재시도 큐(coupon:issue:retry): 처리 실패 시 재시도 대상을 적재</li>
 * </ul>
 * 
 * <p>Redis List 연산:
 * <ul>
 *   <li>LPUSH: 큐의 왼쪽(머리)에 추가</li>
 *   <li>RPOP: 큐의 오른쪽(꼬리)에서 꺼내기 → FIFO 구조</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CouponIssueQueueService {

    private static final String QUEUE_KEY = "coupon:issue:queue";
    private static final String RETRY_KEY = "coupon:issue:retry";
    private static final String DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;

    /**
     * 쿠폰 발급 요청을 메인 큐에 적재한다.
     * 
     * <p>Redis의 즉시 응답 특성을 활용하여, 사용자에게 빠른 응답을 제공하고
     * DB 저장은 비동기 처리기가 나중에 수행하도록 한다.
     * 
     * @param userId 쿠폰을 발급받을 사용자 ID
     * @param couponId 발급할 쿠폰 ID
     * @param expiresAt 쿠폰 만료 일시
     * @return 큐 적재 성공 여부
     */
    public boolean enqueueIssue(Long userId, Long couponId, LocalDateTime expiresAt) {
        String payload = encode(userId, couponId, expiresAt);
        return redisTemplate.opsForList().leftPush(QUEUE_KEY, payload) != null;
    }

    /**
     * 메인 큐에서 쿠폰 발급 요청을 하나 꺼낸다 (FIFO).
     * 
     * <p>비동기 처리기(CouponIssueAsyncProcessor)가 주기적으로 호출하여
     * 큐에 쌓인 요청을 순차적으로 처리한다.
     * 
     * @return 큐에서 꺼낸 발급 요청 (큐가 비었으면 null)
     */
    public IssueRequest pollIssue() {
        String payload = redisTemplate.opsForList().rightPop(QUEUE_KEY);
        if (payload == null) {
            return null;
        }
        return decode(payload);
    }

    /**
     * 재시도 큐에서 쿠폰 발급 요청을 하나 꺼낸다 (FIFO).
     * 
     * <p>메인 큐가 비었을 때 호출되며, 이전에 실패했던 요청을 재처리한다.
     * 
     * @return 큐에서 꺼낸 재시도 요청 (큐가 비었으면 null)
     */
    public IssueRequest pollRetry() {
        String payload = redisTemplate.opsForList().rightPop(RETRY_KEY);
        if (payload == null) {
            return null;
        }
        return decode(payload);
    }

    /**
     * 메인 큐에 쌓여있는 요청 개수를 조회한다.
     * 
     * <p>모니터링 및 디버깅 용도로 사용하며, 큐가 과도하게 밀렸는지 확인할 수 있다.
     * 
     * @return 메인 큐의 크기
     */
    public long getQueueSize() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size == null ? 0L : size;
    }

    /**
     * 재시도 큐에 쌓여있는 요청 개수를 조회한다.
     * 
     * <p>모니터링 용도로 사용하며, 실패 건수가 계속 증가하는지 확인할 수 있다.
     * 
     * @return 재시도 큐의 크기
     */
    public long getRetrySize() {
        Long size = redisTemplate.opsForList().size(RETRY_KEY);
        return size == null ? 0L : size;
    }

    /**
     * 재시도 큐의 요청을 메인 큐로 다시 옮긴다.
     * 
     * <p>재시도 큐에 쌓인 실패 건들을 수동으로 재처리하고 싶을 때 호출한다.
     * 예: 일시적인 DB 장애 복구 후, 재시도 큐를 비우기 위해 사용.
     * 
     * @param maxItems 최대 몇 개까지 옮길지 (전체를 옮기려면 큰 값 지정)
     * @return 실제로 옮긴 개수
     */
    public long requeueRetry(int maxItems) {
        long moved = 0;
        for (int i = 0; i < maxItems; i++) {
            String payload = redisTemplate.opsForList().rightPop(RETRY_KEY);
            if (payload == null) {
                break;
            }
            redisTemplate.opsForList().leftPush(QUEUE_KEY, payload);
            moved++;
        }
        return moved;
    }

    /**
     * 재시도 큐를 완전히 비운다.
     * 
     * <p>장애 상황에서 축적된 실패 건들을 일괄 삭제할 때 사용한다.
     * 주의: 이 작업은 되돌릴 수 없으므로 신중하게 사용해야 한다.
     */
    public void clearRetryQueue() {
        redisTemplate.delete(RETRY_KEY);
    }

    /**
     * 처리 실패한 요청을 재시도 큐에 적재한다.
     * 
     * <p>비동기 처리기에서 DB 저장 중 예외가 발생하면 호출되며,
     * 나중에 다시 처리될 수 있도록 재시도 큐로 분리한다.
     * 
     * @param payload 실패한 요청의 직렬화된 문자열 (userId|couponId|expiresAt 형식)
     */
    public void enqueueRetry(String payload) {
        redisTemplate.opsForList().leftPush(RETRY_KEY, payload);
    }

    /**
     * 쿠폰 발급 정보를 문자열로 직렬화한다.
     * 
     * <p>Redis에는 단순 문자열 형태로 저장하기 위해 파이프(|) 구분자로 연결한다.
     * 예: "123|45|2026-01-12T17:00:00"
     * 
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @param expiresAt 만료 일시
     * @return 직렬화된 문자열
     */
    public static String encode(Long userId, Long couponId, LocalDateTime expiresAt) {
        return userId + DELIMITER + couponId + DELIMITER + expiresAt;
    }

    /**
     * 직렬화된 문자열을 쿠폰 발급 요청 객체로 역직렬화한다.
     * 
     * <p>Redis에서 꺼낸 문자열을 파싱하여 원래 데이터로 복원한다.
     * 
     * @param payload 직렬화된 문자열 (예: "123|45|2026-01-12T17:00:00")
     * @return 역직렬화된 발급 요청 객체
     * @throws IllegalArgumentException payload 형식이 잘못된 경우
     */
    public static IssueRequest decode(String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid payload format: " + payload);
        }
        Long userId = Long.valueOf(parts[0]);
        Long couponId = Long.valueOf(parts[1]);
        LocalDateTime expiresAt = LocalDateTime.parse(parts[2]);
        return new IssueRequest(userId, couponId, expiresAt, payload);
    }

    /**
     * 쿠폰 발급 요청 데이터를 담는 불변 객체
     */
    public static class IssueRequest {
        private final Long userId;
        private final Long couponId;
        private final LocalDateTime expiresAt;
        private final String payload;

        public IssueRequest(Long userId, Long couponId, LocalDateTime expiresAt, String payload) {
            this.userId = userId;
            this.couponId = couponId;
            this.expiresAt = expiresAt;
            this.payload = payload;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getCouponId() {
            return couponId;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public String getPayload() {
            return payload;
        }
    }
}
