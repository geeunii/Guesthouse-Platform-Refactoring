package com.ssg9th2team.geharbang.domain.coupon.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserCouponResponseDto {

    // 특정 유저가 가지고 있는 쿠폰의 상태를 보여주는 정보
    // 마이페이지 "내 쿠폰함"
    // 결제 페이지 "쿠폰 선택"

    // user_coupon 테이블
    private Long id;
    private String status;        // ISSUED / USED / EXPIRED
    private LocalDateTime issuedAt;  // 발급일시
    private LocalDateTime usedAt;    // 사용일시
    private LocalDateTime expiredAt; // 만료일시

    // coupon 테이블 (JOIN)
    private Long couponId;
    private String code;
    private String name;
    private String discountType;
    private int discountValue;
    private int minPrice;
    private Integer maxDiscount;
    private Long accommodationsId;

}
