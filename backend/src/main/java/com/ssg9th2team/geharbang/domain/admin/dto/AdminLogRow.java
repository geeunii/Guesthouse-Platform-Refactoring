package com.ssg9th2team.geharbang.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminLogRow {
    private Long logId;
    private Long adminId;
    private String adminUsername;
    private String targetType;
    private Long targetId;
    private String actionType;
    private String reason;
    private String metadataJson;
    private String requestIp;
    private String userAgent;
    private LocalDateTime createdAt;
}
