package com.ssg9th2team.geharbang.domain.accommodation.controller;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSuggestionRequest;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationAiSuggestionResponse;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationAiService;
import com.ssg9th2team.geharbang.domain.booking.host.support.HostIdentityResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/accommodations")
@RequiredArgsConstructor
public class AccommodationAiController {

    private final AccommodationAiService accommodationAiService;
    private final HostIdentityResolver hostIdentityResolver;

    @PostMapping("/naming")
    public ResponseEntity<AccommodationAiSuggestionResponse> suggestAccommodationNaming(
            @Valid @RequestBody AccommodationAiSuggestionRequest request,
            Authentication authentication
    ) {
        Long hostId = hostIdentityResolver.resolveHostUserId(authentication);
        AccommodationAiSuggestionResponse response = accommodationAiService.suggest(hostId, request);
        return ResponseEntity.ok(response);
    }
}
