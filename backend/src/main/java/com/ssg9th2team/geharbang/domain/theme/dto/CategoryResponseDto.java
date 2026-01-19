package com.ssg9th2team.geharbang.domain.theme.dto;

import com.ssg9th2team.geharbang.domain.theme.entity.ThemeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CategoryResponseDto {
    private String categoryKey;     // "NATURE", "CULTURE", etc.
    private String categoryName;    // "자연", "문화", etc.
    private String emoji;           // "🌿", "🏛️", etc.

    public static CategoryResponseDto from(ThemeCategory category) {
        return CategoryResponseDto.builder()
                .categoryKey(category.name())
                .categoryName(category.getKoreanName())
                .emoji(category.getEmoji())
                .build();
    }
}
