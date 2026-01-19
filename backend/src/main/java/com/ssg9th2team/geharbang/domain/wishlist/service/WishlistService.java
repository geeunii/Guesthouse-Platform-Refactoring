package com.ssg9th2team.geharbang.domain.wishlist.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.wishlist.dto.WishlistDto;
import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;

import java.util.List;

public interface WishlistService {


    // 위시리스트 추가
    void addWishlist(Long userId, Long accommodationId);

    // 위시리스트 삭제 (특정 숙소 취소)
    void removeWishlist(Long userId, Long accommodationId);

    // 내 위시리스트 조회 (마이페이지용)
    List<AccommodationResponseDto> getMyWishlist(Long userId);

    // 위시리스트 조회 (메인페이지용)
    List<Long> getMyWishlistAccommodationIds(Long userId);



}
