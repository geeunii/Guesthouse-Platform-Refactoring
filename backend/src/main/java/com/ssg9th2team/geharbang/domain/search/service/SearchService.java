package com.ssg9th2team.geharbang.domain.search.service;

import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.search.dto.SearchResolveResponse;
import com.ssg9th2team.geharbang.domain.search.dto.SearchSuggestionResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchService {
    PublicListResponse searchPublicList(
            List<Long> themeIds,
            String keyword,
            int page,
            int size,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            LocalDateTime checkin,
            LocalDateTime checkout,
            Integer guestCount,
            Integer minPrice,
            Integer maxPrice,
            boolean includeUnavailable,
            String sort);

    List<SearchSuggestionResponse> suggestPublicSearch(String keyword, int limit);

    SearchResolveResponse resolvePublicAccommodation(String keyword);
}
