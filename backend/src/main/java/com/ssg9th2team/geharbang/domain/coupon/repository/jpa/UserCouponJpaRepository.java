package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {


    // 중복 쿠폰 발급 체크 ( 같은 쿠폰 또 주면 안됨)
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 만료 대상 쿠폰 조회 (ISSUED 상태이면서 만료일이 지난 쿠폰)
    List<UserCoupon> findByStatusAndExpiredAtBefore(UserCouponStatus status, LocalDateTime now);

    List<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status);

    void deleteAllByUserId(Long userId);

    @Query("select uc.couponId from UserCoupon uc where uc.userId = :userId")
    Set<Long> findCouponIdsByUserId(@Param("userId") Long userId);

    // [HIGH] 성능 최적화: couponId로 직접 조회 (전체 조회 후 필터링 X)
    List<UserCoupon> findAllByCouponId(Long couponId);

    // [HIGH] OOM 방지: 전체 조회 시 Stream 사용
    @Query("select uc from UserCoupon uc")
    Stream<UserCoupon> streamAll();

    /**
     * 일일 선착순 쿠폰 재설정 시 사용
     * 특정 쿠폰 ID로 발급된 모든 쿠폰을 삭제한다.
     *
     * @param couponId 삭제할 쿠폰 ID
     * @return 삭제된 행 수
     */
    @Modifying
    @Transactional
    @Query("delete from UserCoupon uc where uc.couponId = :couponId")
    int deleteByCouponId(@Param("couponId") Long couponId);

    /**
     * 일일 선착순 쿠폰 재설정 시 사용 (Bulk Delete)
     * 특정 쿠폰 ID 리스트로 발급된 모든 쿠폰을 한 번에 삭제한다.
     *
     * @param couponIds 삭제할 쿠폰 ID 리스트
     * @return 삭제된 행 수
     */
    @Modifying
    @Transactional
    @Query("delete from UserCoupon uc where uc.couponId in :couponIds")
    int deleteByCouponIds(@Param("couponIds") List<Long> couponIds);

    /**
     * N+1 쿼리 방지를 위한 JOIN 쿼리
     * 사용자 ID, 상태, 트리거 타입으로 쿠폰 조회
     *
     * @param userId      사용자 ID
     * @param status      쿠폰 상태
     * @param triggerType 트리거 타입
     * @return 조건에 맞는 UserCoupon 목록
     */
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.status = :status AND uc.couponId IN " +
           "(SELECT c.couponId FROM Coupon c WHERE c.triggerType = :triggerType)")
    List<UserCoupon> findUserCouponsByTriggerType(
            @Param("userId") Long userId,
            @Param("status") UserCouponStatus status,
            @Param("triggerType") com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType triggerType
    );
}
