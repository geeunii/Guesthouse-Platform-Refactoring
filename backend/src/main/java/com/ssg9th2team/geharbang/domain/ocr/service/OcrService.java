package com.ssg9th2team.geharbang.domain.ocr.service;

import com.ssg9th2team.geharbang.domain.ocr.dto.OcrResponse;

public interface OcrService {
    /**
     * 사업자등록증 이미지에서 사업자번호를 추출합니다.
     * @param base64Image Base64 인코딩된 이미지 (data:image/jpeg;base64,... 형식)
     * @return 추출된 사업자 정보
     */
    OcrResponse extractBusinessLicense(String base64Image);
}
