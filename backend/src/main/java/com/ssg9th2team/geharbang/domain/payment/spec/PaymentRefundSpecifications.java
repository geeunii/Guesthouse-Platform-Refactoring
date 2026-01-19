package com.ssg9th2team.geharbang.domain.payment.spec;

import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public final class PaymentRefundSpecifications {

    private PaymentRefundSpecifications() {
    }

    public static Specification<PaymentRefund> requestedBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("requestedAt"), start),
                cb.lessThan(root.get("requestedAt"), end)
        );
    }

    public static Specification<PaymentRefund> statusEquals(Integer status) {
        if (status == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("refundStatus"), status);
    }

    public static Specification<PaymentRefund> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.where(null);
        }
        String normalized = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var payment = subquery.from(Payment.class);
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(payment.get("id"), root.get("paymentId")),
                    cb.or(
                            cb.like(cb.lower(payment.get("orderId")), normalized),
                            cb.like(cb.lower(payment.get("pgPaymentKey")), normalized)
                    )
            );
            return cb.exists(subquery);
        };
    }

    public static Specification<PaymentRefund> alwaysFalse() {
        return (root, query, cb) -> cb.disjunction();
    }
}
