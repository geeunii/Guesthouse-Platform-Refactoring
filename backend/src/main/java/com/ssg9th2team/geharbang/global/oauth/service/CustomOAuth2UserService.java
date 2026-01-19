package com.ssg9th2team.geharbang.global.oauth.service;

import com.ssg9th2team.geharbang.domain.auth.entity.SocialProvider;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.entity.UserSocial;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.auth.repository.UserSocialRepository;
import com.ssg9th2team.geharbang.domain.user.entity.Gender;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserSocialRepository userSocialRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 Login - Provider: {}", registrationId);

        OAuth2UserInfo oAuth2UserInfo = null;

        if ("naver".equals(registrationId)) {
            oAuth2UserInfo = new NaverOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("google".equals(registrationId)) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("kakao".equals(registrationId)) {
            oAuth2UserInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        String providerId = oAuth2UserInfo.getProviderId();
        String provider = oAuth2UserInfo.getProvider();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();
        String genderStr = oAuth2UserInfo.getGender();
        String mobile = oAuth2UserInfo.getMobile();

        log.info("OAuth2 User Info - Provider: {}, ProviderId: {}, Email: {}, Name: {}, Gender: {}, Mobile: {}",
                provider, providerId, email, name, genderStr, mobile);

        SocialProvider socialProvider = SocialProvider.valueOf(provider.toUpperCase());

        Optional<UserSocial> userSocialOptional = userSocialRepository.findByProviderAndProviderUid(
                socialProvider,
                providerId
        );

        User user;
        boolean needsAccountLink = false;
        SocialProvider pendingProvider = null;
        String pendingProviderId = null;

        if (userSocialOptional.isPresent()) {
            UserSocial userSocial = userSocialOptional.get();
            try {
                user = userSocial.getUser();
                // Eager loading이 아니므로, user의 속성에 접근하는 순간 예외가 발생할 수 있음
                log.info("기존 소셜 로그인 사용자 로그인: {}", user.getEmail());

                // 이메일로 자사 가입자가 있는지 확인 (user_social이 잘못된 user를 참조하는 경우 처리)
                Optional<User> localUser = userRepository.findByEmail(email);
                if (localUser.isPresent() && !localUser.get().getId().equals(user.getId())) {
                    User existingLocalUser = localUser.get();
                    // 자사 가입자가 존재하고, user_social이 다른 user를 참조하는 경우
                    if (existingLocalUser.getPassword() != null) {
                        log.warn("이메일 {}에 대해 자사 가입자(userId={})와 소셜 연결 사용자(userId={})가 다릅니다.",
                                email, existingLocalUser.getId(), user.getId());
                        log.info("기존 user_social을 삭제하고 자사 가입자와 연결을 시도합니다.");

                        // 기존 user_social 삭제
                        userSocialRepository.delete(userSocial);

                        // 자사 가입자와 연결 필요
                        user = existingLocalUser;
                        needsAccountLink = true;
                        pendingProvider = socialProvider;
                        pendingProviderId = providerId;
                    }
                }
            } catch (EntityNotFoundException e) {
                log.warn("소셜 로그인 정보는 있으나 연결된 사용자를 찾을 수 없습니다. (provider={}, providerUid={})", userSocial.getProvider(), userSocial.getProviderUid());
                log.warn("오래된 소셜 로그인 정보를 삭제하고 신규 가입 절차를 진행합니다.");

                userSocialRepository.delete(userSocial);
                ProcessResult result = processNewSocialUser(email, name, genderStr, mobile, socialProvider, providerId);
                user = result.getUser();
                needsAccountLink = result.isNeedsAccountLink();
                pendingProvider = result.getPendingProvider();
                pendingProviderId = result.getPendingProviderId();
            }
        } else {
            // 신규 소셜 로그인 사용자
            ProcessResult result = processNewSocialUser(email, name, genderStr, mobile, socialProvider, providerId);
            user = result.getUser();
            needsAccountLink = result.isNeedsAccountLink();
            pendingProvider = result.getPendingProvider();
            pendingProviderId = result.getPendingProviderId();
        }

        // 계정 통합이 필요한 경우 정보를 attributes에 저장
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        if (needsAccountLink && pendingProvider != null) {
            attributes.put("pendingProvider", pendingProvider.name());
            attributes.put("pendingProviderId", pendingProviderId);
        }

        return new CustomOAuth2User(user, attributes, needsAccountLink);
    }

    private ProcessResult processNewSocialUser(String email, String name, String genderStr, String mobile, SocialProvider socialProvider, String providerId) {
        log.info("신규 소셜 로그인 처리: {}", email);

        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        boolean needsAccountLink = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // 기존 자사 가입자인지 확인 (password가 있고, socialSignupCompleted가 null인 경우)
            if (user.getPassword() != null && user.getSocialSignupCompleted() == null) {
                log.info("기존 자사 가입 사용자 발견 - 계정 통합 필요: {}", email);
                needsAccountLink = true;
                // 아직 UserSocial 저장하지 않음 - 사용자 동의 후 저장
                return new ProcessResult(user, needsAccountLink, socialProvider, providerId);
            }

            log.info("기존 사용자에 소셜 계정 연결: {}", email);
        } else {
            Gender gender = convertGender(genderStr);
            user = User.builder()
                    .name(name)
                    .nickname(generateUniqueNickname(name))
                    .gender(gender)
                    .email(email)
                    .password(null)
                    .phone(mobile)
                    .role(UserRole.USER)
                    .marketingAgreed(false)
                    .hostApproved(null)
                    .socialSignupCompleted(false)
                    .build();
            user = userRepository.save(user);
            log.info("신규 사용자 생성 완료: {}", user.getEmail());
        }

        UserSocial newUserSocial = UserSocial.builder()
                .user(user)
                .provider(socialProvider)
                .providerUid(providerId)
                .email(email)
                .build();
        userSocialRepository.save(newUserSocial);
        log.info("소셜 로그인 정보 저장 완료: provider={}, uid={}", socialProvider, providerId);

        return new ProcessResult(user, needsAccountLink, null, null);
    }

    // 처리 결과를 담는 내부 클래스
    @Getter
    @RequiredArgsConstructor
    private static class ProcessResult {
        private final User user;
        private final boolean needsAccountLink;
        private final SocialProvider pendingProvider; // 연결 대기 중인 소셜 제공자
        private final String pendingProviderId; // 연결 대기 중인 소셜 ID
    }

    private String generateUniqueNickname(String baseName) {
        String nickname = baseName;
        int count = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = baseName + count;
            count++;
        }
        return nickname;
    }

    private Gender convertGender(String genderStr) {
        if (genderStr == null) {
            return null;
        }
        
        return switch (genderStr.toUpperCase()) {
            case "M", "MALE" -> Gender.MALE;
            case "F", "FEMALE" -> Gender.FEMALE;
            default -> null;
        };
    }
}

