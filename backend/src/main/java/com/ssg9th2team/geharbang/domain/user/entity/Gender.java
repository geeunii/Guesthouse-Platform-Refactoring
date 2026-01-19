package com.ssg9th2team.geharbang.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("남"),
    FEMALE("여");

    private final String description;
}
