package com.ssg9th2team.geharbang.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redis 분산 락 어노테이션
 * 
 * 사용 예시:
 * 
 * @DistributedLock(key = "'room:' + #roomId + ':date:' + #checkin")
 *                      public void createReservation(Long roomId, LocalDateTime
 *                      checkin) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키 (SpEL 표현식 지원)
     * 예: "'room:' + #requestDto.roomId()"
     */
    String key();

    /**
     * 락 획득 대기 시간 (기본: 5초)
     */
    long waitTime() default 5L;

    /**
     * 락 유지 시간 (기본: 3초)
     * 이 시간이 지나면 자동으로 락 해제
     */
    long leaseTime() default 3L;

    /**
     * 시간 단위 (기본: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
