package com.ssg9th2team.geharbang.domain.admin.support;

import com.ssg9th2team.geharbang.domain.auth.entity.Admin;
import com.ssg9th2team.geharbang.domain.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminIdentityResolver {

    private final AdminRepository adminRepository;

    public Long resolveAdminUserId(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("ADMIN role required");
        }
        if (!hasAdminRole(authentication)) {
            throw new AccessDeniedException("ADMIN role required");
        }
        String username = authentication.getName();
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        return admin.getId();
    }

    private boolean hasAdminRole(Authentication authentication) {
        if (authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ADMIN".equals(role));
    }
}
