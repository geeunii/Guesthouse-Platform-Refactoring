package com.ssg9th2team.geharbang.domain.admin.controller;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminRejectRequest;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserDetailResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.service.AdminUserService;
import com.ssg9th2team.geharbang.domain.admin.support.AdminId;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService userService;

    @GetMapping
    public AdminPageResponse<AdminUserSummary> getUsers(
            @AdminId Long adminId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(required = false) String hostStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return userService.getUsers(
                normalizeFilter(role),
                normalizeFilter(keyword),
                normalizeFilter(accountStatus),
                normalizeFilter(hostStatus),
                page,
                size,
                sort
        );
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse getUserDetail(
            @AdminId Long adminId,
            @PathVariable Long userId
    ) {
        return userService.getUserDetail(userId);
    }

    @GetMapping("/summary")
    public AdminUserSummaryResponse getUserSummary(@AdminId Long adminId) {
        return userService.getUserSummary();
    }

    @PostMapping("/{userId}/host/approve")
    public AdminUserSummary approveHost(
            @AdminId Long adminId,
            @PathVariable Long userId,
            @RequestBody(required = false) AdminRejectRequest request
    ) {
        return userService.approveHost(adminId, userId, request != null ? request.reason() : null);
    }

    @PostMapping("/{userId}/host/reject")
    public AdminUserSummary rejectHost(
            @AdminId Long adminId,
            @PathVariable Long userId,
            @RequestBody(required = false) AdminRejectRequest request
    ) {
        return userService.rejectHost(adminId, userId, request != null ? request.reason() : null);
    }

    @PostMapping("/{userId}/suspend")
    public AdminUserSummary suspendUser(
            @AdminId Long adminId,
            @PathVariable Long userId,
            @RequestBody(required = false) AdminRejectRequest request
    ) {
        return userService.suspendUser(adminId, userId, request != null ? request.reason() : null);
    }

    @PostMapping("/{userId}/unsuspend")
    public AdminUserSummary unsuspendUser(
            @AdminId Long adminId,
            @PathVariable Long userId,
            @RequestBody(required = false) AdminRejectRequest request
    ) {
        return userService.unsuspendUser(adminId, userId, request != null ? request.reason() : null);
    }

    private String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("all".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)
                || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }
}
