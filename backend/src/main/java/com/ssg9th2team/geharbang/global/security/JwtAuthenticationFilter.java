package com.ssg9th2team.geharbang.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // JWT 검증을 건너뛸 경로 목록
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/auth/",
            "/api/public/",
            "/api/ocr/",
            "/api/list/",
            "/uploads/",
            "/error",
            "/oauth2/",
            "/login/oauth2/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    // JWT 검증을 건너뛸 정확한 경로 목록 (startsWith가 아닌 equals로 비교)
    private static final List<String> EXACT_EXCLUDE_PATHS = Arrays.asList(
            "/api/themes"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // EXCLUDE_PATHS에 포함된 경로로 시작하는 경우 JWT 필터링 건너뛰기
        boolean startsWithExclude = EXCLUDE_PATHS.stream().anyMatch(path::startsWith);

        // EXACT_EXCLUDE_PATHS와 정확히 일치하는 경우 JWT 필터링 건너뛰기
        boolean equalsExclude = EXACT_EXCLUDE_PATHS.stream().anyMatch(path::equals);

        return startsWithExclude || equalsExclude;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Request Header에서 JWT 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰이 유효하면 토큰에서 Authentication 객체를 가져와서 SecurityContext에 저장
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security Context에 '{}' 인증 정보를 저장했습니다.", authentication.getName());
        } else {
            log.debug("유효한 JWT 토큰이 없습니다. URI: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    // Request Header에서 토큰 정보 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
