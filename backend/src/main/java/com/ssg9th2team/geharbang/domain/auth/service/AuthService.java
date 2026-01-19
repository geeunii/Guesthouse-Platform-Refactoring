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

public interface AuthService {

    // 회원가입
    UserResponse signup(SignupRequest signupRequest);

    // 로그인
    TokenResponse login(LoginRequest loginRequest);

    // 이메일 찾기
    FindEmailResponse findEmail(FindEmailRequest findEmailRequest);

    // 비밀번호 찾기 - 사용자 확인 및 인증 코드 전송
    void findPassword(FindPasswordRequest findPasswordRequest);

    // 비밀번호 재설정
    void resetPassword(ResetPasswordRequest resetPasswordRequest);

    // 이메일 중복 확인
    boolean checkEmailDuplicate(String email);

    // 닉네임 중복 확인
    boolean checkNicknameDuplicate(String nickname);

    // 이메일 인증 코드 전송
    void sendVerificationCode(String email);

    // 이메일 인증 코드 확인
    boolean verifyEmailCode(VerifyCodeRequest verifyCodeRequest);

    // 이메일 인증 코드 확인만 (삭제하지 않음) - 비밀번호 찾기용
    boolean verifyEmailCodeOnly(VerifyCodeRequest verifyCodeRequest);

    // 리프레시 토큰으로 액세스 토큰 재발급
    TokenResponse refresh(String refreshToken);

    // 소셜 회원가입 완료
    UserResponse completeSocialSignup(Long userId, CompleteSocialSignupRequest request);

    // 자사 계정과 소셜 계정 연결
    UserResponse linkSocialAccount(Long userId, String provider, String providerId);
}
