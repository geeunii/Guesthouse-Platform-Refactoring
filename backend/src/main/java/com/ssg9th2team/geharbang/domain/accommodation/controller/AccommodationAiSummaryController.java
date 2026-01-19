package com.ssg9th2team.geharbang.domain.accommodation.controller;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationAiSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accommodations")
@RequiredArgsConstructor
public class AccommodationAiSummaryController {

    private final AccommodationAiSummaryService accommodationAiSummaryService;

    @GetMapping("/{accommodationId}/ai-summary")
    public ResponseEntity<AccommodationAiSummaryResponse> getAiSummary(
            @PathVariable Long accommodationId
    ) {
        AccommodationAiSummaryResponse response = accommodationAiSummaryService.generateSummary(accommodationId);
        return ResponseEntity.ok(response);
    }
}
