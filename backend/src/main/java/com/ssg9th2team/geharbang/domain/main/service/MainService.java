package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.main.dto.MainAccommodationListResponse;
import java.util.List;
import java.util.Map;

public interface MainService {

    MainAccommodationListResponse getMainAccommodationList(Long userId, List<Long> filterThemeIds, String keyword);

    // 벌크 조회 - 여러 테마를 한 번에 조회
    Map<Long, MainAccommodationListResponse> getMainAccommodationListBulk(Long userId, List<Long> themeIds,
            String keyword);

}
