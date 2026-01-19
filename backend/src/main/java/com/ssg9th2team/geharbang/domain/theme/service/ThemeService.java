package com.ssg9th2team.geharbang.domain.theme.service;

import com.ssg9th2team.geharbang.domain.theme.dto.CategoryResponseDto;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import java.util.List;
import java.util.Map;

public interface ThemeService {
    List<Theme> findAllThemes();

    Map<Long, Long> getThemeAccommodationCounts();

    List<CategoryResponseDto> getAllCategories();
}
