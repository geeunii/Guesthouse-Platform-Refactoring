package com.ssg9th2team.geharbang.domain.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {
    private boolean success;
    private String businessNumber;  // 추출된 사업자번호 (10자리 숫자)
    private String businessName;    // 상호명
    private String representative;  // 대표자명
    private String fullText;        // 전체 추출 텍스트 (디버깅용)
    private String errorMessage;    // 에러 메시지
}
