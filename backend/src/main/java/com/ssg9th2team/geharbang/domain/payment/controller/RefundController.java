package com.ssg9th2team.geharbang.domain.payment.controller;

import com.ssg9th2team.geharbang.domain.payment.dto.RefundPolicyResult;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.RefundPolicyService;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReservationJpaRepository reservationRepository;
    private final PaymentJpaRepository paymentRepository;
    private final RefundPolicyService refundPolicyService;

    @GetMapping("/quote")
    public RefundPolicyResult getRefundQuote(@RequestParam Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElse(null);
        if (payment == null || payment.getPaymentStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "환불 계산 불가(미결제)");
        }
        if (payment.getPaymentStatus() == 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 환불 완료된 예약입니다.");
        }
        if (payment.getPaymentStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "환불 계산 불가(미결제)");
        }
        Integer approvedAmount = payment.getApprovedAmount() != null
                ? payment.getApprovedAmount()
                : payment.getRequestAmount();
        if (approvedAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 없습니다.");
        }
        return refundPolicyService.calculate(
                reservation.getCheckin(),
                LocalDateTime.now(KST),
                approvedAmount
        );
    }
}
