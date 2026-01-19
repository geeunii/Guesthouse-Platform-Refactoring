package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByPgPaymentKey(String pgPaymentKey);

    List<Payment> findByReservationIdIn(List<Long> reservationIds);

    void deleteByReservationIdIn(List<Long> reservationIds);

    void deleteByReservationId(Long reservationId);

    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(p.approvedAmount), 0)
            FROM Payment p
            WHERE p.paymentStatus = :status
              AND p.createdAt >= :start
              AND p.createdAt < :end
            """)
    Long sumApprovedAmountByStatusAndCreatedAtBetween(@Param("status") Integer status,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(p)
            FROM Payment p
            WHERE p.paymentStatus = :status
              AND p.createdAt >= :start
              AND p.createdAt < :end
            """)
    Long countByStatusAndCreatedAtBetween(@Param("status") Integer status,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
