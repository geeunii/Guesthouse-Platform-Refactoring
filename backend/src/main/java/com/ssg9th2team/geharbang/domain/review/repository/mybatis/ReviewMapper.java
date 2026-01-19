package com.ssg9th2team.geharbang.domain.review.repository.mybatis;

import com.ssg9th2team.geharbang.domain.review.dto.ReviewImageDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {

    // 특정 숙소에 달린 ‘모든 리뷰 목록’을 조회
    List<ReviewResponseDto> selectReviewsByAccommodationId(@Param("userId") Long userId,
                                                           @Param("accommodationsId") Long accommodationsId);

    // [User] 내가 작성한 모든 리뷰 목록 조회
    List<ReviewResponseDto> selectReviewsByUserId(@Param("userId") Long userId);


    // 리뷰 태그 저장 (review_tag_map)
    void insertReviewTags(@Param("reviewId")
                          Long reviewId,
                          @Param("tagIds")
                          List<Long> tagIds);

    // 전체 리뷰 태그 목록 조회 (활성화된 태그만)
    List<ReviewTagDto> selectAllReviewTags();

    // 리뷰 태그 삭제 (review_tag_map)
    void deleteReviewTags(@Param("reviewId") Long reviewId);

    // 숙소별 상위 3개 태그 조회
    List<String> selectTop3TagsByAccommodationId(@Param("accommodationId") Long accommodationId);
}
