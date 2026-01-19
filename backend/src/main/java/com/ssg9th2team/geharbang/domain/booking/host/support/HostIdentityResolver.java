package com.ssg9th2team.geharbang.domain.booking.host.support;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HostIdentityResolver {

    private final UserRepository userRepository;

    public Long resolveHostUserId(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("HOST role required");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!hasHostRole(authentication, user)) {
            throw new AccessDeniedException("HOST role required");
        }
        // 현재 구조에서 hostUserId는 userId와 동일하다.
        return user.getId();
    }

    private boolean hasHostRole(Authentication authentication, User user) {
        if (authentication.getAuthorities() != null) {
            boolean allowed = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> "ROLE_HOST".equals(role) || "HOST".equals(role));
            if (allowed) return true;
        }
        return user.getRole() == UserRole.HOST;
    }
}
