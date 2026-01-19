package com.ssg9th2team.geharbang.domain.user.controller;

import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;
import com.ssg9th2team.geharbang.domain.user.service.UserService;
import com.ssg9th2team.geharbang.domain.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.ssg9th2team.geharbang.global.common.annotation.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 현재 로그인한 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build(); // or throw AccessDeniedException
        }
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<Void> updateUserProfile(@CurrentUser User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        userService.updateUserProfile(user.getEmail(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@CurrentUser User user,
            @RequestBody DeleteAccountRequest deleteAccountRequest) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        userService.deleteUser(user.getEmail(), deleteAccountRequest);
        return ResponseEntity.noContent().build();
    }
}
