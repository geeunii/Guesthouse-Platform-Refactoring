package com.ssg9th2team.geharbang.domain.search.controller;

import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.search.dto.SearchResolveResponse;
import com.ssg9th2team.geharbang.domain.search.dto.SearchSuggestionResponse;
import com.ssg9th2team.geharbang.domain.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public PublicListResponse search(
            @RequestParam(name = "themeIds", required = false) List<Long> themeIds,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "24") int size,
            @RequestParam(name = "minLat", required = false) Double minLat,
            @RequestParam(name = "maxLat", required = false) Double maxLat,
            @RequestParam(name = "minLng", required = false) Double minLng,
            @RequestParam(name = "maxLng", required = false) Double maxLng,
            @RequestParam(name = "checkin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam(name = "checkout", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
            @RequestParam(name = "guestCount", required = false) Integer guestCount,
            @RequestParam(name = "minPrice", required = false) Integer minPrice,
            @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(name = "includeUnavailable", defaultValue = "false") boolean includeUnavailable,
            @RequestParam(name = "sort", required = false) String sort) {
        // 체크인/체크아웃 날짜 검증
        if (checkin != null && checkout != null && !checkout.isAfter(checkin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "체크아웃 날짜는 체크인 날짜 이후여야 합니다.");
        }
        LocalDateTime checkinAt = checkin != null ? checkin.atTime(15, 0) : null;
        LocalDateTime checkoutAt = checkout != null ? checkout.atTime(11, 0) : null;
        return searchService.searchPublicList(
                themeIds,
                keyword,
                page,
                size,
                minLat,
                maxLat,
                minLng,
                maxLng,
                checkinAt,
                checkoutAt,
                guestCount,
                minPrice,
                maxPrice,
                includeUnavailable,
                sort);
    }

    @GetMapping("/search/suggest")
    public List<SearchSuggestionResponse> suggest(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return searchService.suggestPublicSearch(keyword, limit);
    }

    @GetMapping("/search/resolve")
    public ResponseEntity<SearchResolveResponse> resolveAccommodation(
            @RequestParam(name = "keyword", required = false) String keyword) {
        SearchResolveResponse resolved = searchService.resolvePublicAccommodation(keyword);
        if (resolved == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(resolved);
    }
}
