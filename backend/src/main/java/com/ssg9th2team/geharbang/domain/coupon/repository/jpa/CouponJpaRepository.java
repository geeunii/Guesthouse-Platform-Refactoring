package com.ssg9th2team.geharbang.domain.coupon.repository.jpa;

import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {


    // 트리거 타입으로 활성화된 쿠폰 조회
    Optional<Coupon> findByTriggerTypeAndIsActiveTrue(CouponTriggerType triggerType);

    // 다운로드 가능 쿠폰 조회 (특정 숙소 전용 OR 전체 숙소용)
    @Query("SELECT c FROM Coupon c WHERE c.triggerType = :triggerType AND c.isActive = true AND (c.accommodation.accommodationsId = :accommodationId OR c.accommodation IS NULL)")
    List<Coupon> findDownloadableCoupons(@Param("triggerType") CouponTriggerType triggerType, @org.springframework.data.repository.query.Param("accommodationId") Long accommodationId);
}
