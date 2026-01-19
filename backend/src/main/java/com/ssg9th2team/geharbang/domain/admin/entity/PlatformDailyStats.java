package com.ssg9th2team.geharbang.domain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_daily_stats")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformDailyStats {

    @Id
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_hosts", nullable = false)
    private Long totalHosts;

    @Column(name = "new_hosts", nullable = false)
    private Long newHosts;

    @Column(name = "total_accommodations", nullable = false)
    private Long totalAccommodations;

    @Column(name = "new_accommodations", nullable = false)
    private Long newAccommodations;

    @Column(name = "total_reservations", nullable = false)
    private Long totalReservations;

    @Column(name = "reservations_success", nullable = false)
    private Long reservationsSuccess;

    @Column(name = "reservations_failed", nullable = false)
    private Long reservationsFailed;

    @Column(name = "total_revenue", nullable = false)
    private Long totalRevenue;

    @Column(name = "cancel_count", nullable = false)
    private Long cancelCount;

    @Column(name = "refund_count", nullable = false)
    private Long refundCount;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "pending_accommodations", nullable = false)
    private Long pendingAccommodations;

    @Column(name = "open_reports", nullable = false)
    private Long openReports;

    @Column(name = "active_guests", nullable = false)
    private Long activeGuests;

    @Column(name = "active_hosts", nullable = false)
    private Long activeHosts;

    @Column(name = "occupancy_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal occupancyRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
