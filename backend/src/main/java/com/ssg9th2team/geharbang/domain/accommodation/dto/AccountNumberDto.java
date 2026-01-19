package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountNumberDto {
    // 호스트 정산계좌 DTO
    private Long accountNumberId;  // 호스트 정산계좌 코드
    private String bankName;       // 은행명
    private String accountNumber;  // 계좌번호
    private String accountHolder;  // 예금주


    // sg
}
