package com.ssg9th2team.geharbang.domain.ocr.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.ssg9th2team.geharbang.domain.ocr.dto.OcrResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    @Value("${google.cloud.credentials.location}")
    private Resource credentialsResource;

    private ImageAnnotatorSettings visionSettings;
    private boolean ocrEnabled = false;

    @PostConstruct
    public void init() {
        if (credentialsResource == null || !credentialsResource.exists()) {
            log.warn("Google Cloud Vision 인증 파일이 없어 OCR 기능을 비활성화합니다.");
            ocrEnabled = false;
            return;
        }

        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsResource.getInputStream())
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-vision"));

            visionSettings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            ocrEnabled = true;

            log.info("Google Cloud Vision 인증 초기화 완료");
        } catch (IOException e) {
            ocrEnabled = false;
            log.warn("Google Cloud Vision 인증 파일 로드 실패: OCR 기능 비활성화", e);
        }
    }

    // 사업자번호 패턴: XXX-XX-XXXXX 또는 XXXXXXXXXX
    private static final Pattern BUSINESS_NUMBER_PATTERN =
            Pattern.compile("(\\d{3})[\\s\\-]?(\\d{2})[\\s\\-]?(\\d{5})");

    // 상호명 패턴
    private static final Pattern BUSINESS_NAME_PATTERN =
            Pattern.compile("(?:상\\s*호|법인명|상호명)[\\s:：]*([가-힣a-zA-Z0-9\\s]+)");

    // 대표자명 패턴
    private static final Pattern REPRESENTATIVE_PATTERN =
            Pattern.compile("(?:대\\s*표\\s*자|성\\s*명)[\\s:：]*([가-힣]+)");

    @Override
    public OcrResponse extractBusinessLicense(String base64Image) {
        try {
            if (!ocrEnabled) {
                return OcrResponse.builder()
                        .success(false)
                        .errorMessage("OCR 기능이 비활성화되어 있습니다. 관리자에게 문의해주세요.")
                        .build();
            }

            // Base64 이미지 디코딩
            byte[] imageBytes = decodeBase64Image(base64Image);

            if (imageBytes == null || imageBytes.length == 0) {
                return OcrResponse.builder()
                        .success(false)
                        .errorMessage("이미지 디코딩에 실패했습니다.")
                        .build();
            }

            // Google Cloud Vision API 호출
            String extractedText = performOcr(imageBytes);

            if (extractedText == null || extractedText.isEmpty()) {
                return OcrResponse.builder()
                        .success(false)
                        .errorMessage("이미지에서 텍스트를 추출할 수 없습니다.")
                        .build();
            }

            log.info("OCR 추출 텍스트: {}", extractedText);

            // 사업자번호 추출
            String businessNumber = extractBusinessNumber(extractedText);

            if (businessNumber == null) {
                return OcrResponse.builder()
                        .success(false)
                        .fullText(extractedText)
                        .errorMessage("사업자번호를 찾을 수 없습니다.")
                        .build();
            }

            // 사업자번호 유효성 검증
            if (!validateBusinessNumber(businessNumber)) {
                return OcrResponse.builder()
                        .success(false)
                        .businessNumber(businessNumber)
                        .fullText(extractedText)
                        .errorMessage("유효하지 않은 사업자번호입니다.")
                        .build();
            }

            // 상호명, 대표자명 추출
            String businessName = extractBusinessName(extractedText);
            String representative = extractRepresentative(extractedText);

            return OcrResponse.builder()
                    .success(true)
                    .businessNumber(businessNumber)
                    .businessName(businessName)
                    .representative(representative)
                    .fullText(extractedText)
                    .build();

        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생", e);
            return OcrResponse.builder()
                    .success(false)
                    .errorMessage("OCR 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Base64 이미지를 바이트 배열로 디코딩
     */
    private byte[] decodeBase64Image(String base64Image) {
        try {
            String base64Data = base64Image;

            // data:image/jpeg;base64, 형식 처리
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }

            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            log.error("Base64 디코딩 실패", e);
            return null;
        }
    }

    /**
     * Google Cloud Vision API를 사용하여 OCR 수행
     */
    private String performOcr(byte[] imageBytes) throws Exception {
        if (visionSettings == null) {
            throw new Exception("Google Cloud Vision 인증이 초기화되지 않았습니다.");
        }

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create(visionSettings)) {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(
                    java.util.Collections.singletonList(request));

            java.util.List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    log.error("Vision API 오류: {}", res.getError().getMessage());
                    throw new Exception(res.getError().getMessage());
                }

                // 전체 텍스트 반환 (첫 번째 TextAnnotation이 전체 텍스트)
                if (!res.getTextAnnotationsList().isEmpty()) {
                    return res.getTextAnnotationsList().get(0).getDescription();
                }
            }
        }
        return null;
    }

    /**
     * 텍스트에서 사업자번호 추출 (10자리)
     */
    private String extractBusinessNumber(String text) {
        Matcher matcher = BUSINESS_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            // 숫자만 추출하여 10자리로 반환
            return matcher.group(1) + matcher.group(2) + matcher.group(3);
        }
        return null;
    }

    /**
     * 텍스트에서 상호명 추출
     */
    private String extractBusinessName(String text) {
        Matcher matcher = BUSINESS_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 텍스트에서 대표자명 추출
     */
    private String extractRepresentative(String text) {
        Matcher matcher = REPRESENTATIVE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 사업자번호 유효성 검증 (체크섬 알고리즘)
     */
    private boolean validateBusinessNumber(String bizNum) {
        if (bizNum == null || bizNum.length() != 10) {
            return false;
        }

        try {
            int[] checkSum = {1, 3, 7, 1, 3, 7, 1, 3, 5};
            int sum = 0;

            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(bizNum.charAt(i)) * checkSum[i];
            }
            sum += (Character.getNumericValue(bizNum.charAt(8)) * 5) / 10;

            int remainder = (10 - (sum % 10)) % 10;
            return remainder == Character.getNumericValue(bizNum.charAt(9));
        } catch (Exception e) {
            return false;
        }
    }
}
