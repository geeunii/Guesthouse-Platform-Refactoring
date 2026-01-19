# 🔒 동시성 제어 전략 (Concurrency Control Strategy)

## 1. 현재 구현: Redis 분산 락 (Distributed Lock)

> **2026-01-09 업데이트**: 기존 비관적 락에서 **Redis 분산 락(Redisson)**으로 전환하여 더욱 안정적인 동시성 제어를 구현했습니다.

### � 락이 걸리는 시점
**예약 생성 API(`POST /api/reservations`)가 호출될 때** 락이 걸립니다.

사용자 플로우:
1. 숙소 상세 페이지에서 날짜/인원 선택
2. 결제 페이지로 이동 (이 시점에는 락 없음)
3. 예약자 정보 입력
4. **"예약하기" 버튼 클릭 → 이 시점에 분산 락 획득 시도**
5. 락 획득 성공 시 예약 생성, 실패 시 409 Conflict 응답

### 🛠 구현 코드

```java
// 분산 락 어노테이션 적용 - 객실+날짜 기준으로 락
@Override
@DistributedLock(key = "'reservation:room:' + #requestDto.roomId() + ':date:' + #requestDto.checkin().toString().substring(0,10)")
@Transactional
public ReservationResponseDto createReservation(ReservationRequestDto requestDto) {
    // 락 획득 후 예약 로직 실행
    ...
}
```

```java
// DistributedLockAspect.java - AOP로 락 처리
@Around("@annotation(distributedLock)")
public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
    String lockKey = generateLockKey(...);
    RLock lock = redissonClient.getLock(lockKey);
    
    boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
    if (!acquired) {
        throw new LockAcquisitionException("다른 사용자가 예약 중입니다. 잠시 후 다시 시도해주세요.");
    }
    
    try {
        return joinPoint.proceed();
    } finally {
        lock.unlock();
    }
}
```

### 📊 락 키 구조
```
reservation:room:{roomId}:date:{checkinDate}
예시: reservation:room:230:date:2026-01-20
```

동일한 객실 + 같은 체크인 날짜에 대한 동시 요청만 직렬화됩니다.

---

## 2. 락 기법 비교

### � 비관적 락 (Pessimistic Lock) - **이전 방식**
**\"충돌이 발생할 것이라고 비관적으로 가정하고, 데이터를 조회할 때부터 문을 잠근다\"**

*   **동작 방식**: 트랜잭션이 시작될 때 DB에 락(Lock)을 걸어 다른 트랜잭션이 접근하지 못하게 막습니다.
*   **문제점**: MySQL `REPEATABLE READ` 격리 수준에서 동시에 실행되는 트랜잭션들은 서로의 INSERT를 볼 수 없어 초과 예약이 발생함.

### 🟢 낙관적 락 (Optimistic Lock)
**\"충돌이 발생하지 않을 것이라고 낙관적으로 가정하고, 마지막에 검사한다\"**

*   **동작 방식**: 버전(Version) 컬럼을 이용해 수정 시 버전 충돌 여부 확인.
*   **단점**: 충돌 빈번 시 재시도 비용 증가.

### 🔵 분산 락 (Distributed Lock) - **현재 방식** ✅
**\"Redis를 이용해 애플리케이션 레벨에서 락을 관리한다\"**

*   **동작 방식**: Redisson 라이브러리로 Redis에 락 키를 생성하여 동시 접근 제어.
*   **장점**:
    *   DB와 무관하게 독립적인 락 관리
    *   다중 서버 환경에서도 동작 (수평 확장 가능)
    *   락 획득 대기 시간/자동 해제 시간 설정 가능
*   **구현**: `Redisson` 라이브러리 + `@DistributedLock` 커스텀 어노테이션

---

## 3. 왜 Redis 분산 락을 선택했는가?

### ✅ 이유 1: DB 비관적 락의 한계 극복
MySQL의 `REPEATABLE READ` 격리 수준에서는 동시에 실행되는 트랜잭션들이 서로의 미커밋 INSERT를 볼 수 없습니다. 
이로 인해 정원 체크 시점에 모든 트랜잭션이 "정원 여유 있음"으로 판단하여 초과 예약이 발생했습니다.

```
[기존 비관적 락 테스트 결과]
- 10명 동시 요청 → 10명 전원 예약 성공 (버그!)

[Redis 분산 락 테스트 결과]
- 10명 동시 요청 → 6명 성공, 4명 실패 (정상!)
```

### ✅ 이유 2: 다중 서버 환경 대응
DB 레벨 락은 단일 서버에서만 유효합니다. Redis 분산 락은 여러 애플리케이션 서버에서도 동일하게 동작합니다.

### ✅ 이유 3: 명확한 실패 응답
락 획득 실패 시 **HTTP 409 Conflict**로 명확하게 응답하여, 클라이언트가 적절한 재시도 로직을 구현할 수 있습니다.

---

## 4. 설정 값

| 설정 | 값 | 설명 |
|------|------|------|
| waitTime | 3초 | 락 획득 대기 시간 |
| leaseTime | 10초 | 락 자동 해제 시간 |
| 커넥션 풀 최소 유휴 | 10 | Redis 최소 유휴 커넥션 |
| 커넥션 풀 최대 크기 | 50 | Redis 최대 커넥션 풀 |

```properties
# application.properties에서 조정 가능
redisson.connection.minimum-idle-size=10
redisson.connection.pool-size=50
```

---

## 5. 요약

> **\"속도보다는 정확성(Consistency)과 신뢰성(Reliability)이 중요한 숙소 예약 도메인의 특성상, Redis 분산 락을 도입하여 DB 레벨 락의 한계를 극복하고 다중 서버 환경에서도 안정적인 동시성 제어를 구현했습니다.\"**
