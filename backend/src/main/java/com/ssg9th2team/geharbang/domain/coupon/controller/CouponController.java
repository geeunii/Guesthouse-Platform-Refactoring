package com.ssg9th2team.geharbang.domain.coupon.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.coupon.dto.CouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.dto.UserCouponResponseDto;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final com.ssg9th2team.geharbang.domain.coupon.service.CouponService couponService;
    private final UserCouponService userCouponService;
    private final UserRepository userRepository;

    // 숙소 상세페이지에서 다운로드 가능한 쿠폰 목록 조회
    @GetMapping("/accommodation/{accommodationId}")
    public ResponseEntity<List<CouponResponseDto>> getDownloadableCoupons(
            @PathVariable Long accommodationId) {
        
        List<CouponResponseDto> coupons = couponService.getDownloadableCoupons(accommodationId);
        return ResponseEntity.ok(coupons);
    }



    // 상태별 내 쿠폰 조회 (ISSUED / USED / EXPIRED)
    @GetMapping("/my")
    public ResponseEntity<List<UserCouponResponseDto>> getMyCoupons(
            Authentication authentication,
            @RequestParam(defaultValue = "ISSUED") String status) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<UserCouponResponseDto> coupons = userCouponService.getMyCouponsByStatus(user.getId(), status);
        return ResponseEntity.ok(coupons);
    }

    // 쿠폰 수동 발급
    @PostMapping("/issue")
    public ResponseEntity<String> issueCoupon(
            Authentication authentication,
            @RequestParam Long couponId) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        userCouponService.issueCoupon(user.getId(), couponId);
        return ResponseEntity.ok("쿠폰이 발급되었습니다");
    }


    // 쿠폰 사용 처리
    @PostMapping("/{userCouponId}/use")
    public ResponseEntity<String> useCoupon(
            Authentication authentication,
            @PathVariable Long userCouponId) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        userCouponService.useCoupon(user.getId(), userCouponId);
        return ResponseEntity.ok("쿠폰이 사용되었습니다");
    }



    @GetMapping("/my/ids")
    public ResponseEntity<List<Long>> getMyCouponIds(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        return ResponseEntity.ok(new ArrayList<>(userCouponService.getMyCouponIds(user.getId())));
    }
}
