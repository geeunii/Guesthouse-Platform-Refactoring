package com.ssg9th2team.geharbang.domain.admin.dto;

import com.ssg9th2team.geharbang.domain.user.entity.Gender;
import com.ssg9th2team.geharbang.domain.auth.entity.SocialProvider;
import com.ssg9th2team.geharbang.domain.auth.entity.User;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long userId,
        String email,
        String name,
        String nickname,
        String phone,
        Gender gender,
        String role,
        Boolean marketingAgreed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean hostApproved,
        Boolean suspended,
        SocialProvider socialProvider,
        Boolean socialSignupCompleted
) {
    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getGender(),
                user.getRole().name(),
                user.getMarketingAgreed(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getHostApproved(),
                user.getSuspended(),
                user.getSocialProvider(),
                user.getSocialSignupCompleted()
        );
    }
}
