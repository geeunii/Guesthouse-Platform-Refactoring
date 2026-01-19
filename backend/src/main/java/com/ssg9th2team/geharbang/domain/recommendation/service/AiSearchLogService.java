package com.ssg9th2team.geharbang.domain.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 추천 검색 로그를 Redis에 저장하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchLogService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SEARCH_LOGS_KEY = "ai:search:logs";
    private static final String THEME_POPULARITY_KEY = "ai:theme:popularity";
    private static final int MAX_LOG_SIZE = 1000;
    private static final long LOG_TTL_DAYS = 30;

    /**
     * 검색 로그 저장
     */
    public void logSearch(String query, List<String> matchedThemes, Double confidence) {
        try {
            Map<String, Object> logEntry = Map.of(
                    "query", query,
                    "themes", matchedThemes,
                    "confidence", confidence,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String logJson = objectMapper.writeValueAsString(logEntry);
            RList<String> logs = redissonClient.getList(SEARCH_LOGS_KEY);
            logs.add(0, logJson);

            // 최대 크기 초과 시 오래된 로그 삭제
            if (logs.size() > MAX_LOG_SIZE) {
                logs.trim(0, MAX_LOG_SIZE - 1);
            }

            // TTL 설정 (30일)
            logs.expire(LOG_TTL_DAYS, TimeUnit.DAYS);

            // 테마 인기도 증가
            incrementThemePopularity(matchedThemes);

            log.debug("AI 검색 로그 저장: query={}, themes={}", query, matchedThemes);
        } catch (Exception e) {
            log.warn("AI 검색 로그 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 테마 인기도 증가
     */
    private void incrementThemePopularity(List<String> themes) {
        RScoredSortedSet<String> popularity = redissonClient.getScoredSortedSet(THEME_POPULARITY_KEY);
        for (String theme : themes) {
            popularity.addScore(theme, 1);
        }
        popularity.expire(LOG_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 인기 테마 조회 (상위 N개)
     */
    public List<String> getPopularThemes(int limit) {
        RScoredSortedSet<String> popularity = redissonClient.getScoredSortedSet(THEME_POPULARITY_KEY);
        return popularity.valueRangeReversed(0, limit - 1).stream().toList();
    }

    /**
     * 최근 검색 로그 조회
     */
    public List<String> getRecentSearchLogs(int limit) {
        RList<String> logs = redissonClient.getList(SEARCH_LOGS_KEY);
        int size = Math.min(limit, logs.size());
        return logs.subList(0, size);
    }
}
