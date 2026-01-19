package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.CouponInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface CouponInventoryRepository extends JpaRepository<CouponInventory, Long> {

    /**
     * 쿠폰 ID로 선착순 재고 엔티티 조회.
     */
    Optional<CouponInventory> findByCouponId(Long couponId);



    /**
     * 선착순 차감 시 동시성 제어를 위해 PESSIMISTIC_WRITE로 락을 잡고 조회. -> 비관적락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ci from CouponInventory ci where ci.couponId = :couponId")
    Optional<CouponInventory> findWithLockByCouponId(@Param("couponId") Long couponId);



    // 선착순 쿠폰 재고를 초기화
    @Modifying(clearAutomatically = true)
    @Query("update CouponInventory ci set ci.availableToday = ci.dailyLimit, ci.lastResetDate = :today "
            + "where ci.lastResetDate is null or ci.lastResetDate < :today")
    int resetAllInventories(@Param("today") LocalDate today);


    // [MEDIUM] 확장성 고려: 전체 조회 시 Stream 사용
    @Query("select ci from CouponInventory ci")
    Stream<CouponInventory> streamAll();


    @Query("select ci.couponId from CouponInventory ci")
    List<Long> findAllCouponIds();


    @Modifying(clearAutomatically = true)
    @Query("update CouponInventory ci set ci.availableToday = ci.availableToday - 1 "
            + "where ci.couponId = :couponId and ci.availableToday > 0")
    int decrementAvailable(@Param("couponId") Long couponId);

    // 선착순 쿠폰 여부 확인
    boolean existsByCouponId(Long couponId);
}
