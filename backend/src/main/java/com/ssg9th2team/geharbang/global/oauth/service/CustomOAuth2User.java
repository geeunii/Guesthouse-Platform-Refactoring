package com.ssg9th2team.geharbang.global.oauth.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private User user;
    private Map<String, Object> attributes;
    private boolean needsAccountLink; // 기존 자사 가입자와 소셜 계정 연결 필요 여부

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
        this.needsAccountLink = false;
    }

    public CustomOAuth2User(User user, Map<String, Object> attributes, boolean needsAccountLink) {
        this.user = user;
        this.attributes = attributes;
        this.needsAccountLink = needsAccountLink;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey()));
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public Long getUserId() {
        return user.getId();
    }
}
