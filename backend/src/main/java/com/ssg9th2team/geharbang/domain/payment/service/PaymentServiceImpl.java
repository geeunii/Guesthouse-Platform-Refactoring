package com.ssg9th2team.geharbang.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmRequestDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.RefundPolicyResult;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService;
import com.ssg9th2team.geharbang.domain.reservation.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository paymentRefundRepository;
    private final RefundPolicyService refundPolicyService;
    private final ReservationJpaRepository reservationRepository;
    private final UserCouponService userCouponService;
    private final ObjectMapper objectMapper;
    private final WaitlistService waitlistService;

    @Value("${tosspayments.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    @Override
    @Transactional
    public PaymentConfirmResponseDto confirmPayment(PaymentConfirmRequestDto requestDto) {
        log.info("결제 승인 요청: orderId={}, amount={}", requestDto.orderId(), requestDto.amount());

        // orderId에서 reservationId 추출 (형식: ORDER_예약ID_타임스탬프)
        Long reservationId = extractReservationId(requestDto.orderId());

        // 예약 조회 및 검증
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

        // 금액 검증
        if (!reservation.getFinalPaymentAmount().equals(requestDto.amount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다");
        }

        // 토스페이먼츠 결제 승인 API 호출
        try {
            RestTemplate restTemplate = new RestTemplate();

            // UTF-8 인코딩 설정
            restTemplate.getMessageConverters()
                    .add(0, new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8));

            // Basic 인증 헤더 생성
            String encodedSecretKey = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            headers.setAcceptCharset(java.util.Collections.singletonList(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedSecretKey);

            // 요청 바디
            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", requestDto.paymentKey());
            body.put("orderId", requestDto.orderId());
            body.put("amount", requestDto.amount());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    TOSS_CONFIRM_URL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            log.info("토스페이먼츠 응답: status={}", response.getStatusCode());

            // 응답 파싱
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String paymentMethod = responseJson.has("method") ? responseJson.get("method").asText() : "CARD";
            String approvedAtStr = responseJson.has("approvedAt") ? responseJson.get("approvedAt").asText() : null;
            LocalDateTime approvedAt = approvedAtStr != null ? LocalDateTime.parse(approvedAtStr.substring(0, 19))
                    : LocalDateTime.now();

            // Payment 저장
            Payment payment = Payment.builder()
                    .reservationId(reservationId)
                    .pgProviderCode("TOSS")
                    .paymentMethod(paymentMethod)
                    .orderId(requestDto.orderId())
                    .pgPaymentKey(requestDto.paymentKey())
                    .requestAmount(requestDto.amount())
                    .approvedAmount(requestDto.amount())
                    .paymentStatus(1) // 성공
                    .approvedAt(approvedAt)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            // 예약 상태 업데이트 (예약확정, 결제완료)
            reservation.updatePaymentCompleted();
            reservationRepository.save(reservation);

            // 쿠폰 사용 처리
            if (reservation.getUserCouponId() != null) {
                userCouponService.useCoupon(reservation.getUserId(), reservation.getUserCouponId());
                log.info("쿠폰 사용 처리 완료: userCouponId={}", reservation.getUserCouponId());
            }

            // 첫 예약 완료 쿠폰 발급
            boolean firstReservationCouponIssued = userCouponService
                    .issueFirstReservationCoupon(reservation.getUserId());
            if (firstReservationCouponIssued) {
                log.info("첫 예약 쿠폰 발급 완료: userId={}", reservation.getUserId());
            }

            log.info("결제 승인 완료: paymentId={}, reservationId={}", savedPayment.getId(), reservationId);

            return PaymentConfirmResponseDto.of(PaymentResponseDto.from(savedPayment), firstReservationCouponIssued);

        } catch (Exception e) {
            log.error("결제 승인 중 오류 발생 (결제 취소 진행)", e);

            // 1. 이미 승인된 결제가 있다면 취소 (돈 환불)
            try {
                if (requestDto.paymentKey() != null) {
                    RestTemplate restTemplate = new RestTemplate();

                    String encodedSecretKey = Base64.getEncoder()
                            .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
                    headers.set("Authorization", "Basic " + encodedSecretKey);

                    Map<String, Object> body = new HashMap<>();
                    body.put("cancelReason", "시스템 오류로 인한 자동 취소: " + e.getMessage());

                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                    String cancelUrl = "https://api.tosspayments.com/v1/payments/" + requestDto.paymentKey()
                            + "/cancel";

                    restTemplate.exchange(cancelUrl, HttpMethod.POST, entity, String.class);
                    log.info("결제 자동 취소 성공: paymentKey={}", requestDto.paymentKey());
                }
            } catch (Exception cancelEx) {
                log.error("결제 자동 취소 실패! (수동 환불 필요) paymentKey={}", requestDto.paymentKey(), cancelEx);
            }

            // 2. DB 상태 업데이트 시도 (트랜잭션 롤백 상태면 실패할 수 있음 - 무시)
            try {
                Payment failedPayment = Payment.builder()
                        .reservationId(reservationId)
                        .pgProviderCode("TOSS")
                        .paymentMethod("UNKNOWN")
                        .orderId(requestDto.orderId())
                        .requestAmount(requestDto.amount())
                        .paymentStatus(2) // 실패
                        .failureMessage(e.getMessage())
                        .build();
                paymentRepository.save(failedPayment);

                reservation.updatePaymentFailed();
                reservationRepository.save(reservation);
            } catch (Exception dbEx) {
                log.warn("실패 내역 DB 저장 불가 (트랜잭션 롤백됨): {}", dbEx.getMessage());
            }

            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + orderId));
        return PaymentResponseDto.from(payment);
    }

    @Override
    public PaymentResponseDto getPaymentByReservationId(Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: reservationId=" + reservationId));
        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(Long reservationId, String cancelReason, Integer refundAmount) {
        log.info("결제 취소 요청: reservationId={}, refundAmount={}", reservationId, refundAmount);

        // 결제 정보 조회
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

        Integer reservationStatus = reservation.getReservationStatus();
        if (reservationStatus != null && reservationStatus == 9) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 취소된 예약입니다.");
        }
        if (reservationStatus != null && reservationStatus == 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "체크인 완료 상태에서는 취소할 수 없습니다.");
        }
        if (payment.getPaymentStatus() == null || payment.getPaymentStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 완료 상태에서만 취소할 수 있습니다.");
        }

        // 환불 금액 결정 (정책 적용)
        Integer approvedAmount = payment.getApprovedAmount() != null ? payment.getApprovedAmount()
                : payment.getRequestAmount();
        if (approvedAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 없습니다.");
        }
        RefundPolicyResult policyResult = refundPolicyService.calculate(
                reservation.getCheckin(),
                LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")),
                approvedAmount);
        Integer actualRefundAmount = policyResult.refundAmount();
        String normalizedReason = (cancelReason == null || cancelReason.trim().isEmpty())
                ? String.format("policy %s %d%%", policyResult.policyCode(), policyResult.refundRate())
                : cancelReason.trim();

        return processRefund(payment, reservation, actualRefundAmount, normalizedReason, approvedAmount);
    }

    @Override
    @Transactional
    public PaymentResponseDto adminRefundPayment(Long reservationId, String cancelReason, Integer refundAmount) {
        log.info("관리자 강제 환불 요청: reservationId={}, refundAmount={}", reservationId, refundAmount);

        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

        if (payment.getPaymentStatus() == null || payment.getPaymentStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 완료 상태에서만 환불할 수 있습니다.");
        }

        Integer approvedAmount = payment.getApprovedAmount() != null ? payment.getApprovedAmount()
                : payment.getRequestAmount();

        // 관리자가 입력한 금액 사용 (유효성 검사)
        if (refundAmount == null || refundAmount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "환불 금액이 유효하지 않습니다.");
        }
        if (refundAmount > approvedAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "환불 금액이 결제 금액보다 클 수 없습니다.");
        }

        String normalizedReason = (cancelReason == null || cancelReason.trim().isEmpty())
                ? "관리자 강제 환불"
                : cancelReason.trim();

        return processRefund(payment, reservation, refundAmount, normalizedReason, approvedAmount);
    }

    // 공통 환불 처리 로직
    private PaymentResponseDto processRefund(Payment payment, Reservation reservation, Integer actualRefundAmount,
            String reason, Integer approvedAmount) {
        // 환불 기록 생성 (요청 상태)
        PaymentRefund paymentRefund = PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundAmount(actualRefundAmount)
                .refundStatus(0) // 0: 요청
                .reasonMessage(reason)
                .requestedBy("SYSTEM") // or ADMIN/USER
                .build();

        if (actualRefundAmount == 0) {
            paymentRefund.updateRefundSuccess(payment.getPgPaymentKey(), LocalDateTime.now());
            paymentRefundRepository.save(paymentRefund);
            reservation.updateRefunded();
            reservationRepository.save(reservation);

            // 환불 금액이 0이어도 쿠폰은 복구
            if (reservation.getUserCouponId() != null) {
                userCouponService.restoreCoupon(reservation.getUserId(), reservation.getUserCouponId());
                log.info("쿠폰 복구 완료 (환불 불가 정책/0원 환불): userCouponId={}", reservation.getUserCouponId());
            }

            log.info("0원 환불 처리 완료: reservationId={}", reservation.getId());
        } else {
            // 토스페이먼츠 결제 취소 API 호출
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters()
                        .add(0, new org.springframework.http.converter.StringHttpMessageConverter(
                                StandardCharsets.UTF_8));

                String encodedSecretKey = Base64.getEncoder()
                        .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encodedSecretKey);

                Map<String, Object> body = new HashMap<>();
                body.put("cancelReason", reason);
                if (actualRefundAmount < approvedAmount) {
                    body.put("cancelAmount", actualRefundAmount); // 부분 취소
                }

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                String cancelUrl = "https://api.tosspayments.com/v1/payments/" + payment.getPgPaymentKey() + "/cancel";

                ResponseEntity<String> response = restTemplate.exchange(
                        cancelUrl,
                        HttpMethod.POST,
                        entity,
                        String.class);

                log.info("토스페이먼츠 취소 응답: status={}", response.getStatusCode());

                // 환불 성공 - payment_refund 테이블 업데이트
                paymentRefund.updateRefundSuccess(payment.getPgPaymentKey(), LocalDateTime.now());
                paymentRefundRepository.save(paymentRefund);

                // 예약 상태 업데이트 (취소/환불)
                reservation.updateRefunded();
                reservationRepository.save(reservation);

                // 쿠폰 복구 처리
                if (reservation.getUserCouponId() != null) {
                    userCouponService.restoreCoupon(reservation.getUserId(), reservation.getUserCouponId());
                    log.info("쿠폰 복구 완료: userCouponId={}", reservation.getUserCouponId());
                }

                log.info("환불 처리 완료: reservationId={}, refundAmount={}", reservation.getId(), actualRefundAmount);

            } catch (Exception e) {
                log.error("결제 취소 실패", e);

                // 환불 실패 - payment_refund 테이블 업데이트
                paymentRefund.updateRefundFailed("CANCEL_FAILED", e.getMessage());
                paymentRefundRepository.save(paymentRefund);

                throw new RuntimeException("환불 처리에 실패했습니다: " + e.getMessage());
            }
        }

        // 첫 예약 쿠폰 회수 (예약 취소로 인해 첫 예약이 아니게 됨) - 공통 처리
        userCouponService.revokeFirstReservationCoupon(reservation.getUserId());

        // 대기자에게 알림 발송 (빈자리 발생)
        try {
            waitlistService.notifyWaitingUsers(reservation.getRoomId(), reservation.getCheckin(),
                    reservation.getCheckout());
            log.info("취소/환불로 인한 대기자 알림 발송 요청 완료: roomId={}", reservation.getRoomId());
        } catch (Exception e) {
            log.warn("대기자 알림 발송 중 오류 (결제 취소는 성공): {}", e.getMessage());
        }

        return PaymentResponseDto.from(payment);
    }

    private Long extractReservationId(String orderId) {
        // 형식: ORDER_예약ID_타임스탬프
        try {
            String[] parts = orderId.split("_");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
            throw new IllegalArgumentException("잘못된 주문번호 형식입니다: " + orderId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("주문번호에서 예약 ID를 추출할 수 없습니다: " + orderId);
        }
    }

    @Override
    public java.util.List<com.ssg9th2team.geharbang.domain.payment.dto.RefundResponseDto> getAllRefunds() {
        return paymentRefundRepository.findAll().stream()
                .map(com.ssg9th2team.geharbang.domain.payment.dto.RefundResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllPaymentDataByReservationId(Long reservationId) {
        log.info("예약 ID로 결제 정보 삭제 시도: {}", reservationId);
        // 결제 정보 조회 (없을 수도 있음 - 대기 상태에서 취소 등)
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            // 환불 내역이 있으면 먼저 삭제
            paymentRefundRepository.deleteByPaymentId(payment.getId());
            // 결제 내역 삭제 (reservationId로 삭제)
            // paymentRepository.delete(payment); 로 해도 되지만, 명시적으로 deleteByReservationId 사용
            paymentRepository.deleteByReservationId(reservationId);
            log.info("결제 정보 및 환불 내역 삭제 완료: paymentId={}", payment.getId());
        });
    }
}
