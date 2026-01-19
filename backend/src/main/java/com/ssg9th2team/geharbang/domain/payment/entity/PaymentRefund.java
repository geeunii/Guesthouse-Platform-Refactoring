package com.ssg9th2team.geharbang.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_refund")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "refund_amount", nullable = false)
    private Integer refundAmount;

    @Column(name = "refund_status", nullable = false)
    private Integer refundStatus; // 0: 요청 1: 완료 2: 실패

    @Column(name = "pg_refund_key", length = 100)
    private String pgRefundKey;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", length = 255)
    private String failureMessage;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "reason_message", length = 255)
    private String reasonMessage;

    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.requestedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRefundSuccess(String pgRefundKey, LocalDateTime approvedAt) {
        this.refundStatus = 1; // 완료
        this.pgRefundKey = pgRefundKey;
        this.approvedAt = approvedAt;
    }

    public void updateRefundFailed(String failureCode, String failureMessage) {
        this.refundStatus = 2; // 실패
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }
}
