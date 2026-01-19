package com.ssg9th2team.geharbang.global.oauth.service;

import com.ssg9th2team.geharbang.global.oauth.service.OAuth2UserInfo;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = (Map<String, Object>) kakaoAccount.get("profile");
    }

    @Override
    public String getProviderId() {
        // 카카오 응답의 최상위 'id' 값을 String으로 변환하여 반환
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        // 'kakao_account' 맵에서 'email' 정보를 가져옴
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getName() {
        // 'profile' 맵에서 'nickname' 정보를 가져옴
        if (profile == null) {
            return null;
        }
        return (String) profile.get("nickname");
    }

    @Override
    public String getGender() {
        // 카카오 API에서 성별 정보는 'gender' 필드로 제공됨
        // 단, 사용자가 동의 항목에서 '성별'을 선택해야만 받을 수 있음
        if (kakaoAccount == null) {
            return null;
        }
        return (String) kakaoAccount.get("gender");
    }

    @Override
    public String getMobile() {
        // 카카오 API에서 전화번호 정보는 'phone_number' 필드로 제공됨
        // 단, 사용자가 동의 항목에서 '카카오계정(전화번호)'을 선택해야만 받을 수 있음
        if (kakaoAccount == null) {
            return null;
        }
        return (String) kakaoAccount.get("phone_number");
    }
}
