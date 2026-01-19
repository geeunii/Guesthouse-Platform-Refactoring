package com.ssg9th2team.geharbang.domain.coupon.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CouponCreateDto {
    private String code;           // 쿠폰 코드 (REVIEW_3_REWARD 등)
    private String name;           // 쿠폰 이름
    private String description;    // 설명
    private String discountType;   // AMOUNT / PERCENT
    private int discountValue;     // 할인값
    private int minPrice;          // 최소 주문 금액
    private Integer maxDiscount;   // 최대 할인 (정율일 때)
    private LocalDateTime validFrom; // 쿠폰 유효시작 일시
    private LocalDateTime validTo;   // 쿠폰 종료 일시
}

