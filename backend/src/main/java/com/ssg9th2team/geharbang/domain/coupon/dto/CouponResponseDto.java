package com.ssg9th2team.geharbang.domain.coupon.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Setter
@Getter
public class CouponResponseDto {

    //숙소 상세 페이지에서 "이 숙소에서 받을 수 있는 쿠폰 목록" 보여줄 때
    //관리자 페이지에서 "우리가 만든 쿠폰 종류" 확인할 때
    private Long couponId;
    private String code;
    private String name;
    private String description;
    private String discountType;  // 할인 타입 ( 10%할인, 10000원할인)
    private int discountValue; //  할인 금액 , 할인율
    private int minPrice;
    private int maxDiscount;
    private LocalDateTime validFrom; // 이벤트 기간. 이 쿠폰 이벤트가 언제부터 언제까지 진행되는지 알려줍니다.
    private LocalDateTime validTo; //  이벤트 기간. 이 쿠폰 이벤트가 언제부터 언제까지 진행되는지 알려줍니다.
    private Boolean isActive; // 발급 가능 여부. 지금 이 쿠폰을 다운로드 받을 수 있는 상태인지 표시

}
