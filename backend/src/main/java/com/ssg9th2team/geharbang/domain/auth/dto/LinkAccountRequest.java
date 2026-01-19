package com.ssg9th2team.geharbang.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LinkAccountRequest {

    @NotBlank(message = "소셜 로그인 제공자는 필수입니다.")
    private String provider; // GOOGLE, KAKAO, NAVER

    @NotBlank(message = "소셜 로그인 ID는 필수입니다.")
    private String providerId;
}
