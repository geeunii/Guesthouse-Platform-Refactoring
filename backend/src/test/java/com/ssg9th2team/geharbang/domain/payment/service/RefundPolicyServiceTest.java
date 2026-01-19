package com.ssg9th2team.geharbang.domain.payment.service;

import com.ssg9th2team.geharbang.domain.payment.dto.RefundPolicyResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyServiceTest {

    private final RefundPolicyService service = new RefundPolicyService();

    @Test
    void refundPolicyBoundaries() {
        LocalDateTime checkin = LocalDateTime.of(2025, 12, 30, 15, 0);
        int paidAmount = 1110000;

        assertPolicy(checkin.minusDays(7), checkin, paidAmount, 100, "D7", 7);
        assertPolicy(checkin.minusDays(6), checkin, paidAmount, 90, "D6_5", 6);
        assertPolicy(checkin.minusDays(5), checkin, paidAmount, 90, "D6_5", 5);
        assertPolicy(checkin.minusDays(4), checkin, paidAmount, 70, "D4_3", 4);
        assertPolicy(checkin.minusDays(3), checkin, paidAmount, 70, "D4_3", 3);
        assertPolicy(checkin.minusDays(2), checkin, paidAmount, 50, "D2_1", 2);
        assertPolicy(checkin.minusDays(1), checkin, paidAmount, 50, "D2_1", 1);
        assertPolicy(checkin, checkin, paidAmount, 0, "D0_NOSHOW", 0);
        assertPolicy(checkin.plusDays(1), checkin, paidAmount, 0, "D0_NOSHOW", -1);
    }

    private void assertPolicy(
            LocalDateTime cancelAt,
            LocalDateTime checkinAt,
            int paidAmount,
            int expectedRate,
            String expectedCode,
            long expectedDays
    ) {
        RefundPolicyResult result = service.calculate(checkinAt, cancelAt, paidAmount);
        assertThat(result.refundRate()).isEqualTo(expectedRate);
        assertThat(result.policyCode()).isEqualTo(expectedCode);
        assertThat(result.daysBefore()).isEqualTo(expectedDays);
        assertThat(result.refundAmount()).isEqualTo((int) Math.floor(paidAmount * expectedRate / 100.0));
    }
}
