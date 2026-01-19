package com.ssg9th2team.geharbang.domain.payment.spec;

import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.entity.PaymentRefund;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<Payment> createdBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        );
    }

    public static Specification<Payment> statusEquals(Integer status) {
        if (status == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("paymentStatus"), status);
    }

    public static Specification<Payment> refundedExists(Integer refundStatus) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var refund = subquery.from(PaymentRefund.class);
            subquery.select(cb.literal(1L));
            subquery.where(
                    cb.equal(refund.get("paymentId"), root.get("id")),
                    cb.equal(refund.get("refundStatus"), refundStatus)
            );
            return cb.exists(subquery);
        };
    }

    public static Specification<Payment> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.where(null);
        }
        String normalized = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("orderId")), normalized),
                cb.like(cb.lower(root.get("pgPaymentKey")), normalized)
        );
    }
}
