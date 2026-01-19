package com.ssg9th2team.geharbang.domain.accommodation_theme.repository; // New package

import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AccommodationThemeRepository extends JpaRepository<AccommodationTheme, Long> {

    // Custom query to find themes associated with a list of accommodation IDs
    @Query("SELECT at FROM AccommodationTheme at WHERE at.accommodation.accommodationsId IN :accommodationIds")
    List<AccommodationTheme> findByAccommodationIds(@Param("accommodationIds") List<Long> accommodationIds);

    // Custom query to find accommodations by a set of theme IDs
    @Query("SELECT DISTINCT at.accommodation.accommodationsId FROM AccommodationTheme at WHERE at.theme.id IN :themeIds")
    Set<Long> findAccommodationIdsByThemeIds(@Param("themeIds") Set<Long> themeIds);

}
