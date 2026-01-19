package com.ssg9th2team.geharbang.domain.theme.dto;

import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ThemeDto {
    private Long id;
    private String name;
    private String imageUrl;

    public static ThemeDto of(Theme theme) {
        return new ThemeDto(theme.getId(), theme.getThemeName(), theme.getThemeImageUrl());
    }
}
