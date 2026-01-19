package com.ssg9th2team.geharbang.domain.theme.service;

import com.ssg9th2team.geharbang.domain.theme.dto.CategoryResponseDto;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import com.ssg9th2team.geharbang.domain.theme.entity.ThemeCategory;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeRepository;
import com.ssg9th2team.geharbang.domain.theme.repository.ThemeAccommodationCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeServiceImpl implements ThemeService {

    private final ThemeRepository themeRepository;

    @Override
    public List<Theme> findAllThemes() {
        return themeRepository.findAll();
    }

    @Override
    public Map<Long, Long> getThemeAccommodationCounts() {
        return themeRepository.findThemeAccommodationCounts()
                .stream()
                .collect(Collectors.toMap(
                        ThemeAccommodationCountProjection::getThemeId,
                        ThemeAccommodationCountProjection::getAccommodationCount,
                        (existing, ignored) -> existing,
                        HashMap::new
                ));
    }

    @Override
    public List<CategoryResponseDto> getAllCategories() {
        return Arrays.stream(ThemeCategory.values())
                .map(CategoryResponseDto::from)
                .collect(Collectors.toList());
    }
}
