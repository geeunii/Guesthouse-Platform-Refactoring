package com.ssg9th2team.geharbang.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSocialSignupRequest {

    @NotNull(message = "약관 동의 상태는 필수입니다.")
    private Boolean termsAgreed; // 필수 약관 동의 여부

    private Boolean marketingAgreed; // 마케팅 정보 수신 동의

    private List<Long> themeIds; // 관심 테마 ID 목록
}
