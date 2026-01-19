package com.ssg9th2team.geharbang.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 이메일 찾기 DTO

@Getter
@NoArgsConstructor
public class FindEmailRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;
}
