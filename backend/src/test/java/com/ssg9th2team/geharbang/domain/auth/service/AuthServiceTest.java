package com.ssg9th2team.geharbang.domain.auth.service;

import com.ssg9th2team.geharbang.domain.auth.dto.CompleteSocialSignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.LoginRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.SignupRequest;
import com.ssg9th2team.geharbang.domain.auth.dto.TokenResponse;
import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.entity.Admin;
import com.ssg9th2team.geharbang.domain.auth.entity.SocialProvider;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.AdminRepository;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeRepository;
import com.ssg9th2team.geharbang.global.security.JwtTokenProvider;
import com.ssg9th2team.geharbang.global.service.EmailService;
import com.ssg9th2team.geharbang.global.util.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private ThemeRepository themeRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private VerificationCodeService verificationCodeService;
    @Mock
    private com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService userCouponService;

    private User testUser;
    private Admin testAdmin;
    private Theme testTheme1;
    private Theme testTheme2;

    @BeforeEach
    void setUp() {
        // Mock User
        testUser = User.builder()
                .name("Test User").nickname("TestNick").email("user@example.com")
                .password("encodedPassword").phone("010-1234-5678").role(UserRole.USER)
                .marketingAgreed(false).socialProvider(SocialProvider.LOCAL).socialId(null)
                .socialSignupCompleted(true) // For completeSocialSignup test
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L); // Manually set ID for user

        // Mock Admin
        testAdmin = Admin.builder()
                .username("admin@example.com")
                .password("adminEncodedPassword")
                .build();
        ReflectionTestUtils.setField(testAdmin, "id", 99L); // Manually set ID for admin

        // Mock Themes
        testTheme1 = Theme.builder().themeName("테마1").themeCategory("카테고리1").build();
        ReflectionTestUtils.setField(testTheme1, "id", 10L);
        testTheme2 = Theme.builder().themeName("테마2").themeCategory("카테고리2").build();
        ReflectionTestUtils.setField(testTheme2, "id", 11L);

        // Common mock behaviors
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true); // Default to true for password matching
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access.token");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refresh.token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);
    }

    // --- Signup Tests ---
    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest(
                "New User", "NewNick", null, "new@example.com",
                "password123", "010-9876-5432", List.of(testTheme1.getId()), true);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(themeRepository.findAllById(anyList())).thenReturn(List.of(testTheme1));
        when(userRepository.save(any(User.class))).thenReturn(testUser); // Return a saved user mock

        // when
        UserResponse response = authService.signup(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_EmailDuplicate() {
        // given
        SignupRequest request = new SignupRequest(null, null, null, "duplicate@example.com", null, null, null, null);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.signup(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signup_Fail_NicknameDuplicate() {
        // given
        SignupRequest request = new SignupRequest(null, "duplicateNick", null, "new@example.com", null, null, null, null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByNickname(anyString())).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.signup(request));
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Login Tests ---
    @Test
    @DisplayName("로그인 성공 - 사용자")
    void login_UserSuccess() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        when(adminRepository.findByUsername(anyString())).thenReturn(Optional.empty()); // Not an admin
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        
        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("로그인 성공 - 관리자")
    void login_AdminSuccess() {
        // given
        LoginRequest request = new LoginRequest("admin@example.com", "adminPassword");
        when(adminRepository.findByUsername(anyString())).thenReturn(Optional.of(testAdmin));
        // passwordEncoder.matches is mocked to return true in setUp
        
        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_Fail_UserNotFound() {
        // given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");
        when(adminRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_WrongPassword() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");
        when(adminRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false); // Simulate wrong password

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    // --- Refresh Token Tests ---
    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_Success() {
        // given
        String refreshToken = "valid.refresh.token";
        Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(refreshToken)).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user@example.com");

        // when
        TokenResponse response = authService.refresh(refreshToken);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        verify(jwtTokenProvider, times(1)).generateAccessToken(authentication);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 Refresh Token")
    void refresh_Fail_InvalidToken() {
        // given
        String refreshToken = "invalid.refresh.token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.refresh(refreshToken));
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any(Authentication.class));
    }

    // --- Social Signup Completion Tests ---
    @Test
    @DisplayName("소셜 회원가입 완료 성공")
    void completeSocialSignup_Success() {
        // given
        CompleteSocialSignupRequest request = new CompleteSocialSignupRequest(true, true, List.of(testTheme1.getId()));
        
        // Ensure user's socialSignupCompleted is not already true
        ReflectionTestUtils.setField(testUser, "socialSignupCompleted", false); 
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(themeRepository.findAllById(anyList())).thenReturn(List.of(testTheme1));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        UserResponse response = authService.completeSocialSignup(testUser.getId(), request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        // verify(testUser, times(1)).completeSocialSignup(true, new HashSet<>(List.of(testTheme1))); // testUser is not a mock
        verify(userRepository, times(1)).save(testUser);
        assertThat(testUser.getSocialSignupCompleted()).isTrue(); // Verify state change
    }

    @Test
    @DisplayName("소셜 회원가입 완료 실패 - 사용자 없음")
    void completeSocialSignup_Fail_UserNotFound() {
        // given
        CompleteSocialSignupRequest request = new CompleteSocialSignupRequest(true, null, null);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.completeSocialSignup(99L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("소셜 회원가입 완료 실패 - 이미 완료된 사용자")
    void completeSocialSignup_Fail_AlreadyCompleted() {
        // given
        CompleteSocialSignupRequest request = new CompleteSocialSignupRequest(true, null, null);
        User completedUser = User.builder().email("completed@example.com").socialSignupCompleted(true).build();
        ReflectionTestUtils.setField(completedUser, "id", 2L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(completedUser));

        // when & then
        assertThrows(IllegalStateException.class, () -> authService.completeSocialSignup(completedUser.getId(), request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("소셜 회원가입 완료 실패 - 필수 약관 미동의")
    void completeSocialSignup_Fail_TermsNotAgreed() {
        // given
        CompleteSocialSignupRequest request = new CompleteSocialSignupRequest(false, null, null);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        ReflectionTestUtils.setField(testUser, "socialSignupCompleted", false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.completeSocialSignup(testUser.getId(), request));
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Check Duplicate Tests ---
    @Test
    @DisplayName("이메일 중복 확인")
    void checkEmailDuplicate() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThat(authService.checkEmailDuplicate("exists@example.com")).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 확인")
    void checkNicknameDuplicate() {
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        assertThat(authService.checkNicknameDuplicate("newnick")).isFalse();
    }

    // --- Email Verification Tests ---
    @Test
    @DisplayName("이메일 인증 코드 전송 성공")
    void sendVerificationCode_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(verificationCodeService.generateAndSaveCode(anyString())).thenReturn("123456");
        authService.sendVerificationCode("new@example.com");
        verify(emailService, times(1)).sendVerificationEmail(eq("new@example.com"), eq("123456"));
    }

    @Test
    @DisplayName("이메일 인증 코드 전송 실패 - 이메일 중복")
    void sendVerificationCode_Fail_EmailDuplicate() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.sendVerificationCode("duplicate@example.com"));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 확인 성공")
    void verifyEmailCode_Success() {
        com.ssg9th2team.geharbang.domain.auth.dto.VerifyCodeRequest request = 
            new com.ssg9th2team.geharbang.domain.auth.dto.VerifyCodeRequest("test@example.com", "123456");
        when(verificationCodeService.verifyCode(anyString(), anyString())).thenReturn(true);
        assertThat(authService.verifyEmailCode(request)).isTrue();
    }
}
