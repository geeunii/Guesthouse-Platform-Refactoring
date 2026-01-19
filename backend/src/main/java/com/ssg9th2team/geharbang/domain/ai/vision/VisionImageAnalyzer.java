package com.ssg9th2team.geharbang.domain.ai.vision;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import com.ssg9th2team.geharbang.domain.ai.vision.dto.VisionLabel;
import com.ssg9th2team.geharbang.global.image.ImageResizeProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisionImageAnalyzer {

    @Value("${google.cloud.credentials.location:}")
    private Resource credentialsResource;

    private final ImageResizeProcessor imageResizeProcessor;

    private ImageAnnotatorSettings visionSettings;
    private boolean enabled = false;
    private static final int MAX_IMAGE_WIDTH = 1024;
    private static final int MAX_IMAGE_HEIGHT = 1024;

    @PostConstruct
    public void init() {
        if (credentialsResource == null || !credentialsResource.exists()) {
            log.warn("Google Cloud Vision credentials not found. Image analysis is disabled.");
            enabled = false;
            return;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsResource.getInputStream())
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-vision"));
            visionSettings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            enabled = true;
            log.info("VisionImageAnalyzer initialized with Google Cloud Vision credentials.");
        } catch (IOException e) {
            log.warn("Failed to load Google Cloud Vision credentials. Image analysis disabled.", e);
            enabled = false;
        }
    }

    public ImageAnalysisResult analyze(String base64Image) {
        return analyzeMultiple(Collections.singletonList(base64Image));
    }

    public ImageAnalysisResult analyzeMultiple(List<String> base64Images) {
        if (!enabled) {
            return ImageAnalysisResult.disabled();
        }
        if (base64Images == null || base64Images.isEmpty()) {
            return ImageAnalysisResult.empty();
        }

        List<AnnotateImageRequest> requests = new ArrayList<>();
        for (String base64Image : base64Images) {
            byte[] bytes = decode(base64Image);
            if (bytes.length == 0) {
                continue;
            }
            Image image = Image.newBuilder().setContent(ByteString.copyFrom(bytes)).build();
            Feature textFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            Feature labelFeature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(textFeature)
                    .addFeatures(labelFeature)
                    .setImage(image)
                    .build();
            requests.add(request);
        }

        if (requests.isEmpty()) {
            return ImageAnalysisResult.empty();
        }

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(visionSettings)) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            if (response == null || response.getResponsesCount() == 0) {
                return ImageAnalysisResult.empty();
            }

            StringBuilder textBuilder = new StringBuilder();
            List<VisionLabel> allLabels = new ArrayList<>();

            for (AnnotateImageResponse result : response.getResponsesList()) {
                if (result.hasError()) {
                    log.warn("Vision API error: {}", result.getError().getMessage());
                    continue;
                }
                if (!result.getTextAnnotationsList().isEmpty()) {
                    String text = result.getTextAnnotations(0).getDescription();
                    if (text != null && !text.isBlank()) {
                        if (textBuilder.length() > 0) {
                            textBuilder.append("\n---\n");
                        }
                        textBuilder.append(text);
                    }
                }
                
                java.util.Set<String> seenDescriptions = new java.util.HashSet<>();
                result.getLabelAnnotationsList().forEach(entity -> {
                    VisionLabel label = VisionLabel.builder()
                            .description(entity.getDescription())
                            .score(entity.getScore())
                            .build();
                    if (seenDescriptions.add(label.getDescription())) {
                        allLabels.add(label);
                    }
                });
            }

            allLabels.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
            return ImageAnalysisResult.success(textBuilder.toString(), allLabels);
        } catch (Exception e) {
            log.warn("Vision API invocation failed: {}", e.getMessage());
            return ImageAnalysisResult.empty();
        }
    }

    private byte[] decode(String imagePayload) {
        if (imagePayload == null || imagePayload.isBlank()) return new byte[0];
        try {
            byte[] rawBytes;
            if (isHttpUrl(imagePayload)) {
                rawBytes = downloadRemoteImage(imagePayload);
            } else {
                String data = imagePayload;
                if (data.contains(",")) {
                    data = data.substring(data.indexOf(",") + 1);
                }
                rawBytes = Base64.getDecoder().decode(data);
            }
            if (rawBytes.length == 0) {
                return new byte[0];
            }
            return imageResizeProcessor.resizeToJpeg(rawBytes, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to decode base64 image: {}", ex.getMessage());
            return new byte[0];
        }
    }

    @Getter
    public static class ImageAnalysisResult {
        private final boolean visionEnabled;
        private final String fullText;
        private final List<VisionLabel> labels;

        private ImageAnalysisResult(boolean visionEnabled, String fullText, List<VisionLabel> labels) {
            this.visionEnabled = visionEnabled;
            this.fullText = fullText;
            this.labels = labels;
        }

        public static ImageAnalysisResult disabled() {
            return new ImageAnalysisResult(false, "", Collections.emptyList());
        }

        public static ImageAnalysisResult empty() {
            return new ImageAnalysisResult(true, "", Collections.emptyList());
        }

        public static ImageAnalysisResult success(String text, List<VisionLabel> labels) {
            return new ImageAnalysisResult(true, text == null ? "" : text, labels == null ? Collections.emptyList() : labels);
        }
    }

    private boolean isHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private byte[] downloadRemoteImage(String imageUrl) {
        try (InputStream inputStream = new URL(imageUrl).openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.warn("Failed to download image from {}: {}", imageUrl, e.getMessage());
            return new byte[0];
        }
    }
}
