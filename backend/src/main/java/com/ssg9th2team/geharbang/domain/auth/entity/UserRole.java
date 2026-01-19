package com.ssg9th2team.geharbang.domain.auth.entity;

public enum UserRole {
    USER("ROLE_USER", "일반 사용자"),
    HOST("ROLE_HOST", "호스트");

    private final String key;
    private final String description;

    UserRole(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }
}
