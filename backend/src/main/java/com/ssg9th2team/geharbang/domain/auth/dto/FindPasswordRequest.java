package com.ssg9th2team.geharbang.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 비밀번호 찾기 - 사용자 확인 DTO

@Getter
@NoArgsConstructor
public class FindPasswordRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;
}
