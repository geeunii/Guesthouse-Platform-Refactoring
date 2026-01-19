package com.ssg9th2team.geharbang.domain.auth.repository;

import com.ssg9th2team.geharbang.domain.auth.entity.SocialProvider;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSocialRepository extends JpaRepository<UserSocial, Long> {
    
    /// 소셜 로그인 제공자와 제공자 고유 ID로 사용자 소셜 정보 조회
    Optional<UserSocial> findByProviderAndProviderUid(SocialProvider provider, String providerUid);
    
    /// 사용자 ID로 소셜 로그인 정보 조회
    Optional<UserSocial> findByUserId(Long userId);

    // 사용자로 모든 소셜 로그인 정보 조회
    List<UserSocial> findByUser(User user);
    
    // 사용자와 제공자로 소셜 로그인 정보 존재 여부 확인
    boolean existsByUserAndProvider(User user, SocialProvider provider);
}
