package com.ssg9th2team.geharbang.domain.admin.repository.mybatis;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminDashboardMapper {
    List<AdminAccommodationSummary> selectPendingAccommodations(@Param("limit") int limit);
}
