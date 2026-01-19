package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserDetailResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminUserSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.log.AdminLogConstants;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.spec.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminLogService adminLogService;
    private static final int MIN_REASON_LENGTH = 5;
    private static final int MAX_REASON_LENGTH = 200;

    private enum HostStatusFilter {
        PENDING,
        APPROVED,
        REJECTED,
        NONE
    }
    public AdminPageResponse<AdminUserSummary> getUsers(
            String role,
            String query,
            String accountStatus,
            String hostStatus,
            int page,
            int size,
            String sort
    ) {
        // 체크: role/query 필터 + page/size가 DB 페이징으로 정상 동작하는지 확인.
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? Math.min(size, 50) : 20;
        Sort sorting = resolveUserSort(sort);

        UserRole targetRole = parseRole(role);
        Boolean suspended = parseAccountStatus(accountStatus);
        HostStatusFilter hostStatusFilter = parseHostStatus(hostStatus);
        Page<User> pageResult = userRepository.findAll(
                UserSpecifications.hasRole(targetRole)
                        .and(UserSpecifications.keywordContains(query))
                        .and(UserSpecifications.suspendedEquals(suspended))
                        .and(resolveHostStatusSpec(hostStatusFilter)),
                PageRequest.of(safePage, safeSize, sorting)
        );

        return AdminPageResponse.of(
                pageResult.getContent().stream().map(this::toSummary).toList(),
                safePage,
                safeSize,
                (int) pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = findUser(userId);
        return AdminUserDetailResponse.from(user);
    }

    public AdminUserSummaryResponse getUserSummary() {
        long totalUsers = userRepository.count();
        long totalGuests = userRepository.count(UserSpecifications.hasRole(UserRole.USER));
        long totalHosts = userRepository.count(UserSpecifications.hasRole(UserRole.HOST));
        long pendingHostApprovals = userRepository.count(
                UserSpecifications.hasRole(UserRole.HOST)
                        .and(UserSpecifications.hostApprovedIsNull())
        );
        long suspendedUsers = userRepository.count(UserSpecifications.suspendedEquals(true));
        return new AdminUserSummaryResponse(
                totalUsers,
                totalGuests,
                totalHosts,
                pendingHostApprovals,
                suspendedUsers
        );
    }

    public AdminUserSummary approveHost(Long adminUserId, Long userId, String reason) {
        User user = findUser(userId);
        UserRole beforeRole = user.getRole();
        Boolean beforeApproved = user.getHostApproved();
        if (user.getRole() != UserRole.HOST) {
            user.updateRole(UserRole.HOST);
        }
        user.updateHostApproved(true);
        User saved = userRepository.save(user);
        String normalizedReason = normalizeOptionalReason(reason, "메모");
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("before", java.util.Map.of("role", beforeRole != null ? beforeRole.name() : null, "hostApproved", beforeApproved));
        metadata.put("after", java.util.Map.of("role", saved.getRole().name(), "hostApproved", true));
        if (StringUtils.hasText(reason)) {
            metadata.put("reason", normalizedReason);
        }
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_USER,
                userId,
                AdminLogConstants.ACTION_APPROVE,
                normalizedReason,
                metadata
        );
        return toSummary(saved);
    }

    public AdminUserSummary rejectHost(Long adminUserId, Long userId, String reason) {
        User user = findUser(userId);
        UserRole beforeRole = user.getRole();
        Boolean beforeApproved = user.getHostApproved();
        if (user.getRole() != UserRole.HOST) {
            user.updateRole(UserRole.HOST);
        }
        user.updateHostApproved(false);
        User saved = userRepository.save(user);
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("before", java.util.Map.of("role", beforeRole != null ? beforeRole.name() : null, "hostApproved", beforeApproved));
        metadata.put("after", java.util.Map.of("role", saved.getRole().name(), "hostApproved", false));
        String normalizedReason = normalizeRequiredReason(reason, "반려 사유");
        metadata.put("reason", normalizedReason);
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_USER,
                userId,
                AdminLogConstants.ACTION_REJECT,
                normalizedReason,
                metadata
        );
        return toSummary(saved);
    }

    public AdminUserSummary suspendUser(Long adminUserId, Long userId, String reason) {
        User user = findUser(userId);
        Boolean beforeSuspended = user.getSuspended();
        user.updateSuspended(true);
        User saved = userRepository.save(user);
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("before", java.util.Map.of("suspended", beforeSuspended));
        metadata.put("after", java.util.Map.of("suspended", true));
        String normalizedReason = normalizeRequiredReason(reason, "정지 사유");
        metadata.put("reason", normalizedReason);
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_USER,
                userId,
                AdminLogConstants.ACTION_BAN,
                normalizedReason,
                metadata
        );
        return toSummary(saved);
    }

    public AdminUserSummary unsuspendUser(Long adminUserId, Long userId, String reason) {
        User user = findUser(userId);
        Boolean beforeSuspended = user.getSuspended();
        user.updateSuspended(false);
        User saved = userRepository.save(user);
        String normalizedReason = normalizeOptionalReason(reason, "메모");
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("before", java.util.Map.of("suspended", beforeSuspended));
        metadata.put("after", java.util.Map.of("suspended", false));
        if (StringUtils.hasText(reason)) {
            metadata.put("reason", normalizedReason);
        }
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_USER,
                userId,
                AdminLogConstants.ACTION_UNBAN,
                normalizedReason,
                metadata
        );
        return toSummary(saved);
    }

    private Sort resolveUserSort(String sort) {
        if (!StringUtils.hasText(sort) || "latest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        if ("idAsc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        if ("idDesc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort value");
    }

    private UserRole parseRole(String role) {
        if (!StringUtils.hasText(role) || "all".equalsIgnoreCase(role)) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        return switch (normalized) {
            case "HOST", "ROLE_HOST" -> UserRole.HOST;
            case "USER", "ROLE_USER", "GUEST" -> UserRole.USER;
            default -> null;
        };
    }

    private Boolean parseAccountStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if ("SUSPENDED".equals(normalized)) {
            return true;
        }
        if ("ACTIVE".equals(normalized)) {
            return false;
        }
        return null;
    }

    private HostStatusFilter parseHostStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING" -> HostStatusFilter.PENDING;
            case "APPROVED" -> HostStatusFilter.APPROVED;
            case "REJECTED" -> HostStatusFilter.REJECTED;
            case "NONE" -> HostStatusFilter.NONE;
            default -> null;
        };
    }

    private org.springframework.data.jpa.domain.Specification<User> resolveHostStatusSpec(HostStatusFilter status) {
        if (status == null) {
            return org.springframework.data.jpa.domain.Specification.where(null);
        }
        return (root, query, cb) -> {
            switch (status) {
                case NONE -> {
                    return cb.equal(root.get("role"), UserRole.USER);
                }
                case PENDING -> {
                    return cb.and(
                            cb.equal(root.get("role"), UserRole.HOST),
                            cb.isNull(root.get("hostApproved"))
                    );
                }
                case APPROVED -> {
                    return cb.and(
                            cb.equal(root.get("role"), UserRole.HOST),
                            cb.isTrue(root.get("hostApproved"))
                    );
                }
                case REJECTED -> {
                    return cb.and(
                            cb.equal(root.get("role"), UserRole.HOST),
                            cb.isFalse(root.get("hostApproved"))
                    );
                }
                default -> {
                    return cb.conjunction();
                }
            }
        };
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserSummary toSummary(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getPhone(),
                user.getHostApproved(),
                user.getSuspended(),
                user.getCreatedAt()
        );
    }

    private String normalizeRequiredReason(String reason, String label) {
        if (!StringUtils.hasText(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "를 5자 이상 입력해 주세요.");
        }
        String normalized = reason.trim();
        if (normalized.length() < MIN_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "를 5자 이상 입력해 주세요.");
        }
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "는 200자 이내로 입력해 주세요.");
        }
        return normalized;
    }

    private String normalizeOptionalReason(String reason, String label) {
        if (!StringUtils.hasText(reason)) {
            return "-";
        }
        String normalized = reason.trim();
        if (normalized.length() < MIN_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "는 5자 이상 입력해 주세요.");
        }
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "는 200자 이내로 입력해 주세요.");
        }
        return normalized;
    }
}
