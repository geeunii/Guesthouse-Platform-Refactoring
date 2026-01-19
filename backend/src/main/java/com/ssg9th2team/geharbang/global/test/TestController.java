package com.ssg9th2team.geharbang.global.test;

import com.ssg9th2team.geharbang.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
// @Profile("!prod") // 프로덕션에서는 비활성화 권장
public class TestController {

    private final ReservationService reservationService;

    @PostMapping("/trigger-cleanup")
    public ResponseEntity<String> triggerCleanup() {
        int count = reservationService.cleanupOldPendingReservations();
        return ResponseEntity.ok("Cleanup executed. Deleted count: " + count);
    }
}
