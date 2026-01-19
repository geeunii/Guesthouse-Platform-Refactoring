package com.ssg9th2team.geharbang.global.config;

import com.ssg9th2team.geharbang.domain.admin.support.AdminIdArgumentResolver;
import com.ssg9th2team.geharbang.global.common.resolver.CurrentUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // 이거 꼭 추가해야 합니다!
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminIdArgumentResolver adminIdArgumentResolver;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    // ▼▼▼ [핵심 수정] 설정 파일에서 변수 가져오기 (없으면 기본값 localhost 사용) ▼▼▼
    @Value("${env.oauth2-frontend-base-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000", // 로컬 React
                        "http://localhost:5173", // 로컬 Vite
                        "http://localhost:8080", // 로컬 백엔드
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:8080",
                        // ▼▼▼ [하드코딩 제거됨] 변수로 대체 ▼▼▼
                        frontendUrl, // 예: http://49.50.138.206:8081
                        frontendUrl + ":8081" // 혹시 모를 포트 중복 대비
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(adminIdArgumentResolver);
        resolvers.add(currentUserArgumentResolver);
    }
}