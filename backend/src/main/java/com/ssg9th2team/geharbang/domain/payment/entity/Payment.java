package com.ssg9th2team.geharbang.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "pg_provider_code", nullable = false, length = 20)
    private String pgProviderCode;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "pg_payment_key", length = 100)
    private String pgPaymentKey;

    @Column(name = "request_amount", nullable = false)
    private Integer requestAmount;

    @Column(name = "approved_amount")
    private Integer approvedAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "KRW";

    @Column(name = "payment_status", nullable = false)
    private Integer paymentStatus;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", length = 255)
    private String failureMessage;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePaymentSuccess(String pgPaymentKey, Integer approvedAmount, String paymentMethod,
            LocalDateTime approvedAt) {
        this.pgPaymentKey = pgPaymentKey;
        this.approvedAmount = approvedAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = 1; // 성공
        this.approvedAt = approvedAt;
    }

    public void updatePaymentFailure(String failureCode, String failureMessage) {
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.paymentStatus = 2; // 실패
    }
}
