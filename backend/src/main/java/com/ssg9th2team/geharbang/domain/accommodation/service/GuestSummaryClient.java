package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;

import java.util.List;

public interface GuestSummaryClient {
    AccommodationAiSummaryResponse generate(Accommodation accommodation, List<ReviewEntity> reviews, List<String> topTags, int minPrice, boolean hasDormitory);
}
