package com.ssg9th2team.geharbang.domain.auth.dto;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private Gender gender;
    private Boolean marketingAgreed;
    private Boolean hostApproved;
    private LocalDateTime createdAt;

    // User 엔티티로부터 UserResponse 생성
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .marketingAgreed(user.getMarketingAgreed())
                .hostApproved(user.getHostApproved())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
