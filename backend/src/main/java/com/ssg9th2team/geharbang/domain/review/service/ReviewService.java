package com.ssg9th2team.geharbang.domain.review.service;

import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewUpdateDto;

import java.util.List;

public interface ReviewService {

    // 리뷰 등록 (쿠폰 발급 여부 반환)
    boolean createReview(Long userId, ReviewCreateDto reviewCreateDto);

    // 리뷰 수정
    void updateReview(Long userId, Long reviewId, ReviewUpdateDto reviewUpdateDto);

    void deleteReview(Long userId, Long reviewId);


    // 숙소별 리뷰 조회
    List<ReviewResponseDto> getReviewsByAccommodation(Long userId, Long accommodationsId);

    // [User] 내가 작성한 리뷰 조회
    List<ReviewResponseDto> getReviewsByUserId(Long userId);

    // 전체 태그 목록 조회
    List<ReviewTagDto> getAllReviewTags();
}
