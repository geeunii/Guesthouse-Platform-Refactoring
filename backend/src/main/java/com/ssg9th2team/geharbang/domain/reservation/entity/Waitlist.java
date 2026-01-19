package com.ssg9th2team.geharbang.domain.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대기 목록 엔티티
 * 정원 초과로 예약 실패 시 대기 등록하여 빈자리 알림 수신
 */
@Entity
@Table(name = "waitlist")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "accommodations_id", nullable = false)
    private Long accommodationsId;

    @Column(name = "checkin", nullable = false)
    private LocalDateTime checkin;

    @Column(name = "checkout", nullable = false)
    private LocalDateTime checkout;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "is_notified", nullable = false)
    @Builder.Default
    private Boolean isNotified = false;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 대기 등록 최대 개수
    public static final int MAX_WAITLIST_PER_USER = 3;

    // 예약 기회 유효 시간 (24시간)
    public static final int RESERVATION_OPPORTUNITY_HOURS = 24;

    // 알림 가능 최소 일수 (체크인 7일 전까지만 알림)
    public static final int MIN_DAYS_BEFORE_CHECKIN = 7;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 알림 발송 완료 처리
     * 24시간 예약 기회 부여
     */
    public void markAsNotified() {
        markAsNotified(LocalDateTime.now());
    }

    public void markAsNotified(LocalDateTime notificationTime) {
        this.isNotified = true;
        this.notifiedAt = notificationTime;
        this.expiresAt = this.notifiedAt.plusHours(RESERVATION_OPPORTUNITY_HOURS);
    }

    /**
     * 알림이 만료되었는지 확인 (24시간 경과)
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
