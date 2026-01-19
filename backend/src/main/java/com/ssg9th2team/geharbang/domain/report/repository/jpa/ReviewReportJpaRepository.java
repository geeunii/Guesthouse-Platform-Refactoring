package com.ssg9th2team.geharbang.domain.report.repository.jpa;

import com.ssg9th2team.geharbang.domain.report.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReviewReportJpaRepository extends JpaRepository<ReviewReport, Long>, JpaSpecificationExecutor<ReviewReport> {

    @Modifying
    @Query("DELETE FROM ReviewReport rr WHERE rr.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    long countByStateAndCreatedAtBefore(String state, LocalDateTime cutoff);
}
