package com.ssg9th2team.geharbang.domain.theme.entity;

import lombok.Getter;

@Getter
public enum ThemeCategory {
    NATURE("자연", "🌿"),
    CULTURE("문화", "🏛️"),
    ACTIVITY("활동", "🏄"),
    VIBE("분위기", "✨"),
    PARTY("파티", "🥳"),
    MEETING("만남", "💞"),
    PERSONA("특성/성향", "👤"),
    FACILITY("시설", "🏠"),
    FOOD("음식", "🍴"),
    PLAY("놀이", "🎮");

    private final String koreanName;
    private final String emoji;

    ThemeCategory(String koreanName, String emoji) {
        this.koreanName = koreanName;
        this.emoji = emoji;
    }

    public static ThemeCategory fromString(String category) {
        try {
            return ThemeCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
