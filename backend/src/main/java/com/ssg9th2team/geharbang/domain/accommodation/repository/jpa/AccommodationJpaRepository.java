package com.ssg9th2team.geharbang.domain.accommodation.repository.jpa;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccommodationJpaRepository
                extends JpaRepository<Accommodation, Long>, JpaSpecificationExecutor<Accommodation> {

        @Query("select a from Accommodation a where a.latitude is null or a.longitude is null")
        List<Accommodation> findMissingCoordinates(Pageable pageable);

        long countByLatitudeIsNullOrLongitudeIsNull();

        @Query(value = """
                        SELECT COALESCE(AVG(DATEDIFF(:now, DATE(created_at))), 0)
                        FROM accommodation
                        WHERE approval_status = 'PENDING'
                        """, nativeQuery = true)
        Double avgPendingLeadTimeDays(@Param("now") java.time.LocalDate now);

        long countByApprovalStatusAndCreatedAtBefore(
                        com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus status,
                        LocalDateTime cutoff);

        /**
         * 테마 카테고리에 해당하는 승인된 숙소 검색
         * AccommodationTheme 중간 테이블을 통해 JOIN
         */
        @Query("""
                        SELECT DISTINCT a FROM Accommodation a
                        JOIN com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme at ON at.accommodation = a
                        JOIN at.theme t
                        WHERE t.themeCategory IN :themeCategories
                        AND a.accommodationStatus = 1
                        AND a.approvalStatus = 'APPROVED'
                        ORDER BY a.rating DESC
                        """)
        List<Accommodation> findByThemeCategories(@Param("themeCategories") List<String> themeCategories);

        /**
         * 키워드로 숙소 설명 검색 (승인된 숙소만)
         */
        @Query("""
                        SELECT DISTINCT a FROM Accommodation a
                        WHERE a.accommodationStatus = 1
                        AND a.approvalStatus = 'APPROVED'
                        AND (LOWER(a.accommodationsDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))
                             OR LOWER(a.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))
                             OR LOWER(a.accommodationsName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        ORDER BY a.rating DESC
                        """)
        List<Accommodation> findByKeywordInDescription(@Param("keyword") String keyword);

        /**
         * 위치(city/district/township)로 승인된 숙소 검색
         */
        @Query("""
                        SELECT DISTINCT a FROM Accommodation a
                        WHERE a.accommodationStatus = 1
                        AND a.approvalStatus = 'APPROVED'
                        AND (LOWER(a.city) LIKE LOWER(CONCAT('%', :location, '%'))
                             OR LOWER(a.district) LIKE LOWER(CONCAT('%', :location, '%'))
                             OR LOWER(a.township) LIKE LOWER(CONCAT('%', :location, '%')))
                        ORDER BY a.rating DESC
                        """)
        List<Accommodation> findByLocation(@Param("location") String location);

        List<Accommodation> findByUserId(Long userId);
}
