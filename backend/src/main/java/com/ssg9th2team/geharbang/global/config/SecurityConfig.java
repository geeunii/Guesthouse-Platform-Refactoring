package com.ssg9th2team.geharbang.global.config;

import com.ssg9th2team.geharbang.global.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.ssg9th2team.geharbang.global.oauth.service.CustomOAuth2UserService;
import com.ssg9th2team.geharbang.global.oauth.service.OAuth2AuthenticationFailureHandler;
import com.ssg9th2team.geharbang.global.oauth.service.OAuth2AuthenticationSuccessHandler;
import com.ssg9th2team.geharbang.global.security.JwtAuthenticationFilter;
import com.ssg9th2team.geharbang.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider jwtTokenProvider;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
        private final ClientRegistrationRepository clientRegistrationRepository;
        private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // CSRF 비활성화 (JWT 사용)
                                .csrf(AbstractHttpConfigurer::disable)

                                // CORS 설정
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // 세션 사용 안 함 (JWT 사용)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 폼 로그인 비활성화 (SPA에서 처리)
                                .formLogin(form -> form.disable())

                                // 기본 HTTP Basic 인증 비활성화
                                .httpBasic(basic -> basic.disable())

                                // 요청 권한 설정
                                .authorizeHttpRequests(auth -> auth
                                                // 인증이 필요한 API 경로
                                               .requestMatchers("/api/user/**").authenticated()
                                                .requestMatchers("/api/reservations/**").authenticated()
                                                .requestMatchers("/api/reviews/write/**").authenticated()
                                                .requestMatchers("/api/coupons/my").authenticated()
                                                .requestMatchers("/api/coupons/my/ids").authenticated()
                                                .requestMatchers("/api/coupons/issue").authenticated()
                                                .requestMatchers("/api/coupons/*/use").authenticated()
                                                .requestMatchers("/api/wishlist", "/api/wishlist/**").authenticated()
                                                .requestMatchers("/api/host/**").authenticated()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                // 그 외 모든 요청은 허용 (SPA에서 프론트엔드 라우팅 처리)
                                                .anyRequest().permitAll())

                                // OAuth2 로그인 설정 (기본 로그인 페이지 비활성화)
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/api/oauth2/login-disabled") // 비활성화용 더미 경로
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorization")
                                                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                                                                .authorizationRequestResolver(
                                                                                authorizationRequestResolver()))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))

                                // JWT 인증 필터 추가
                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtTokenProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // 네이버 OAuth2 재인증 강제를 위한 커스텀 리졸버
        @Bean
        public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
                DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository,
                                "/oauth2/authorization");

                resolver.setAuthorizationRequestCustomizer(customizer -> {
                        OAuth2AuthorizationRequest.Builder builder = customizer;

                        // 네이버 OAuth2의 경우 auth_type=reauthenticate 파라미터 추가
                        Map<String, Object> additionalParameters = new HashMap<>(
                                        builder.build().getAdditionalParameters());
                        additionalParameters.put("auth_type", "reauthenticate"); // 네이버 재인증 강제

                        builder.additionalParameters(additionalParameters);
                });

                return resolver;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(Arrays.asList("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
