package com.ssg9th2team.geharbang.domain.auth.service;

import com.ssg9th2team.geharbang.domain.auth.dto.CompleteSocialSignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.FindEmailRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.FindEmailResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.FindPasswordRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.ResetPasswordRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.SignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.TokenResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.VerifyCodeRequest;
import com.ssg9th2team.geharbang.domain.auth.entity.SocialProvider;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.entity.UserSocial;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.repository.UserSocialRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeRepository;
import com.ssg9th2team.geharbang.global.security.JwtTokenProvider;
import com.ssg9th2team.geharbang.global.service.EmailService;
import com.ssg9th2team.geharbang.global.util.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ssg9th2team.geharbang.domain.auth.entity.Admin;
import com.ssg9th2team.geharbang.domain.auth.repository.AdminRepository;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSocialRepository userSocialRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ThemeRepository themeRepository;
    private final EmailService emailService; // 이메일 서비스
    private final VerificationCodeService verificationCodeService; // 인증 코드 관리 서비스
    private final UserCouponService userCouponService; // 쿠폰 서비스

    @Override
    @Transactional
    public UserResponse signup(SignupRequest signupRequest) {
        // 1. 이메일 및 닉네임 중복 체크
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        // 3. 관심 테마 조회
        Set<Theme> themes = new HashSet<>();
        if (!CollectionUtils.isEmpty(signupRequest.getThemeIds())) {
            List<Theme> foundThemes = themeRepository.findAllById(signupRequest.getThemeIds());
            themes.addAll(foundThemes);
        }

        // 4. User 엔티티 생성
        User user = User.builder()
                .name(signupRequest.getName())
                .nickname(signupRequest.getNickname())
                .gender(signupRequest.getGender())
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .phone(signupRequest.getPhone())
                .role(UserRole.USER)
                .marketingAgreed(
                        signupRequest.getMarketingAgreed() != null ? signupRequest.getMarketingAgreed() : false)
                .hostApproved(null)
                .themes(themes)
                .socialProvider(SocialProvider.LOCAL)
                .socialId(null)
                .build();

        User savedUser = userRepository.save(user);

        // 회원가입 축하 쿠폰 발급
        userCouponService.issueByTrigger(savedUser.getId(), CouponTriggerType.SIGNUP);

        log.info("회원가입 완료: {}", savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest loginRequest) {
        // 관리자 우선으로 확인
        Optional<Admin> adminOptional = adminRepository.findByUsername(loginRequest.getEmail());
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), admin.getPassword())) {
                return createAdminToken(admin);
            }
        }

        // 사용자로 확인
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return createUserToken(user);
    }

    @Override
    @Transactional(readOnly = true)
    public FindEmailResponse findEmail(FindEmailRequest findEmailRequest) {
        log.info("이메일 찾기 시도: name='{}', phone='{}'", findEmailRequest.getName(), findEmailRequest.getPhone());

        User user = userRepository.findByNameAndPhone(findEmailRequest.getName(), findEmailRequest.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 사용자 정보가 없습니다."));

        return new FindEmailResponse(user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public void findPassword(FindPasswordRequest findPasswordRequest) {
        log.info("비밀번호 찾기 시도: email='{}', phone='{}'", findPasswordRequest.getEmail(), findPasswordRequest.getPhone());

        // 1. 이메일과 전화번호로 사용자 존재 확인
        User user = userRepository.findByEmail(findPasswordRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 사용자 정보가 없습니다."));

        if (!user.getPhone().equals(findPasswordRequest.getPhone())) {
            throw new IllegalArgumentException("일치하는 사용자 정보가 없습니다.");
        }

        // 2. 인증 코드 생성 및 저장
        String verificationCode = verificationCodeService.generateAndSaveCode(user.getEmail());

        // 3. 이메일 전송
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        log.info("비밀번호 재설정 인증 코드 전송 완료: {} (코드: {})", user.getEmail(), verificationCode);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        log.info("비밀번호 재설정 시도: email='{}'", resetPasswordRequest.getEmail());

        // 1. 인증 코드 확인
        boolean isVerified = verificationCodeService.verifyCode(
                resetPasswordRequest.getEmail(),
                resetPasswordRequest.getCode());

        if (!isVerified) {
            throw new IllegalArgumentException("인증 코드가 유효하지 않거나 만료되었습니다.");
        }

        // 2. 사용자 조회
        User user = userRepository.findByEmail(resetPasswordRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(resetPasswordRequest.getNewPassword());
        user.updatePassword(encodedPassword);

        userRepository.save(user);
        log.info("비밀번호 재설정 완료: {}", user.getEmail());
    }

    // 관리자 토큰 생성
    private TokenResponse createAdminToken(Admin admin) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getUsername(),
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("관리자 로그인 성공: {}", admin.getUsername());

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .role("ADMIN")
                .userId(admin.getId())
                .build();
    }

    // 사용자 토큰 생성
    private TokenResponse createUserToken(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())));

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("로그인 성공: {}", user.getEmail());

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    // 이메일 인증 코드 전송
    @Override
    public void sendVerificationCode(String email) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            // 이메일이 이미 사용 중인 경우
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 인증 코드 생성 및 저장
        String verificationCode = verificationCodeService.generateAndSaveCode(email);

        // 3. 이메일 전송
        emailService.sendVerificationEmail(email, verificationCode);
        log.info("인증 코드 전송 완료: {} (코드: {})", email, verificationCode);
    }

    // 이메일 인증 코드 확인. -> verifyCodeRequest
    // 인증 코드가 유효하지 않거나 만료된 경우 -> IllegalArgumentException
    @Override
    @Transactional(readOnly = true)
    public boolean verifyEmailCode(VerifyCodeRequest verifyCodeRequest) {
        return verificationCodeService.verifyCode(verifyCodeRequest.getEmail(), verifyCodeRequest.getCode());
    }

    // 이메일 인증 코드 확인만 (삭제하지 않음) - 비밀번호 찾기용
    @Override
    @Transactional(readOnly = true)
    public boolean verifyEmailCodeOnly(VerifyCodeRequest verifyCodeRequest) {
        return verificationCodeService.verifyCodeOnly(verifyCodeRequest.getEmail(), verifyCodeRequest.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. Refresh Token에서 사용자 정보 추출
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);

        // 3. 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        log.info("토큰 갱신 성공: {}", authentication.getName());

        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Override
    @Transactional
    public UserResponse completeSocialSignup(Long userId, CompleteSocialSignupRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 이미 회원가입이 완료된 경우
        if (user.getSocialSignupCompleted() != null && user.getSocialSignupCompleted()) {
            throw new IllegalStateException("이미 회원가입이 완료된 사용자입니다.");
        }

        // 3. 약관 동의 확인
        if (request.getTermsAgreed() == null || !request.getTermsAgreed()) {
            throw new IllegalArgumentException("필수 약관에 동의해야 합니다.");
        }

        // 4. 관심 테마 조회
        Set<Theme> themes = new HashSet<>();
        if (!CollectionUtils.isEmpty(request.getThemeIds())) {
            List<Theme> foundThemes = themeRepository.findAllById(request.getThemeIds());
            themes.addAll(foundThemes);
        }

        // 5. 소셜 회원가입 완료 처리
        user.completeSocialSignup(request.getMarketingAgreed(), themes);

        User savedUser = userRepository.save(user);

        // 회원가입 축하 쿠폰 발급
        userCouponService.issueByTrigger(savedUser.getId(), CouponTriggerType.SIGNUP);

        log.info("소셜 회원가입 완료: {}", savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    public UserResponse linkSocialAccount(Long userId, String provider, String providerId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 소셜 제공자 변환
        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }

        // 3. 이미 연결된 소셜 계정인지 확인
        boolean alreadyLinked = userSocialRepository.findByProviderAndProviderUid(socialProvider, providerId).isPresent();
        if (alreadyLinked) {
            throw new IllegalStateException("이미 다른 계정에 연결된 소셜 계정입니다.");
        }

        // 4. 소셜 계정 연결
        UserSocial userSocial = UserSocial.builder()
                .user(user)
                .provider(socialProvider)
                .providerUid(providerId)
                .email(user.getEmail())
                .build();
        userSocialRepository.save(userSocial);

        log.info("계정 통합 완료: userId={}, provider={}, providerId={}", userId, provider, providerId);
        return UserResponse.from(user);
    }
}
