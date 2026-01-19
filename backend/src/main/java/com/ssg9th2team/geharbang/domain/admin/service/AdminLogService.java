package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminLogRow;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminLogService {

    private final AdminLogMapper adminLogMapper;
    private final ObjectMapper objectMapper;

    public void writeLog(Long adminUserId, String targetType, Long targetId, String actionType, String reason) {
        writeLog(adminUserId, targetType, targetId, actionType, reason, (Map<String, Object>) null);
    }

    public void writeLog(Long adminUserId, String targetType, Long targetId, String actionType, String reason, Map<String, Object> metadata) {
        String metadataJson = serializeMetadata(metadata);
        writeLog(adminUserId, targetType, targetId, actionType, reason, metadataJson);
    }

    public void writeLog(Long adminUserId, String targetType, Long targetId, String actionType, String reason, String metadataJson) {
        try {
            if (adminUserId == null || targetId == null) {
                return;
            }
            String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
            RequestInfo requestInfo = resolveRequestInfo();
            String normalizedMetadata = mergeRequestMetadata(metadataJson, requestInfo);
            int inserted = adminLogMapper.insertAdminLog(
                adminUserId,
                targetType,
                targetId,
                actionType,
                normalizedReason,
                normalizedMetadata,
                requestInfo.ip(),
                requestInfo.userAgent()
            );
            if (inserted > 0) {
                log.info("AdminLog inserted: adminId={}, target={}#{}, action={}", adminUserId, targetType, targetId, actionType);
            }
        } catch (Exception e) {
            log.warn("Failed to insert admin_log: targetType={}, targetId={}, actionType={}",
                    targetType, targetId, actionType, e);
        }
    }

    public AdminPageResponse<AdminLogRow> getLogs(
            LocalDate startDate,
            LocalDate endDate,
            String actionType,
            String targetType,
            Long targetIdExact,
            String keyword,
            int page,
            int size,
            int maxSize
    ) {
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? Math.min(size, maxSize) : maxSize;
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Long normalizedTargetId = targetIdExact;
        if (normalizedTargetId != null) {
            normalizedKeyword = null;
        }
        int offset = safePage * safeSize;
        List<AdminLogRow> items = adminLogMapper.selectAdminLogs(
                start, end, actionType, targetType, normalizedTargetId, normalizedKeyword, safeSize, offset);
        long total = adminLogMapper.countAdminLogs(start, end, actionType, targetType, normalizedTargetId, normalizedKeyword);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return AdminPageResponse.of(items, safePage, safeSize, total, totalPages);
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            return null;
        }
    }

    private String mergeRequestMetadata(String metadataJson, RequestInfo requestInfo) {
        Map<String, Object> base = new LinkedHashMap<>();
        if (StringUtils.hasText(metadataJson)) {
            try {
                base.putAll(objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ex) {
                base.put("payload", metadataJson.trim());
            }
        }
        if (StringUtils.hasText(requestInfo.ip())) {
            base.putIfAbsent("requestIp", requestInfo.ip());
        }
        if (StringUtils.hasText(requestInfo.userAgent())) {
            base.putIfAbsent("userAgent", requestInfo.userAgent());
        }
        return serializeMetadata(base);
    }

    private RequestInfo resolveRequestInfo() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return RequestInfo.empty();
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            String ip = StringUtils.hasText(forwarded)
                    ? forwarded.split(",")[0].trim()
                    : request.getRemoteAddr();
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
            return new RequestInfo(ip, userAgent);
        } catch (Exception ex) {
            return RequestInfo.empty();
        }
    }

    private record RequestInfo(String ip, String userAgent) {
        static RequestInfo empty() {
            return new RequestInfo(null, null);
        }
    }
}
