package com.ssg9th2team.geharbang.global.oauth.service;

import java.util.Map;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getEmail();
    String getName();
    String getGender(); // 성별 정보 추가
    String getMobile(); // 전화번호 정보 추가
}
