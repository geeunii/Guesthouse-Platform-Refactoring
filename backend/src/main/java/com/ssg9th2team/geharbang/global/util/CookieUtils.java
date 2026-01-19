package com.ssg9th2team.geharbang.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class CookieUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(name))
                    .forEach(cookie -> {
                        cookie.setValue("");
                        cookie.setPath("/");
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                    });
        }
    }

    /**
     * OAuth2AuthorizationRequest를 JSON으로 직렬화하여 URL 인코딩된 문자열로 반환
     * SerializationUtils.serialize() 대신 안전한 JSON 직렬화 사용
     */
    public static String serializeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("authorizationUri", authorizationRequest.getAuthorizationUri());
            data.put("clientId", authorizationRequest.getClientId());
            data.put("redirectUri", authorizationRequest.getRedirectUri());
            data.put("scopes", authorizationRequest.getScopes());
            data.put("state", authorizationRequest.getState());
            data.put("authorizationRequestUri", authorizationRequest.getAuthorizationRequestUri());
            data.put("attributes", authorizationRequest.getAttributes());
            data.put("additionalParameters", authorizationRequest.getAdditionalParameters());

            // responseType은 문자열로 저장
            if (authorizationRequest.getResponseType() != null) {
                data.put("responseType", authorizationRequest.getResponseType().getValue());
            }

            String json = objectMapper.writeValueAsString(data);
            return URLEncoder.encode(json, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OAuth2AuthorizationRequest", e);
            throw new IllegalStateException("Failed to serialize OAuth2AuthorizationRequest", e);
        }
    }

    /**
     * URL 인코딩된 JSON 문자열을 OAuth2AuthorizationRequest로 역직렬화
     * SerializationUtils.deserialize() 대신 안전한 JSON 역직렬화 사용
     * 역직렬화 실패 시 예외 대신 null 반환 (OAuth2 플로우 정상 진행 위함)
     */
    @SuppressWarnings("unchecked")
    public static OAuth2AuthorizationRequest deserializeAuthorizationRequest(Cookie cookie) {
        try {
            if (cookie == null || cookie.getValue() == null || cookie.getValue().isEmpty()) {
                return null;
            }

            String json = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            // 필수 필드 검증
            String authorizationUri = (String) data.get("authorizationUri");
            String clientId = (String) data.get("clientId");
            if (authorizationUri == null || clientId == null) {
                log.warn("Invalid OAuth2AuthorizationRequest cookie: missing required fields");
                return null;
            }

            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri((String) data.get("redirectUri"))
                    .state((String) data.get("state"))
                    .authorizationRequestUri((String) data.get("authorizationRequestUri"));

            // scopes 처리
            Object scopesObj = data.get("scopes");
            if (scopesObj instanceof Iterable) {
                Set<String> scopes = new HashSet<>();
                ((Iterable<?>) scopesObj).forEach(s -> scopes.add((String) s));
                builder.scopes(scopes);
            }

            // attributes 처리
            Object attributesObj = data.get("attributes");
            if (attributesObj instanceof Map) {
                builder.attributes(attrs -> attrs.putAll((Map<String, Object>) attributesObj));
            }

            // additionalParameters 처리
            Object additionalParamsObj = data.get("additionalParameters");
            if (additionalParamsObj instanceof Map) {
                builder.additionalParameters((Map<String, Object>) additionalParamsObj);
            }

            return builder.build();
        } catch (Exception e) {
            // 역직렬화 실패 시 null 반환 - OAuth2 인증 실패로 처리되어 FailureHandler 호출됨
            log.warn("Failed to deserialize OAuth2AuthorizationRequest, treating as expired/invalid: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 일반 문자열을 URL 인코딩
     */
    public static String serialize(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * URL 디코딩하여 문자열 반환
     */
    public static String deserialize(Cookie cookie) {
        return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
    }
}
