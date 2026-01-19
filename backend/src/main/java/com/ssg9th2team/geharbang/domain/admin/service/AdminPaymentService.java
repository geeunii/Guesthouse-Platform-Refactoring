package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentSummaryResponse;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentMetricsPoint;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPaymentMetricsRow;
import com.ssg9th2team.geharbang.domain.admin.log.AdminLogConstants;
import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminPaymentMetricsMapper;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.RefundPolicyResult;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.RefundPolicyService;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    @Value("${host.platform.fee-rate:0.04}")
    private double platformFeeRate;

    private static final String ERR_PAYMENT_NOT_FOUND = "결제 정보를 찾을 수 없습니다.";
    private static final String ERR_PAYMENT_ALREADY_REFUNDED = "이미 환불 완료된 결제입니다.";
    private static final String ERR_PAYMENT_NOT_PAID = "결제 완료 상태에서만 환불 가능합니다.";
    private static final String ERR_REFUND_AMOUNT_MISSING = "환불 금액이 없습니다.";
    private static final String ERR_REFUND_AMOUNT_EXCEEDS = "환불 금액이 결제 금액을 초과했습니다.";
    private static final String ERR_RESERVATION_ALREADY_REFUNDED = "이미 환불 완료된 예약입니다.";
    private static final String ERR_RESERVATION_NOT_REFUNDABLE = "완료 또는 취소된 예약은 환불할 수 없습니다.";
    private static final String ERR_RESERVATION_NOT_PAID = "결제 완료 상태에서만 환불 가능합니다.";
    private static final String ERR_CHECKIN_PASSED = "체크인 이후 환불은 불가합니다.";

    private final PaymentJpaRepository paymentRepository;
    private final AdminPaymentMetricsMapper paymentMetricsMapper;
    private final PaymentRefundJpaRepository paymentRefundRepository;
    private final ReservationJpaRepository reservationRepository;
    private final AdminLogService adminLogService;
    private final RefundPolicyService refundPolicyService;
    @PersistenceContext
    private EntityManager entityManager;

    public AdminPageResponse<AdminPaymentSummary> getPayments(
            String status,
            String type,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        // 체크: status/type/keyword 필터가 DB 페이징 결과와 totalElements에 반영되는지 확인.
        int safePage = Math.max(0, page);
        int safeSize = size > 0 ? Math.min(size, 50) : 20;
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Specification<Payment> spec = buildPaymentSpec(status, type, keyword, null, null);
        Page<Payment> pageResult = paymentRepository.findAll(spec, PageRequest.of(safePage, safeSize, sorting));
        List<Payment> pageItems = pageResult.getContent();
        Map<Long, Reservation> reservationMap = reservationRepository.findAllById(
                        pageItems.stream()
                                .map(Payment::getReservationId)
                                .filter(id -> id != null)
                                .toList())
                .stream()
                .collect(Collectors.toMap(Reservation::getId, reservation -> reservation));
        List<AdminPaymentSummary> items = pageItems.stream()
                .map(payment -> toSummary(payment, reservationMap.get(payment.getReservationId())))
                .toList();
        return AdminPageResponse.of(
                items,
                safePage,
                safeSize,
                (int) pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    /*
     * KPI definitions (Admin payments)
     * - Gross: Payment.paymentStatus=1 AND createdAt in [from, to) SUM(approvedAmount)
     * - Refund completed: PaymentRefund.refundStatus=1 AND requestedAt in [from, to) COUNT/SUM(refundAmount)
     * - Net: Gross - Refund completed amount
     * - Platform fee: Net * platformFeeRate
     * - Payment failure: Payment.paymentStatus=2 COUNT
     * - Refund request: PaymentRefund.refundStatus=0 COUNT
     */
    public AdminPaymentSummaryResponse getPaymentSummary(
            String status,
            String type,
            String keyword,
            String from,
            String to
    ) {
        LocalDate startDate = parseDateOrDefault(from, LocalDate.now().minusDays(29));
        LocalDate endDate = parseDateOrDefault(to, LocalDate.now());
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Integer statusCode = parseStatus(status);
        String typeMode = parseType(type);
        Specification<Payment> basePaymentSpec = buildPaymentSpec(status, type, keyword, start, end);

        long grossAmount = sumPayments(basePaymentSpec.and(statusEquals(1)));
        long successCount = countPayments(basePaymentSpec.and(statusEquals(1)));
        long failureCount = countPayments(basePaymentSpec.and(statusEquals(2)));

        Specification<PaymentRefund> baseRefundSpec = buildRefundSpec(statusCode, typeMode, keyword, start, end);
        long refundRequestCount = countRefunds(baseRefundSpec.and(refundStatusEquals(0)));
        long refundCompletedCount = countRefunds(baseRefundSpec.and(refundStatusEquals(1)));
        long refundCompletedAmount = sumRefunds(baseRefundSpec.and(refundStatusEquals(1)));
        long netAmount = grossAmount - refundCompletedAmount;
        long platformFeeAmount = Math.round(netAmount * platformFeeRate);

        return new AdminPaymentSummaryResponse(
                startDate,
                endDate,
                grossAmount,
                refundCompletedAmount,
                netAmount,
                successCount,
                failureCount,
                refundRequestCount,
                refundCompletedCount,
                platformFeeRate,
                platformFeeAmount
        );
    }

    private Specification<Payment> buildPaymentSpec(String status, String type, String keyword,
                                                    LocalDateTime start, LocalDateTime end) {
        Integer statusCode = parseStatus(status);
        String typeMode = parseType(type);
        Specification<Payment> spec = Specification.where(keywordContains(keyword));
        if (start != null && end != null) {
            spec = spec.and(createdBetween(start, end));
        }
        if (statusCode != null) {
            if (statusCode == 3) {
                spec = spec.and(refundedStatus());
            } else {
                spec = spec.and(statusEquals(statusCode));
            }
        }
        if (typeMode != null) {
            if ("REFUND".equals(typeMode)) {
                spec = spec.and(refundExists());
            } else if ("RESERVATION".equals(typeMode)) {
                spec = spec.and(Specification.not(refundExists()));
            }
        }
        return spec;
    }

    private Specification<PaymentRefund> buildRefundSpec(Integer statusCode, String typeMode, String keyword,
                                                         LocalDateTime start, LocalDateTime end) {
        if ("RESERVATION".equals(typeMode)) {
            return alwaysFalseRefund();
        }
        if (statusCode != null && statusCode != 3) {
            return alwaysFalseRefund();
        }
        Specification<PaymentRefund> spec = Specification.where(refundKeywordContains(keyword));
        if (start != null && end != null) {
            spec = spec.and(requestedBetween(start, end));
        }
        if ("REFUND".equals(typeMode)) {
            spec = spec.and(noOpRefundSpec());
        }
        if (statusCode != null && statusCode == 3) {
            spec = spec.and(refundStatusEquals(1));
        }
        return spec;
    }

    private Specification<Payment> statusEquals(Integer status) {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentSpecifications.statusEquals(status);
    }

    private Specification<Payment> refundedStatus() {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentSpecifications.statusEquals(3)
                .or(com.ssg9th2team.geharbang.domain.payment.spec.PaymentSpecifications.refundedExists(1));
    }

    private Specification<Payment> refundExists() {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentSpecifications.refundedExists(1);
    }

    private Specification<Payment> keywordContains(String keyword) {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentSpecifications.keywordContains(keyword);
    }

    private Specification<Payment> createdBetween(LocalDateTime start, LocalDateTime end) {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentSpecifications.createdBetween(start, end);
    }

    private Specification<PaymentRefund> refundStatusEquals(Integer status) {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentRefundSpecifications.statusEquals(status);
    }

    private Specification<PaymentRefund> requestedBetween(LocalDateTime start, LocalDateTime end) {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentRefundSpecifications.requestedBetween(start, end);
    }

    private Specification<PaymentRefund> refundKeywordContains(String keyword) {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentRefundSpecifications.keywordContains(keyword);
    }

    private Specification<PaymentRefund> alwaysFalseRefund() {
        return com.ssg9th2team.geharbang.domain.payment.spec.PaymentRefundSpecifications.alwaysFalse();
    }

    private Specification<PaymentRefund> noOpRefundSpec() {
        return Specification.where(null);
    }

    private long sumPayments(Specification<Payment> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Payment> root = query.from(Payment.class);
        query.select(cb.coalesce(cb.sum(root.get("approvedAmount")).as(Long.class), 0L));
        if (spec != null) {
            query.where(spec.toPredicate(root, query, cb));
        }
        return Optional.ofNullable(entityManager.createQuery(query).getSingleResult()).orElse(0L);
    }

    private long countPayments(Specification<Payment> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Payment> root = query.from(Payment.class);
        query.select(cb.count(root));
        if (spec != null) {
            query.where(spec.toPredicate(root, query, cb));
        }
        return Optional.ofNullable(entityManager.createQuery(query).getSingleResult()).orElse(0L);
    }

    private long sumRefunds(Specification<PaymentRefund> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<PaymentRefund> root = query.from(PaymentRefund.class);
        query.select(cb.coalesce(cb.sum(root.get("refundAmount")).as(Long.class), 0L));
        if (spec != null) {
            query.where(spec.toPredicate(root, query, cb));
        }
        return Optional.ofNullable(entityManager.createQuery(query).getSingleResult()).orElse(0L);
    }

    private long countRefunds(Specification<PaymentRefund> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<PaymentRefund> root = query.from(PaymentRefund.class);
        query.select(cb.count(root));
        if (spec != null) {
            query.where(spec.toPredicate(root, query, cb));
        }
        return Optional.ofNullable(entityManager.createQuery(query).getSingleResult()).orElse(0L);
    }

    public AdminPaymentDetail getPaymentDetail(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_PAYMENT_NOT_FOUND));
        return toDetail(payment);
    }

    @Transactional
    public PaymentResponseDto refundPayment(Long adminUserId, Long paymentId, String reason, Integer amount, Boolean override) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_PAYMENT_NOT_FOUND));
        Integer beforePaymentStatus = payment.getPaymentStatus();
        boolean hasRefund = paymentRefundRepository.findByPaymentId(paymentId).stream()
                .anyMatch(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() != 2);
        if (hasRefund) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_PAYMENT_ALREADY_REFUNDED);
        }
        if (payment.getPaymentStatus() != null && payment.getPaymentStatus() == 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_PAYMENT_ALREADY_REFUNDED);
        }
        if (payment.getPaymentStatus() == null || payment.getPaymentStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_PAYMENT_NOT_PAID);
        }
        Reservation reservation = reservationRepository.findById(payment.getReservationId()).orElse(null);
        Integer beforeReservationStatus = reservation != null ? reservation.getReservationStatus() : null;
        Integer beforeReservationPaymentStatus = reservation != null ? reservation.getPaymentStatus() : null;
        validateRefundEligibility(reservation);
        Integer baseAmount = payment.getApprovedAmount() != null
                ? payment.getApprovedAmount()
                : payment.getRequestAmount();
        if (baseAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_REFUND_AMOUNT_MISSING);
        }

        RefundPolicyResult policyResult = reservation != null
                ? refundPolicyService.calculate(reservation.getCheckin(), LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")), baseAmount)
                : null;
        Integer policyAmount = policyResult != null ? policyResult.refundAmount() : baseAmount;

        boolean overrideApplied = Boolean.TRUE.equals(override) || amount != null;
        if (overrideApplied && !StringUtils.hasText(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "override 사유를 입력해주세요.");
        }
        Integer refundAmount = overrideApplied ? amount : policyAmount;
        if (refundAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_REFUND_AMOUNT_MISSING);
        }
        if (refundAmount > baseAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_REFUND_AMOUNT_EXCEEDS);
        }

        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        String policyNote = policyResult != null
                ? String.format("policy %s %d%%", policyResult.policyCode(), policyResult.refundRate())
                : null;
        String logReason = normalizedReason;
        if (overrideApplied) {
            logReason = String.format("override: %s | %s", normalizedReason, policyNote != null ? policyNote : "policy N/A");
        } else if (!StringUtils.hasText(logReason)) {
            logReason = policyNote;
        }

        PaymentRefund refund = PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundAmount(refundAmount)
                .refundStatus(1)
                .reasonMessage(logReason)
                .requestedBy("ADMIN")
                .approvedAt(LocalDateTime.now())
                .build();
        paymentRefundRepository.save(refund);

        entityManager.createQuery(
                        "UPDATE Payment p SET p.paymentStatus = :status, p.updatedAt = :updatedAt WHERE p.id = :id")
                .setParameter("status", 3)
                .setParameter("updatedAt", LocalDateTime.now())
                .setParameter("id", paymentId)
                .executeUpdate();

        if (reservation != null) {
            entityManager.createQuery(
                            "UPDATE Reservation r SET r.reservationStatus = :status, r.paymentStatus = :paymentStatus, r.updatedAt = :updatedAt WHERE r.id = :id")
                    .setParameter("status", 9)
                    .setParameter("paymentStatus", 3)
                    .setParameter("updatedAt", LocalDateTime.now())
                    .setParameter("id", reservation.getId())
                    .executeUpdate();
        }

        entityManager.clear();
        Payment updated = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_PAYMENT_NOT_FOUND));
        java.util.Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        java.util.Map<String, Object> before = new java.util.LinkedHashMap<>();
        before.put("paymentStatus", beforePaymentStatus);
        before.put("reservationStatus", beforeReservationStatus);
        before.put("reservationPaymentStatus", beforeReservationPaymentStatus);
        metadata.put("before", before);
        java.util.Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("paymentStatus", updated.getPaymentStatus());
        if (reservation != null) {
            after.put("reservationStatus", 9);
            after.put("reservationPaymentStatus", 3);
        }
        metadata.put("after", after);
        metadata.put("refundAmount", refundAmount);
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.TARGET_PAYMENT,
                paymentId,
                AdminLogConstants.ACTION_REFUND,
                logReason,
                metadata
        );
        return PaymentResponseDto.from(updated);
    }

    public List<AdminPaymentMetricsPoint> getMetrics(
            String mode,
            Integer year,
            String status,
            String type,
            String keyword
    ) {
        Integer statusCode = parseStatus(status);
        String typeMode = parseType(type);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        if ("REFUND".equals(typeMode) || (statusCode != null && statusCode == 3)) {
            return buildRefundMetrics(mode, year, normalizedKeyword);
        }

        if ("monthly".equalsIgnoreCase(mode)) {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            LocalDateTime from = LocalDate.of(targetYear, 1, 1).atStartOfDay();
            LocalDateTime to = LocalDate.of(targetYear, 12, 31).atTime(23, 59, 59);
            List<AdminPaymentMetricsRow> raw = paymentMetricsMapper.selectMonthlyMetrics(from, to, statusCode, typeMode, normalizedKeyword);
            Map<Integer, AdminPaymentMetricsRow> byMonth = raw.stream()
                    .filter(row -> row.period() != null)
                    .collect(Collectors.toMap(AdminPaymentMetricsRow::period, row -> row));
            return buildMonthlyPoints(targetYear, byMonth);
        }

        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 4;
        LocalDateTime from = LocalDate.of(startYear, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59);
        List<AdminPaymentMetricsRow> raw = paymentMetricsMapper.selectYearlyMetrics(from, to, statusCode, typeMode, normalizedKeyword);
        Map<Integer, AdminPaymentMetricsRow> byYear = raw.stream()
                .filter(row -> row.period() != null)
                .collect(Collectors.toMap(AdminPaymentMetricsRow::period, row -> row));
        return buildYearlyPoints(startYear, currentYear, byYear);
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "success", "completed" -> 1;
            case "failed", "fail" -> 2;
            case "refunded", "refund" -> 3;
            default -> null;
        };
    }

    private String parseType(String type) {
        if (!StringUtils.hasText(type) || "all".equalsIgnoreCase(type)) {
            return null;
        }
        String normalized = type.trim().toLowerCase();
        if ("refund".equals(normalized) || "refunded".equals(normalized) || "refund".equalsIgnoreCase(type)) {
            return "REFUND";
        }
        if ("reservation".equals(normalized) || "reserve".equals(normalized)) {
            return "RESERVATION";
        }
        return null;
    }

    private boolean matchesStatus(Payment payment, String status, Map<Long, PaymentRefund> refundMap) {
        Integer statusCode = parseStatus(status);
        if (statusCode == null) {
            return true;
        }
        if (statusCode == 3) {
            return statusCode.equals(payment.getPaymentStatus()) || refundMap.containsKey(payment.getId());
        }
        return statusCode.equals(payment.getPaymentStatus());
    }

    private boolean matchesType(Payment payment, String type, Map<Long, PaymentRefund> refundMap) {
        String typeMode = parseType(type);
        if (typeMode == null) {
            return true;
        }
        if ("REFUND".equals(typeMode)) {
            return refundMap.containsKey(payment.getId());
        }
        if ("RESERVATION".equals(typeMode)) {
            return !refundMap.containsKey(payment.getId());
        }
        return true;
    }

    private boolean matchesKeyword(Payment payment, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase();
        String orderId = payment.getOrderId() != null ? payment.getOrderId().toLowerCase() : "";
        String pgPaymentKey = payment.getPgPaymentKey() != null ? payment.getPgPaymentKey().toLowerCase() : "";
        return orderId.contains(normalized) || pgPaymentKey.contains(normalized);
    }

    private boolean matchesRefundStatus(PaymentRefund refund, String status) {
        Integer statusCode = parseStatus(status);
        if (statusCode == null) {
            return true;
        }
        if (statusCode == 3) {
            return refund.getRefundStatus() != null && refund.getRefundStatus() == 1;
        }
        return false;
    }

    private boolean matchesRefundType(PaymentRefund refund, String type) {
        String typeMode = parseType(type);
        if (typeMode == null) {
            return true;
        }
        return "REFUND".equals(typeMode);
    }

    private boolean matchesRefundKeyword(PaymentRefund refund, String keyword, Map<Long, Payment> paymentMap) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        Payment payment = refund != null ? paymentMap.get(refund.getPaymentId()) : null;
        if (payment == null) {
            return false;
        }
        return matchesKeyword(payment, keyword);
    }

    private AdminPaymentSummary toSummary(Payment payment, Reservation reservation) {
        return new AdminPaymentSummary(
                payment.getId(),
                payment.getReservationId(),
                payment.getOrderId(),
                payment.getPgPaymentKey(),
                payment.getApprovedAmount(),
                payment.getPaymentStatus(),
                reservation != null ? reservation.getReservationStatus() : null,
                reservation != null ? reservation.getCheckin() : null,
                payment.getCreatedAt()
        );
    }

    private AdminPaymentDetail toDetail(Payment payment) {
        Reservation reservation = reservationRepository.findById(payment.getReservationId()).orElse(null);
        PaymentRefund refund = paymentRefundRepository.findByPaymentId(payment.getId()).stream()
                .max(Comparator.comparing(PaymentRefund::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        return new AdminPaymentDetail(
                payment.getId(),
                payment.getReservationId(),
                reservation != null ? reservation.getAccommodationsId() : null,
                reservation != null ? reservation.getUserId() : null,
                reservation != null ? reservation.getReservationStatus() : null,
                payment.getOrderId(),
                payment.getPgPaymentKey(),
                payment.getRequestAmount(),
                payment.getApprovedAmount(),
                payment.getPaymentStatus(),
                payment.getApprovedAt(),
                refund != null ? refund.getRefundStatus() : null,
                refund != null ? refund.getRefundAmount() : null,
                refund != null ? refund.getApprovedAt() : null,
                refund != null ? refund.getReasonMessage() : null,
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    /*
     * Allowed refund state (reservation 기준)
     * - reservation_status: 2(확정) / 3(체크인 완료) / 9(취소)
     * - reservation_payment_status: 0(미결제) / 1(결제 완료) / 2(실패) / 3(환불 완료)
     * - refund_status: 1(완료) 상태는 이미 환불된 것으로 간주
     * - checkin: 현재 시각 이후만 환불 허용
     * Invalid 조합은 409로 차단.
     */
    private void validateRefundEligibility(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        Integer reservationStatus = reservation.getReservationStatus();
        Integer reservationPaymentStatus = reservation.getPaymentStatus();
        if (reservationStatus != null && (reservationStatus == 3 || reservationStatus == 9)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_RESERVATION_NOT_REFUNDABLE);
        }
        if (reservationPaymentStatus != null && reservationPaymentStatus == 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_RESERVATION_ALREADY_REFUNDED);
        }
        if (reservationPaymentStatus != null && reservationPaymentStatus != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_RESERVATION_NOT_PAID);
        }
        if (reservation.getCheckin() != null && !reservation.getCheckin().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ERR_CHECKIN_PASSED);
        }
    }

    private List<AdminPaymentMetricsPoint> buildMonthlyPoints(int year, Map<Integer, AdminPaymentMetricsRow> byMonth) {
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    AdminPaymentMetricsRow row = byMonth.get(month);
                    long totalAmount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalAmount).orElse(0L);
                    long totalCount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalCount).orElse(0L);
                    return new AdminPaymentMetricsPoint(String.format("%d월", month), totalAmount, totalCount);
                })
                .toList();
    }

    private List<AdminPaymentMetricsPoint> buildYearlyPoints(int startYear, int endYear, Map<Integer, AdminPaymentMetricsRow> byYear) {
        return java.util.stream.IntStream.rangeClosed(startYear, endYear)
                .mapToObj(year -> {
                    AdminPaymentMetricsRow row = byYear.get(year);
                    long totalAmount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalAmount).orElse(0L);
                    long totalCount = Optional.ofNullable(row).map(AdminPaymentMetricsRow::totalCount).orElse(0L);
                    return new AdminPaymentMetricsPoint(String.valueOf(year), totalAmount, totalCount);
                })
                .toList();
    }

    private Map<Long, PaymentRefund> buildRefundMap(List<Payment> payments) {
        List<Long> paymentIds = payments.stream()
                .map(Payment::getId)
                .filter(id -> id != null)
                .toList();
        if (paymentIds.isEmpty()) {
            return Map.of();
        }
        return paymentRefundRepository.findByPaymentIdIn(paymentIds).stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .collect(Collectors.toMap(PaymentRefund::getPaymentId, refund -> refund, (a, b) -> a));
    }

    private List<AdminPaymentMetricsPoint> buildRefundMetrics(String mode, Integer year, String keyword) {
        if ("monthly".equalsIgnoreCase(mode)) {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            LocalDateTime from = LocalDate.of(targetYear, 1, 1).atStartOfDay();
            LocalDateTime to = LocalDate.of(targetYear, 12, 31).atTime(23, 59, 59);
            return buildRefundMonthlyPoints(targetYear, from, to, keyword);
        }
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 4;
        LocalDateTime from = LocalDate.of(startYear, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59);
        return buildRefundYearlyPoints(startYear, currentYear, from, to, keyword);
    }

    private List<AdminPaymentMetricsPoint> buildRefundMonthlyPoints(int year, LocalDateTime from, LocalDateTime to, String keyword) {
        Map<Integer, AdminPaymentMetricsRow> byMonth = new HashMap<>();
        List<PaymentRefund> refunds = paymentRefundRepository.findByRequestedAtBetween(from, to);
        Map<Long, Payment> paymentMap = loadPaymentMap(refunds);
        refunds.stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .filter(refund -> matchesRefundKeyword(refund, keyword, paymentMap))
                .forEach(refund -> {
                    int month = refund.getRequestedAt() != null ? refund.getRequestedAt().getMonthValue() : 0;
                    if (month == 0) return;
                    AdminPaymentMetricsRow current = byMonth.get(month);
                    long amount = refund.getRefundAmount() != null ? refund.getRefundAmount() : 0L;
                    if (current == null) {
                        byMonth.put(month, new AdminPaymentMetricsRow(month, amount, 1L));
                    } else {
                        byMonth.put(month, new AdminPaymentMetricsRow(
                                month,
                                current.totalAmount() + amount,
                                current.totalCount() + 1
                        ));
                    }
                });
        return buildMonthlyPoints(year, byMonth);
    }

    private List<AdminPaymentMetricsPoint> buildRefundYearlyPoints(int startYear, int endYear, LocalDateTime from, LocalDateTime to, String keyword) {
        Map<Integer, AdminPaymentMetricsRow> byYear = new HashMap<>();
        List<PaymentRefund> refunds = paymentRefundRepository.findByRequestedAtBetween(from, to);
        Map<Long, Payment> paymentMap = loadPaymentMap(refunds);
        refunds.stream()
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .filter(refund -> matchesRefundKeyword(refund, keyword, paymentMap))
                .forEach(refund -> {
                    int year = refund.getRequestedAt() != null ? refund.getRequestedAt().getYear() : 0;
                    if (year == 0) return;
                    AdminPaymentMetricsRow current = byYear.get(year);
                    long amount = refund.getRefundAmount() != null ? refund.getRefundAmount() : 0L;
                    if (current == null) {
                        byYear.put(year, new AdminPaymentMetricsRow(year, amount, 1L));
                    } else {
                        byYear.put(year, new AdminPaymentMetricsRow(
                                year,
                                current.totalAmount() + amount,
                                current.totalCount() + 1
                        ));
                    }
                });
        return buildYearlyPoints(startYear, endYear, byYear);
    }

    private Map<Long, Payment> loadPaymentMap(List<PaymentRefund> refunds) {
        List<Long> paymentIds = refunds.stream()
                .map(PaymentRefund::getPaymentId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (paymentIds.isEmpty()) {
            return Map.of();
        }
        return paymentRepository.findAllById(paymentIds).stream()
                .collect(Collectors.toMap(Payment::getId, payment -> payment, (a, b) -> a));
    }

    private LocalDate parseDateOrDefault(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }
}
