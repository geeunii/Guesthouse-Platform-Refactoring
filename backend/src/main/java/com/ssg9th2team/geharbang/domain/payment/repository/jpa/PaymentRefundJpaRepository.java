package com.ssg9th2team.geharbang.domain.payment.repository.jpa;

import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRefundJpaRepository extends JpaRepository<PaymentRefund, Long>, JpaSpecificationExecutor<PaymentRefund> {

    List<PaymentRefund> findByPaymentId(Long paymentId);

    Optional<PaymentRefund> findByPgRefundKey(String pgRefundKey);

    @Modifying
    void deleteByPaymentIdIn(List<Long> paymentIds);

    void deleteByPaymentId(Long paymentId);

    List<PaymentRefund> findByRequestedAtBetween(LocalDateTime start, LocalDateTime end);

    List<PaymentRefund> findByPaymentIdIn(List<Long> paymentIds);

    @Query("""
            SELECT COALESCE(SUM(r.refundAmount), 0)
            FROM PaymentRefund r
            WHERE r.refundStatus = :status
              AND r.requestedAt >= :start
              AND r.requestedAt < :end
            """)
    Long sumRefundAmountByStatusAndRequestedAtBetween(@Param("status") Integer status,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(r)
            FROM PaymentRefund r
            WHERE r.refundStatus = :status
              AND r.requestedAt >= :start
              AND r.requestedAt < :end
            """)
    Long countByStatusAndRequestedAtBetween(@Param("status") Integer status,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
