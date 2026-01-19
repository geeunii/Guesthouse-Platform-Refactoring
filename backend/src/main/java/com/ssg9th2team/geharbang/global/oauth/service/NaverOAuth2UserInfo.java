package com.ssg9th2team.geharbang.global.oauth.service;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        return (String) response.get("name");
    }

    @Override
    public String getGender() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        // 네이버는 "M", "F", "U" (미확인) 형식으로 성별 정보 제공
        return (String) response.get("gender");
    }

    @Override
    public String getMobile() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            return null;
        }
        // 네이버는 'mobile' 필드로 휴대전화번호 제공
        return (String) response.get("mobile");
    }
}
