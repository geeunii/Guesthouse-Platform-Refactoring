package com.ssg9th2team.geharbang.domain.accommodation.controller;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationUpdateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationService;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final UserRepository userRepository;

    // 숙소 등록
    @PostMapping
    public ResponseEntity<?> createAccommodation(@Valid @RequestBody AccommodationCreateRequestDto requestDto, Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"))
                    .getId();
            Long accommodationsId = accommodationService.createAccommodation(userId, requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(accommodationsId);
        } catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 에러 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }


    // 숙소 상세조회
    @GetMapping("/{accommodationsId}")
    public ResponseEntity<?> getAccommodation(@PathVariable Long accommodationsId) {
        try {
            AccommodationResponseDto response = accommodationService.getAccommodation(accommodationsId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // 숙소 수정
    @PutMapping("/{accommodationsId}")
    public ResponseEntity<?> updateAccommodation(
            @PathVariable Long accommodationsId,
            @RequestBody AccommodationUpdateRequestDto requestDto) {
        try {
            accommodationService.updateAccommodation(accommodationsId, requestDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // 숙소 삭제
    @DeleteMapping("/{accommodationsId}")
    public ResponseEntity<?> deleteAccommodation(@PathVariable Long accommodationsId) {
        try {
            accommodationService.deleteAccommodation(accommodationsId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
