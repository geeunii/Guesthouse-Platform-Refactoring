package com.ssg9th2team.geharbang.domain.admin.repository.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminLogMapper {
    int insertAdminLog(
            @Param("adminId") Long adminId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("actionType") String actionType,
            @Param("reason") String reason,
            @Param("metadataJson") String metadataJson,
            @Param("requestIp") String requestIp,
            @Param("userAgent") String userAgent
    );

    List<com.ssg9th2team.geharbang.domain.admin.dto.AdminLogRow> selectAdminLogs(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            @Param("actionType") String actionType,
            @Param("targetType") String targetType,
            @Param("targetIdExact") Long targetIdExact,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    long countAdminLogs(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            @Param("actionType") String actionType,
            @Param("targetType") String targetType,
            @Param("targetIdExact") Long targetIdExact,
            @Param("keyword") String keyword
    );
}
