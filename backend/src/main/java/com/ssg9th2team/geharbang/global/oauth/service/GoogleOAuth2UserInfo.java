package com.ssg9th2team.geharbang.global.oauth.service;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getGender() {
        // Google API 기본 범위에서는 성별 정보를 제공하지 않습니다.
        // 별도 권한 요청 및 앱 검증이 필요하므로 null로 반환합니다.
        return null;
    }

    @Override
    public String getMobile() {
        // Google API 기본 범위에서는 전화번호 정보를 제공하지 않습니다.
        return null;
    }
}
