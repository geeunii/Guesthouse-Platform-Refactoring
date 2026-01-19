package com.ssg9th2team.geharbang.domain.dashboard.host.service;

import com.ssg9th2team.geharbang.domain.dashboard.host.dto.HostDashboardSummaryResponse;
import com.ssg9th2team.geharbang.domain.dashboard.host.dto.TodayScheduleItemResponse;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(statements = {
        "DELETE FROM reservation WHERE reservation_id IN (9200001, 9200002)",
        "DELETE FROM host_daily_stats WHERE user_id = 99001",
        "DELETE FROM accommodation WHERE accommodations_id IN (9100001, 9100002, 9100003)",
        "DELETE FROM users WHERE user_id IN (9000001, 99001)",
        "DELETE FROM account_number WHERE account_number_id = 99001",
        "INSERT IGNORE INTO account_number (account_number_id, bank_name, account_number, account_holder) " +
                "VALUES (99001, '테스트은행', '000-0000-0000', '테스트계좌')",
        "INSERT INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved) " +
                "VALUES (9000001, 'test-user-9000001@example.com', NULL, '010-0000-0001', 'USER', 0, " +
                "'2025-01-01 00:00:00', '2025-01-01 00:00:00', NULL)",
        "INSERT INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved) " +
                "VALUES (99001, 'test-host-99001@example.com', NULL, '010-0000-9901', 'HOST', 0, " +
                "'2025-01-01 00:00:00', '2025-01-01 00:00:00', 1)",
        "INSERT INTO accommodation (accommodations_id, account_number_id, user_id, accommodations_name, accommodations_category, accommodation_status, approval_status, created_at) " +
                "VALUES " +
                "(9100001, 99001, 99001, 'Host A', 'GUESTHOUSE', 1, 'APPROVED', '2025-01-01 00:00:00'), " +
                "(9100002, 99001, 99001, 'Host B', 'GUESTHOUSE', 1, 'APPROVED', '2025-01-01 00:00:00'), " +
                "(9100003, 99001, 99001, 'Host C', 'GUESTHOUSE', 0, 'APPROVED', '2025-01-01 00:00:00')",
        "INSERT INTO host_daily_stats (stat_date, user_id, reservation_count, reserved_nights, total_guests, revenue, " +
                "canceled_count, avg_price, review_count, avg_rating, occupancy_rate, created_at, updated_at) " +
                "VALUES " +
                "('2025-01-05', 99001, 2, 2, 4, 1000, 0, 500, 1, 4.5, 50.0, '2025-01-06 00:00:00', '2025-01-06 00:00:00'), " +
                "('2025-01-15', 99001, 3, 3, 6, 2000, 1, 700, 2, 3.5, 60.0, '2025-01-16 00:00:00', '2025-01-16 00:00:00')",
        "INSERT INTO reservation (reservation_id, accommodations_id, user_id, checkin, checkout, stay_nights, guest_count, " +
                "reservation_status, total_amount_before_dc, coupon_discount_amount, final_payment_amount, payment_status, " +
                "reserver_name, reserver_phone, created_at, updated_at) " +
                "VALUES " +
                "(9200001, 9100001, 9000001, '2025-01-10 15:00:00', '2025-01-11 11:00:00', 1, 2, 2, 100000, 0, 100000, 1, " +
                "'guest-one', '010-0000-0001', '2025-01-01 00:00:00', '2025-01-01 00:00:00'), " +
                "(9200002, 9100002, 9000001, '2025-01-09 15:00:00', '2025-01-10 11:00:00', 1, 2, 3, 120000, 0, 120000, 1, " +
                "'guest-two', '010-0000-0002', '2025-01-01 00:00:00', '2025-01-01 00:00:00')"
})
@Disabled("TODO: SQL 스크립트 오류 수정 필요 - nickname 컬럼 누락")
class HostDashboardServiceTest {

    private static final Long HOST_ID = 99001L;

    @Autowired
    private HostDashboardService hostDashboardService;

    @MockBean
    private ObjectStorageService objectStorageService;

    @Test
    @DisplayName("Host dashboard summary aggregates stats and operating accommodations")
    void getSummaryAggregatesStats() {
        HostDashboardSummaryResponse response = hostDashboardService.getSummary(HOST_ID, 2025, 1);

        assertThat(response.getConfirmedRevenue()).isEqualTo(3000L);
        assertThat(response.getConfirmedReservations()).isEqualTo(5);
        assertThat(response.getAvgRating()).isCloseTo(4.0, within(0.01));
        assertThat(response.getOperatingAccommodations()).isEqualTo(2);
        assertThat(response.getTotalAccommodations()).isEqualTo(3);
    }

    @Test
    @DisplayName("Host dashboard today schedule returns ordered check-in and check-out items")
    void getTodayScheduleReturnsOrderedItems() {
        LocalDate date = LocalDate.of(2025, 1, 10);

        List<TodayScheduleItemResponse> items = hostDashboardService.getTodaySchedule(HOST_ID, date);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getType()).isEqualTo("CHECKIN");
        assertThat(items.get(0).getReservationId()).isEqualTo(9200001L);
        assertThat(items.get(0).getAccommodationName()).isEqualTo("Host A");

        assertThat(items.get(1).getType()).isEqualTo("CHECKOUT");
        assertThat(items.get(1).getReservationId()).isEqualTo(9200002L);
        assertThat(items.get(1).getAccommodationName()).isEqualTo("Host B");
    }
}
