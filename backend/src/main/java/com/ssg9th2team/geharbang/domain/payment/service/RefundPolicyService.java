package com.ssg9th2team.geharbang.domain.payment.service;

import com.ssg9th2team.geharbang.domain.payment.dto.RefundPolicyResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
public class RefundPolicyService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public RefundPolicyResult calculate(LocalDateTime checkinAt, LocalDateTime cancelAt, int paidAmount) {
        if (checkinAt == null || cancelAt == null) {
            return new RefundPolicyResult(0, 0, "D0_NOSHOW", 0);
        }
        LocalDate checkinDate = checkinAt.atZone(KST).toLocalDate();
        LocalDate cancelDate = cancelAt.atZone(KST).toLocalDate();
        long daysBefore = ChronoUnit.DAYS.between(cancelDate, checkinDate);

        int refundRate;
        String policyCode;
        if (daysBefore >= 7) {
            refundRate = 100;
            policyCode = "D7";
        } else if (daysBefore >= 5) {
            refundRate = 90;
            policyCode = "D6_5";
        } else if (daysBefore >= 3) {
            refundRate = 70;
            policyCode = "D4_3";
        } else if (daysBefore >= 1) {
            refundRate = 50;
            policyCode = "D2_1";
        } else {
            refundRate = 0;
            policyCode = "D0_NOSHOW";
        }

        long rawAmount = (long) paidAmount * refundRate;
        int refundAmount = (int) Math.floorDiv(rawAmount, 100);
        return new RefundPolicyResult(refundRate, refundAmount, policyCode, daysBefore);
    }
}
