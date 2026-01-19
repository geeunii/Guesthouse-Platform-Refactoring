package com.ssg9th2team.geharbang.global.oauth;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.ssg9th2team.geharbang.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Value("${oauth2.allowed-redirect-hosts:localhost,127.0.0.1}")
    private String allowedRedirectHosts;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(CookieUtils::deserializeAuthorizationRequest)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serializeAuthorizationRequest(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin) && isValidRedirectUri(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME,
                    CookieUtils.serialize(redirectUriAfterLogin), COOKIE_EXPIRE_SECONDS);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);

        // 인터페이스 계약 준수: 쿠키를 실제로 삭제
        if (authorizationRequest != null) {
            removeAuthorizationRequestCookies(request, response);
        }

        return authorizationRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    /**
     * redirect_uri가 허용된 호스트인지 검증 (Open Redirect 방지)
     */
    private boolean isValidRedirectUri(String redirectUri) {
        try {
            URI uri = URI.create(redirectUri);
            String host = uri.getHost();

            if (host == null) {
                return false;
            }

            List<String> allowedHosts = List.of(allowedRedirectHosts.split(","));
            boolean isValid = allowedHosts.stream()
                    .map(String::trim)
                    .anyMatch(allowedHost -> host.equals(allowedHost) || host.endsWith("." + allowedHost));

            if (!isValid) {
                log.warn("Invalid redirect_uri blocked: {}", redirectUri);
            }

            return isValid;
        } catch (IllegalArgumentException e) {
            log.warn("Malformed redirect_uri blocked: {}", redirectUri);
            return false;
        }
    }
}
