package com.ssg9th2team.geharbang.domain.ocr.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OcrRequest {
    private String image; // Base64 인코딩된 이미지 (data:image/jpeg;base64,... 형식)
}
