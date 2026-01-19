package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.dto.CouponResponseDto;
import java.util.List;

public interface CouponService {

    /**
     * 특정 숙소에서 다운로드 가능한 쿠폰 목록 조회
     * - 숙소 지정 쿠폰 (accommodationsId 일치)
     * - 전체 공용 쿠폰 (accommodationsId is null)
     * - TriggerType이 DOWNLOAD인 경우만
     * - Active 상태인 경우만
     */
    List<CouponResponseDto> getDownloadableCoupons(Long accommodationId);
}
