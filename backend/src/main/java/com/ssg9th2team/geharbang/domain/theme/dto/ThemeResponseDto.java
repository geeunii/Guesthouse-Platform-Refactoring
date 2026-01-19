package com.ssg9th2team.geharbang.domain.theme.dto;

import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ThemeResponseDto {
    private Long id;
    private String themeCategory;
    private String themeName;
    private Long accommodationCount;
    private String themeImageUrl;

    public static ThemeResponseDto from(Theme theme) {
        return from(theme, 0L);
    }

    public static ThemeResponseDto from(Theme theme, Long accommodationCount) {
        return ThemeResponseDto.builder()
                .id(theme.getId())
                .themeCategory(theme.getThemeCategory())
                .themeName(theme.getThemeName())
                .accommodationCount(accommodationCount == null ? 0L : accommodationCount)
                .themeImageUrl(theme.getThemeImageUrl())
                .build();
    }
}
