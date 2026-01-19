package com.ssg9th2team.geharbang.domain.wishlist.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;
import com.ssg9th2team.geharbang.domain.wishlist.repository.jpa.WishlistJpaRepository;
import com.ssg9th2team.geharbang.domain.wishlist.repository.mybatis.WishlistMapper;
import com.ssg9th2team.geharbang.global.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistMapper wishlistMapper;
    private final WishlistJpaRepository wishlistJpaRepository;

    // 메인페이지에서 하트 모양 클릭 -> 하트 모양 활성화
    @Override
    @Transactional
    public void addWishlist(Long userId, Long accommodationId) {
        // 중복 검증
        boolean exists = wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, accommodationId);
        if (exists) {
            throw new DuplicateResourceException("Wishlist already exists!");
        }

        Wishlist wishlist = Wishlist.builder()
                .userId(userId)
                .accommodationsId(accommodationId)
                .build();

        wishlistJpaRepository.save(wishlist);
    }

    // 위시리스트 삭제 (특정 숙소 취소) -> 하트 모양 비활성화
    @Override
    @Transactional
    public void removeWishlist(Long userId, Long accommodationId) {
        wishlistJpaRepository.deleteByUserIdAndAccommodationsId(userId, accommodationId);
    }

    // (마이페이지용 - 상세 정보 포함)
    @Override
    @Transactional(readOnly = true)
    public List<AccommodationResponseDto> getMyWishlist(Long userId) {
        return wishlistMapper.selectWishlistByUserId(userId);
    }

    // 위시리스트 조회 (메인페이지용) -> 하트 색상 표시용 조회
    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyWishlistAccommodationIds(Long userId) {
        return wishlistMapper.selectWishlistAccommodationIds(userId);
    }
}
