package com.ssg9th2team.geharbang.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_social")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider; // GOOGLE, KAKAO, NAVER

    @Column(name = "provider_uid", nullable = false, length = 255)
    private String providerUid; // 소셜 로그인 제공자의 고유 ID

    @Column(length = 255)
    private String email; // 소셜 계정 이메일

    @Column(name = "profile_image", length = 255)
    private String profileImage; // 프로필 이미지 URL

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserSocial(User user, SocialProvider provider, String providerUid, String email, String profileImage) {
        this.user = user;
        this.provider = provider;
        this.providerUid = providerUid;
        this.email = email;
        this.profileImage = profileImage;
    }

    // 프로필 이미지 업데이트
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
