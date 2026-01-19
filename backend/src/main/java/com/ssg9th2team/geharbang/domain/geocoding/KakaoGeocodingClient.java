package com.ssg9th2team.geharbang.domain.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@Slf4j
public class KakaoGeocodingClient implements GeocodingClient {

    private static final String AUTH_HEADER = "Authorization";
    private static final String AUTH_PREFIX = "KakaoAK ";
    private static final int DEFAULT_RESULT_SIZE = 1;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kakao.rest-api-key:}")
    private String restApiKey;

    @Value("${kakao.geocode-url:https://dapi.kakao.com/v2/local/search/address.json}")
    private String geocodeUrl;

    @Override
    public Optional<GeoPoint> geocode(String address) {
        if (!StringUtils.hasText(address)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(restApiKey)) {
            throw new IllegalStateException("Kakao rest api key is not configured.");
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(geocodeUrl)
                    .queryParam("query", address)
                    .queryParam("size", DEFAULT_RESULT_SIZE);

            HttpHeaders headers = new HttpHeaders();
            headers.set(AUTH_HEADER, AUTH_PREFIX + restApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.build().encode().toUri(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Kakao geocode failed with status {}", response.getStatusCode());
                return Optional.empty();
            }
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            return parseCoordinates(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Kakao geocode request failed.", ex);
        }
    }

    private Optional<GeoPoint> parseCoordinates(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                return Optional.empty();
            }

            JsonNode first = documents.get(0);
            GeoPoint point = extractPoint(first.path("road_address"));
            if (point == null) {
                point = extractPoint(first.path("address"));
            }
            if (point == null) {
                point = extractPoint(first);
            }
            return Optional.ofNullable(point);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Kakao geocode response.", ex);
        }
    }

    private GeoPoint extractPoint(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String x = textValue(node, "x");
        String y = textValue(node, "y");
        if (!StringUtils.hasText(x) || !StringUtils.hasText(y)) {
            return null;
        }
        BigDecimal longitude = new BigDecimal(x).setScale(7, RoundingMode.HALF_UP);
        BigDecimal latitude = new BigDecimal(y).setScale(7, RoundingMode.HALF_UP);
        return new GeoPoint(latitude, longitude);
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
