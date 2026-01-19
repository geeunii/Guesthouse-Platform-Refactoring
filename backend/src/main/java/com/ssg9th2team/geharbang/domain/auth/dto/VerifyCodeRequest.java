package com.ssg9th2team.geharbang.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/// 이메일 인증 코드 확인 요청 DTO
///
///사용자가 이메일 인증을 할 때, 클라이언트에서 서버로 보내는 데이터를 담는 객체
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequest {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "인증 코드는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String code;
}
