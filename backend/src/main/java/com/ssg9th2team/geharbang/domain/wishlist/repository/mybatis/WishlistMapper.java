package com.ssg9th2team.geharbang.domain.wishlist.repository.mybatis;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WishlistMapper {

  // (마이페이지용 - 상세 정보 포함) 마이페이지에서 내가 찜한 숙소 이름, 사진, 가격 등을 화면에 뿌려줄 때 사용
    List<AccommodationResponseDto> selectWishlistByUserId(Long userId);

    // 메인페이지용 : 메인/검색 페이지에서 각 숙소 카드의 하트를 빨간색으로 색칠할지 판단하기 위한 ID 목록 추출
    List<Long> selectWishlistAccommodationIds(Long userId);

    // 호스트가 숙소 정보 삭제 -> 사용자가 찜한 숙소위시리스트에서 삭제
    void deleteWishlistByAccommodationId(Long accommodationsId);

    // 여러 숙소 일괄 삭제
    void deleteWishlistByAccommodationIdIn(@Param("accommodationIds") List<Long> accommodationIds);
}
