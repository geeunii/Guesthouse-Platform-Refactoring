package com.ssg9th2team.geharbang.global.scheduler;

import com.ssg9th2team.geharbang.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 예약 관련 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationService reservationService;

    /**
     * 10분마다 오래된 대기 예약 삭제
     * - 생성 후 10분이 지난 대기(0) 상태 예약을 삭제
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10분마다
    public void cleanupOldPendingReservations() {
        try {
            int deletedCount = reservationService.cleanupOldPendingReservations();
            if (deletedCount > 0) {
                log.info("오래된 대기 예약 {}건 삭제 완료", deletedCount);
            }
        } catch (Exception e) {
            log.error("대기 예약 정리 중 오류 발생", e);
        }
    }
}
