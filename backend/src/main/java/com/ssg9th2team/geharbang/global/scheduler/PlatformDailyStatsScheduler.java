package com.ssg9th2team.geharbang.global.scheduler;

import com.ssg9th2team.geharbang.domain.admin.service.PlatformDailyStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PlatformDailyStatsScheduler {

    private final PlatformDailyStatsService statsService;

    @Scheduled(cron = "0 10 0 * * *")
    public void refreshYesterdayStats() {
        statsService.refreshDailyStats(LocalDate.now().minusDays(1));
    }
}
