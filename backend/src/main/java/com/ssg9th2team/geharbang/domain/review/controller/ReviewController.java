package com.ssg9th2team.geharbang.domain.review.controller;

import com.ssg9th2team.geharbang.global.common.annotation.CurrentUser;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewUpdateDto;
import com.ssg9th2team.geharbang.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 등록
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(
            @CurrentUser User user,
            @RequestBody ReviewCreateDto reviewCreateDto) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        boolean couponIssued = reviewService.createReview(user.getId(), reviewCreateDto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "리뷰가 등록되었습니다");
        response.put("couponIssued", couponIssued);

        return ResponseEntity.ok(response);
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview( // HTTP 응답 본문(body)에 String 타입 데이터를 담아서 보내겠다
            @CurrentUser User user,
            @PathVariable Long reviewId, // url에 리뷰 아이디 /api/reviews/3
            @RequestBody ReviewUpdateDto reviewUpdateDto) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        reviewService.updateReview(user.getId(), reviewId, reviewUpdateDto);

        return ResponseEntity.ok("리뷰가 수정되었습니다");
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @CurrentUser User user,
            @PathVariable Long reviewId) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        reviewService.deleteReview(user.getId(), reviewId);

        return ResponseEntity.ok("리뷰가 삭제되었습니다");
    }

    // 숙소별 리뷰 조회
    @GetMapping("/accommodations/{accommodationsId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByAccommodation(
            @CurrentUser User user,
            @PathVariable Long accommodationsId) {

        Long userId = (user != null) ? user.getId() : null;

        List<ReviewResponseDto> reviews = reviewService.getReviewsByAccommodation(userId, accommodationsId);

        return ResponseEntity.ok(reviews);
    }

    // 내 리뷰 조회
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(@CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<ReviewResponseDto> reviews = reviewService.getReviewsByUserId(user.getId());
        return ResponseEntity.ok(reviews);
    }

    // 전체 태그 목록 조회
    @GetMapping("/tags")
    public ResponseEntity<List<ReviewTagDto>> getAllReviewTags() {
        List<ReviewTagDto> tags = reviewService.getAllReviewTags();
        return ResponseEntity.ok(tags);
    }
}
