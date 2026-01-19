package com.ssg9th2team.geharbang.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component/// 스프링이 클래스를 관리하도록 빈 등록
public class JwtTokenProvider {

    private final SecretKey key;/// 암호화에 사용할 비밀 키
    private final long accessTokenExpiration;/// Access 토큰 유효 시간
    private final long refreshTokenExpiration;/// Refresh 토큰 유효 시간
    private final UserRepository userRepository;/// 사용자 정보 조회용

    /// 생성자 application.properties에 적은 설정값들을 가져와 초기화
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,/// 비밀키
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,/// 접근토큰
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,/// 리프래시 토큰
            UserRepository userRepository) {/// 사용자 저장소

        /// BASE 64로 인코딩된 비밀키를 디코딩해서 실제 암호화 키 객체로 만들기
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.userRepository = userRepository;
    }

    /// Access Token 생성
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, accessTokenExpiration);
    }

    /// Refresh Token 생성
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, refreshTokenExpiration);
    }

    /// JWT 토큰을 암호화 하여 생성
    private String generateToken(Authentication authentication, long expiration) {
        /// 사용자의 권한 정보 (ROLE_USER, ROLE_ADMIN 등)을 문자열로 합침.
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration); ///현재 시간 + 유효시간 = 만료시간

        return Jwts.builder()
                .subject(authentication.getName()) /// 토큰
                .claim("auth", authorities)///  토큰에 담을 권한 정보
                .issuedAt(now) ///  토큰 발행 시간
                .expiration(expiryDate) /// 토큰 만료 시간
                .signWith(key, Jwts.SIG.HS512)/// 비밀키로 서명 (HS512 알고리즘 사용 )
                .compact(); ///최종적으로 문자열로 변 환
    }

    // 토큰에서 인증 정보 조회
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);///JWT안에 저장된 정보가 있는것을  Claims

        if (claims.get("auth") == null) {/// 만약에 auth 가  null이라면
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");///오류 던져
        }

        /// 토큰에서 이메일 추출
        String email = claims.getSubject();
        
        /// 이메일로 실제 User 엔티티 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "사용자를 찾을 수 없습니다: " + email));

        /// CustomUserDetails 생성 (실제 User 엔티티 포함)
        UserDetails principal = new CustomUserDetails(user);

        /// Spring Security에서 권한을 GrantedAuthority라는 객체로 관리
        /// 권한 목록 생성
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 검증`
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // 토큰에서 Claims 추출
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 토큰에서 사용자 이메일 추출
    public String getUserEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // Access Token 만료 시간 조회
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
