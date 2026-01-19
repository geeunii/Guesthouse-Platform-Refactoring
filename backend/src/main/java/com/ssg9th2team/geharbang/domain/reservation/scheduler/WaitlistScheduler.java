package com.ssg9th2team.geharbang.domain.reservation.scheduler;

import com.ssg9th2team.geharbang.domain.reservation.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대기 목록 자동 정리 스케줄러
 * - 매일 자정: 체크인 지난 대기, 오래된 대기 삭제
 * - 매시간: 24시간 만료 알림 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WaitlistScheduler {

    private final WaitlistService waitlistService;

    /**
     * 매일 자정에 실행
     * - 체크인 날짜가 지난 대기 삭제
     * - 30일 이상 된 오래된 대기 삭제
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyCleanup() {
        log.info("=== 대기 목록 일일 정리 시작 ===");

        try {
            // 1. 체크인 지난 대기 삭제
            int pastCheckinDeleted = waitlistService.cleanupPastCheckinWaitlists();

            // 2. 오래된 대기 삭제
            int oldDeleted = waitlistService.cleanupOldWaitlists();

            log.info("=== 대기 목록 일일 정리 완료: 체크인 지난 {}건, 30일 이상 {}건 삭제 ===",
                    pastCheckinDeleted, oldDeleted);
        } catch (Exception e) {
            log.error("대기 목록 일일 정리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매시간 정각에 실행
     * - 24시간 만료된 알림 처리 (대기 삭제)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void hourlyExpiredNotificationCleanup() {
        log.debug("24시간 만료 알림 정리 시작");

        try {
            int expiredDeleted = waitlistService.processExpiredNotifications();

            if (expiredDeleted > 0) {
                log.info("24시간 만료 알림 정리 완료: {}건 삭제", expiredDeleted);
            }
        } catch (Exception e) {
            log.error("24시간 만료 알림 정리 실패: {}", e.getMessage(), e);
        }
    }
}
