package com.ssg9th2team.geharbang.domain.reservation.controller;

import com.ssg9th2team.geharbang.domain.reservation.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 대기 목록 등록
     */
    @PostMapping
    public ResponseEntity<?> registerWaitlist(@RequestBody WaitlistRequest request) {
        try {
            LocalDateTime checkin = Instant.parse(request.checkin()).atZone(KST).toLocalDateTime();
            LocalDateTime checkout = Instant.parse(request.checkout()).atZone(KST).toLocalDateTime();

            Long waitlistId = waitlistService.registerWaitlist(
                    request.roomId(),
                    request.accommodationsId(),
                    checkin,
                    checkout,
                    request.guestCount());
            return ResponseEntity.ok().body(new WaitlistResponse(waitlistId, "대기 등록되었습니다. 빈자리 발생 시 이메일로 알려드립니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 대기 목록 취소
     */
    @DeleteMapping("/{waitlistId}")
    public ResponseEntity<?> cancelWaitlist(@PathVariable Long waitlistId) {
        try {
            waitlistService.cancelWaitlist(waitlistId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Request/Response DTOs
    public record WaitlistRequest(
            Long roomId,
            Long accommodationsId,
            String checkin,
            String checkout,
            Integer guestCount) {
    }

    public record WaitlistResponse(Long waitlistId, String message) {
    }

    public record ErrorResponse(String error) {
    }
}
