package com.ssg9th2team.geharbang.domain.accommodation.controller;

import com.ssg9th2team.geharbang.domain.accommodation.dto.HostAccommodationSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/host/accommodation")
@RequiredArgsConstructor
public class HostAccommodationController {

    private final AccommodationMapper accommodationMapper;
    private final AccommodationService accommodationService;
    private final UserRepository userRepository;

    @GetMapping
    public List<HostAccommodationSummaryResponse> listHostAccommodations(Authentication authentication) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        return accommodationMapper.selectHostAccommodations(hostId);
    }

    @GetMapping("/{accommodationsId}")
    public ResponseEntity<HostAccommodationSummaryResponse> getHostAccommodation(
            @PathVariable Long accommodationsId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Long hostId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
        HostAccommodationSummaryResponse response = accommodationMapper
                .selectHostAccommodationById(hostId, accommodationsId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{accommodationsId}")
    public ResponseEntity<Void> deleteHostAccommodation(@PathVariable Long accommodationsId) {
        accommodationService.deleteAccommodation(accommodationsId);
        return ResponseEntity.noContent().build();
    }
}
