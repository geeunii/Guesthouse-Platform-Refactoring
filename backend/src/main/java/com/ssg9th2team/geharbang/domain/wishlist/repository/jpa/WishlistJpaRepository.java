package com.ssg9th2team.geharbang.domain.wishlist.repository.jpa;

import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistJpaRepository extends JpaRepository<Wishlist, Long> {

    // 위시리스트 저장
    // save()

    // 중복 추가 방지 확인용
    boolean existsByUserIdAndAccommodationsId(Long userId, Long accommodationsId);

    // 사용자가 상세 페이지에서 찜 버튼을 다시 눌러서 취소(해제)할 때 사용 (내 것만 삭제)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Wishlist w WHERE w.userId = :userId AND w.accommodationsId = :accommodationsId")
    void deleteByUserIdAndAccommodationsId(@Param("userId") Long userId, @Param("accommodationsId") Long accommodationsId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
