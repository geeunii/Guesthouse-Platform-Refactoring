package com.ssg9th2team.geharbang.domain.user.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;

import com.ssg9th2team.geharbang.domain.user.dto.UpdateProfileRequest;

public interface UserService {
    void deleteUser(String email, DeleteAccountRequest deleteAccountRequest);

    // 이메일로 사용자 정보 조회
    User getUserByEmail(String email);

    // 사용자 프로필(닉네임, 전화번호) 업데이트
    void updateUserProfile(String email, UpdateProfileRequest request);
}
