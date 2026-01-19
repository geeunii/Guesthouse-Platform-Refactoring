package com.ssg9th2team.geharbang.domain.review.repository.jpa;

import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewJpaRepository extends JpaRepository<ReviewEntity, Long> {

  // save(entity) 저장/수정
  // findById(id) 조회
  // deleteById(id) 삭제 (hard delete)

  // 리뷰 단건 조회 (삭제되지 않은 것만)
  // 리뷰 수정, 리뷰 삭제에서 사용
  Optional<ReviewEntity> findByReviewIdAndIsDeletedFalse(Long reviewId);

  // 이미 리뷰 작성했는지 확인
  boolean existsByUserIdAndAccommodationsIdAndIsDeletedFalse(Long userId, Long accommodationsId);

  // 유저의 리뷰 개수 카운트 (쿠폰 발급용)
  long countByUserIdAndIsDeletedFalse(Long userId);

  @Modifying
  @Query("DELETE FROM ReviewEntity r WHERE r.userId = :userId")
  void deleteAllByUserId(@Param("userId") Long userId);

  /**
   * 리뷰 내용에 키워드가 포함된 숙소 ID 목록 조회
   */
  @Query("""
      SELECT DISTINCT r.accommodationsId FROM ReviewEntity r
      WHERE r.isDeleted = false
      AND LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
      """)
  List<Long> findAccommodationIdsByKeywordInContent(@Param("keyword") String keyword);

  // 숙소별 최신 리뷰 조회
  List<ReviewEntity> findByAccommodationsIdAndIsDeletedFalse(Long accommodationsId, Pageable pageable);

  // 숙소별 전체 리뷰 개수 조회
  long countByAccommodationsIdAndIsDeletedFalse(Long accommodationsId);
}
