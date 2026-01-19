package com.ssg9th2team.geharbang.global.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 분산 락 AOP
 * 
 * @DistributedLock 어노테이션이 붙은 메서드에 자동으로 Redis 분산 락 적용
 * 
 * @Order(1)로 트랜잭션보다 먼저 실행되어야 함
 *            락 획득 -> 트랜잭션 시작 -> 비즈니스 로직 -> 트랜잭션 커밋 -> 락 해제
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(1) // @Transactional보다 먼저 실행
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "LOCK:";
    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = LOCK_PREFIX + parseKey(joinPoint, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        log.debug("분산 락 획득 시도: {}", lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit());

            if (!acquired) {
                log.warn("분산 락 획득 실패: {} (대기 시간 초과)", lockKey);
                throw new LockAcquisitionException(
                        "다른 사용자가 예약 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.debug("분산 락 획득 성공: {}", lockKey);
            return joinPoint.proceed();

        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("분산 락 해제: {}", lockKey);
            }
        }
    }

    /**
     * SpEL 표현식을 파싱하여 실제 키 값으로 변환
     */
    private String parseKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
