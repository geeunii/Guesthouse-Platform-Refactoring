package com.ssg9th2team.geharbang.domain.theme.repository;

import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    @Query(value = """
            SELECT
                t.theme_id AS themeId,
                COUNT(DISTINCT a.accommodations_id) AS accommodationCount
            FROM theme t
            LEFT JOIN accommodation_theme at ON at.theme_id = t.theme_id
            LEFT JOIN accommodation a ON a.accommodations_id = at.accommodations_id
                AND a.accommodation_status = 1
                AND a.approval_status = 'APPROVED'
            GROUP BY t.theme_id
            """, nativeQuery = true)
    List<ThemeAccommodationCountProjection> findThemeAccommodationCounts();
}
