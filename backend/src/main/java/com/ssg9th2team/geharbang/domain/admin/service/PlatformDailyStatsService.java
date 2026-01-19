package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.admin.entity.PlatformDailyStats;
import com.ssg9th2team.geharbang.domain.admin.repository.PlatformDailyStatsRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentRefundJpaRepository;
import com.ssg9th2team.geharbang.domain.report.repository.jpa.ReviewReportJpaRepository;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformDailyStatsService {

    private final PlatformDailyStatsRepository statsRepository;
    private final UserRepository userRepository;
    private final AccommodationJpaRepository accommodationRepository;
    private final ReservationJpaRepository reservationRepository;
    private final PaymentJpaRepository paymentRepository;
    private final PaymentRefundJpaRepository refundRepository;
    private final ReviewReportJpaRepository reportRepository;

    public PlatformDailyStats refreshDailyStats(LocalDate statDate) {
        LocalDateTime start = statDate.atStartOfDay();
        LocalDateTime end = statDate.plusDays(1).atStartOfDay();

        long totalHosts = userRepository.count(roleEquals(UserRole.HOST));
        long newHosts = userRepository.count(roleCreatedBetween(UserRole.HOST, start, end));

        long totalAccommodations = accommodationRepository.count();
        long newAccommodations = accommodationRepository.count(createdBetween(start, end));

        long totalReservations = reservationRepository.count(createdBetweenReservation(start, end));
        long reservationsSuccess = reservationRepository.count(paymentStatusBetween(1, start, end));
        long reservationsFailed = reservationRepository.count(paymentStatusInBetween(List.of(2, 3), start, end));
        long cancelCount = reservationRepository.count(reservationStatusBetween(9, start, end));

        List<Payment> payments = paymentRepository.findAll();
        long paymentSuccessAmount = payments.stream()
                .filter(payment -> isBetween(payment.getCreatedAt(), start, end))
                .filter(payment -> payment.getPaymentStatus() != null && payment.getPaymentStatus() == 1)
                .mapToLong(payment -> payment.getApprovedAmount() != null ? payment.getApprovedAmount() : 0L)
                .sum();

        List<PaymentRefund> refunds = refundRepository.findAll();
        long refundCount = refunds.stream()
                .filter(refund -> isBetween(refund.getCreatedAt(), start, end))
                .filter(refund -> refund.getRefundStatus() != null
                        && (refund.getRefundStatus() == 0 || refund.getRefundStatus() == 1))
                .count();
        long refundAmount = refunds.stream()
                .filter(refund -> isBetween(refund.getCreatedAt(), start, end))
                .filter(refund -> refund.getRefundStatus() != null && refund.getRefundStatus() == 1)
                .mapToLong(refund -> refund.getRefundAmount() != null ? refund.getRefundAmount() : 0L)
                .sum();

        long pendingAccommodations = accommodationRepository.count(approvalEquals(ApprovalStatus.PENDING));
        long openReports = reportRepository.count(reportStateEquals("WAIT"));

        long activeGuests = reservationRepository.countDistinctGuest(start, end);
        long activeHosts = reservationRepository.countDistinctHost(start, end);

        PlatformDailyStats stats = PlatformDailyStats.builder()
                .statDate(statDate)
                .totalHosts(totalHosts)
                .newHosts(newHosts)
                .totalAccommodations(totalAccommodations)
                .newAccommodations(newAccommodations)
                .totalReservations(totalReservations)
                .reservationsSuccess(reservationsSuccess)
                .reservationsFailed(reservationsFailed)
                .totalRevenue(paymentSuccessAmount)
                .cancelCount(cancelCount)
                .refundCount(refundCount)
                .refundAmount(refundAmount)
                .pendingAccommodations(pendingAccommodations)
                .openReports(openReports)
                .activeGuests(activeGuests)
                .activeHosts(activeHosts)
                .occupancyRate(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();

        return statsRepository.save(stats);
    }

    public List<PlatformDailyStats> getStats(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(6);
        LocalDate end = to != null ? to : LocalDate.now();
        return statsRepository.findByStatDateBetweenOrderByStatDateAsc(start, end);
    }

    private Specification<com.ssg9th2team.geharbang.domain.auth.entity.User> roleEquals(UserRole role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    private Specification<com.ssg9th2team.geharbang.domain.auth.entity.User> roleCreatedBetween(
            UserRole role, LocalDateTime start, LocalDateTime end
    ) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("role"), role),
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation> approvalEquals(ApprovalStatus status) {
        return (root, query, cb) -> cb.equal(root.get("approvalStatus"), status);
    }

    private Specification<com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation> createdBetween(
            LocalDateTime start, LocalDateTime end
    ) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<com.ssg9th2team.geharbang.domain.reservation.entity.Reservation> createdBetweenReservation(
            LocalDateTime start, LocalDateTime end
    ) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<com.ssg9th2team.geharbang.domain.reservation.entity.Reservation> paymentStatusBetween(
            Integer status, LocalDateTime start, LocalDateTime end
    ) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("paymentStatus"), status),
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<com.ssg9th2team.geharbang.domain.reservation.entity.Reservation> paymentStatusInBetween(
            List<Integer> statuses, LocalDateTime start, LocalDateTime end
    ) {
        return (root, query, cb) -> cb.and(
                root.get("paymentStatus").in(statuses),
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<com.ssg9th2team.geharbang.domain.reservation.entity.Reservation> reservationStatusBetween(
            Integer status, LocalDateTime start, LocalDateTime end
    ) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("reservationStatus"), status),
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    private Specification<com.ssg9th2team.geharbang.domain.report.entity.ReviewReport> reportStateEquals(String state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    private boolean isBetween(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) {
            return false;
        }
        return !value.isBefore(start) && value.isBefore(end);
    }
}
