package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestSummaryFacade {

    private final OpenAiGuestSummaryClient openAiClient;
    private final RuleBasedGuestSummaryClient ruleBasedClient;

    public AccommodationAiSummaryResponse generate(Accommodation accommodation, List<ReviewEntity> reviews, List<String> topTags, int minPrice, boolean hasDormitory) {
        // 1. OpenAI 시도
        if (openAiClient.isConfigured()) {
            try {
                log.info("Generating guest summary using OpenAI...");
                return openAiClient.generate(accommodation, reviews, topTags, minPrice, hasDormitory);
            } catch (GuestSummaryAiException e) { // 전용 예외를 명시적으로 잡음
                log.warn("OpenAI generation failed. Falling back to Rule-based logic.", e);
            }
        } else {
            log.info("OpenAI not configured. Using Rule-based logic.");
        }

        // 2. Fallback: Rule-based
        return ruleBasedClient.generate(accommodation, reviews, topTags, minPrice, hasDormitory);
    }
}
