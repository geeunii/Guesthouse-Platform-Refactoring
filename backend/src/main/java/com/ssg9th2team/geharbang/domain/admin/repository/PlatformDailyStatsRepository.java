package com.ssg9th2team.geharbang.domain.admin.repository;

import com.ssg9th2team.geharbang.domain.admin.entity.PlatformDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlatformDailyStatsRepository extends JpaRepository<PlatformDailyStats, LocalDate> {
    List<PlatformDailyStats> findByStatDateBetweenOrderByStatDateAsc(LocalDate from, LocalDate to);
}
