package com.ssg9th2team.geharbang.domain.auth.entity;

import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.user.entity.Gender;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(length = 100) // nullable = true is default
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column // nullable = true (소셜 로그인 시 NULL 가능)
    private Gender gender;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(length = 255) // BCrypt 암호화를 위해 255로 설정
    private String password; // 소셜 로그인 시 NULL 가능

    @Column(length = 50) // 소셜 로그인 시 NULL 가능
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", length = 20)
    private SocialProvider socialProvider; // 소셜 로그인 제공자 (NAVER, KAKAO, GOOGLE, LOCAL)

    @Column(name = "social_id", length = 100)
    private String socialId; // 소셜 로그인 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "marketing_agree", nullable = false)
    private Boolean marketingAgreed = false; // 마케팅 정보 수신 동의

    @Column(name = "host_approved")
    private Boolean hostApproved; // 호스트 숙소 등록 승인 상태 (1: 승인됨, 0: 승인 거절, NULL: 일반 USER)

    @Column(name = "is_suspended", nullable = false)
    private Boolean suspended = false; // 계정 정지 여부

    @Column(name = "social_signup_completed")
    private Boolean socialSignupCompleted; // 소셜 로그인 회원가입 완료 여부 (true: 완료, false: 미완료, NULL: 일반 회원가입)

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_theme",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    private Set<Theme> themes = new HashSet<>();

    @Builder
    public User(String name, String nickname, Gender gender, String email, String password, String phone, UserRole role, Boolean marketingAgreed, Boolean hostApproved, Boolean socialSignupCompleted, Set<Theme> themes, SocialProvider socialProvider, String socialId) {
        this.name = name;
        this.nickname = nickname;
        this.gender = gender;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role != null ? role : UserRole.USER;
        this.marketingAgreed = marketingAgreed != null ? marketingAgreed : false;
        this.hostApproved = hostApproved;
        this.suspended = false;
        this.socialSignupCompleted = socialSignupCompleted;
        this.themes = themes;
        this.socialProvider = socialProvider;
        this.socialId = socialId;
    }

    // 비밀번호 업데이트
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 프로필 업데이트
    public void updateProfile(String name, String nickname, String phone) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
    }

    // 닉네임, 전화번호, 성별 업데이트
    public void updateProfile(String nickname, String phone, Gender gender) {
        this.nickname = nickname;
        this.phone = phone;
        this.gender = gender;
    }

    // 역할 변경 (예: USER -> HOST)
    public void updateRole(UserRole role) {
        this.role = role;
    }

    // 호스트 승인 상태 변경
    public void updateHostApproved(Boolean approved) {
        this.hostApproved = approved;
    }

    public void updateSuspended(Boolean suspended) {
        this.suspended = suspended != null ? suspended : false;
    }

    // 소셜 회원가입 완료 처리
    public void completeSocialSignup(Boolean marketingAgreed, Set<Theme> themes) {
        this.socialSignupCompleted = true;
        this.marketingAgreed = marketingAgreed != null ? marketingAgreed : false;
        if (themes != null) {
            this.themes = themes;
        }
    }
}
