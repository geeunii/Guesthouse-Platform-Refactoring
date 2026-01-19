package com.ssg9th2team.geharbang.domain.wishlist.controller;

import com.ssg9th2team.geharbang.global.common.annotation.CurrentUser;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.wishlist.dto.WishlistDto;
import com.ssg9th2team.geharbang.domain.wishlist.service.WishlistService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 위시리스트 추가
    @PostMapping
    public ResponseEntity<?> addWishlist(@RequestBody WishlistDto wishlistDto, @CurrentUser User user) {
        System.out.println("[위시리스트 추가] 요청 받음 - accommodationsId: " + 
            (wishlistDto != null ? wishlistDto.getAccommodationsId() : "null"));
        
        if (user == null) {
            System.out.println("[위시리스트 추가] 실패 - 사용자 정보 없음 (user is null)");
            return ResponseEntity.status(401).build();
        }
        
        System.out.println("[위시리스트 추가] 사용자 정보 확인됨 - userId: " + user.getId() + 
            ", email: " + user.getEmail());
        
        try {
            wishlistService.addWishlist(user.getId(), wishlistDto.getAccommodationsId());
            System.out.println("[위시리스트 추가] 성공 - userId: " + user.getId() + 
                ", accommodationsId: " + wishlistDto.getAccommodationsId());
            return ResponseEntity.ok("Wishlist added.");
        } catch (Exception e) {
            System.err.println("[위시리스트 추가] 에러 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to add wishlist: " + e.getMessage());
        }
    }

    // 위시리스트 삭제
    @DeleteMapping("/{accommodationId}")
    public ResponseEntity<?> removeWishlist(@PathVariable Long accommodationId, @CurrentUser User user) {
        System.out.println("[위시리스트 삭제] 요청 받음 - accommodationId: " + accommodationId);
        
        if (user == null) {
            System.out.println("[위시리스트 삭제] 실패 - 사용자 정보 없음 (user is null)");
            return ResponseEntity.status(401).build();
        }
        
        System.out.println("[위시리스트 삭제] 사용자 정보 확인됨 - userId: " + user.getId());
        
        try {
            wishlistService.removeWishlist(user.getId(), accommodationId);
            System.out.println("[위시리스트 삭제] 성공 - userId: " + user.getId() + 
                ", accommodationId: " + accommodationId);
            return ResponseEntity.ok("Wishlist removed.");
        } catch (Exception e) {
            System.err.println("[위시리스트 삭제] 에러 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to remove wishlist: " + e.getMessage());
        }
    }

    // 내 위시리스트 조회 (마이페이지용 - 상세 정보 포함)
    @GetMapping
    public ResponseEntity<?> getMyWishlist(@CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(wishlistService.getMyWishlist(user.getId()));
    }

    // 내 위시리스트 ID 조회 (메인페이지용 - 하트 표시용)
    @GetMapping("/accommodation-ids")
    public ResponseEntity<?> getMyWishlistIds(@CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        return ResponseEntity.ok(wishlistService.getMyWishlistAccommodationIds(user.getId()));
    }
}
