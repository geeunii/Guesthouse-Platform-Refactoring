package com.ssg9th2team.geharbang.domain.ocr.controller;

import com.ssg9th2team.geharbang.domain.ocr.dto.OcrRequest;
import com.ssg9th2team.geharbang.domain.ocr.dto.OcrResponse;
import com.ssg9th2team.geharbang.domain.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    /**
     * 사업자등록증 이미지에서 사업자번호 추출
     * POST /api/ocr/business-license
     */
    @PostMapping("/business-license")
    public ResponseEntity<OcrResponse> extractBusinessLicense(@RequestBody OcrRequest request) {
        log.info("사업자등록증 OCR 요청 수신");

        if (request.getImage() == null || request.getImage().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    OcrResponse.builder()
                            .success(false)
                            .errorMessage("이미지가 제공되지 않았습니다.")
                            .build()
            );
        }

        OcrResponse response = ocrService.extractBusinessLicense(request.getImage());

        if (response.isSuccess()) {
            log.info("사업자번호 추출 성공: {}", response.getBusinessNumber());
            return ResponseEntity.ok(response);
        } else {
            log.warn("사업자번호 추출 실패: {}", response.getErrorMessage());
            return ResponseEntity.ok(response); // 실패해도 200으로 응답, success: false로 구분
        }
    }
}
