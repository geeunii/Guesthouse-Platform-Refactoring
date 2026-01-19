package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.dto.CouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    @Cacheable(value = "downloadableCoupons", key = "#accommodationId")
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getDownloadableCoupons(Long accommodationId) {
        // DOWNLOAD 타입이면서, (전체 공개 OR 해당 숙소 전용)인 활성 쿠폰 조회
        List<Coupon> coupons = couponJpaRepository.findDownloadableCoupons(CouponTriggerType.DOWNLOAD, accommodationId);

        return coupons.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private CouponResponseDto toDto(Coupon coupon) {
        CouponResponseDto dto = new CouponResponseDto();
        dto.setCouponId(coupon.getCouponId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue() != null ? coupon.getDiscountValue() : 0);
        dto.setMinPrice(coupon.getMinPrice() != null ? coupon.getMinPrice() : 0);
        dto.setMaxDiscount(coupon.getMaxDiscount() != null ? coupon.getMaxDiscount() : 0);
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidTo(coupon.getValidTo());
        dto.setIsActive(coupon.getIsActive());
        return dto;
    }
}
