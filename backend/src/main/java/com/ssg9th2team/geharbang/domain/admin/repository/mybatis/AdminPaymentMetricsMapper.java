package com.ssg9th2team.geharbang.domain.admin.repository.mybatis;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentMetricsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminPaymentMetricsMapper {
    List<AdminPaymentMetricsRow> selectMonthlyMetrics(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") Integer status,
            @Param("typeCode") String typeCode,
            @Param("keyword") String keyword
    );

    List<AdminPaymentMetricsRow> selectYearlyMetrics(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") Integer status,
            @Param("typeCode") String typeCode,
            @Param("keyword") String keyword
    );
}
