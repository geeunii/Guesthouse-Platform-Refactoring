package com.ssg9th2team.geharbang.domain.accommodation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accommodation", indexes = {
        @Index(name = "idx_accommodation_rating", columnList = "rating"),
        @Index(name = "idx_accommodation_review_count", columnList = "review_count"),
        @Index(name = "idx_accommodation_min_price", columnList = "min_price"),
        @Index(name = "idx_accommodation_status", columnList = "accommodation_status, approval_status"),
        @Index(name = "idx_accommodation_lat_lng", columnList = "latitude, longitude")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodations_id")
    private Long accommodationsId;

    @Column(name = "account_number_id", nullable = false)
    private Long accountNumberId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "accommodations_name", length = 100, nullable = false)
    private String accommodationsName;

    @Enumerated(EnumType.STRING)
    @Column(name = "accommodations_category", nullable = false)
    private AccommodationsCategory accommodationsCategory;

    @Column(name = "accommodations_description", columnDefinition = "TEXT", nullable = false)
    private String accommodationsDescription;

    @Column(name = "short_description", length = 100, nullable = false)
    private String shortDescription;

    @Column(name = "city", length = 50, nullable = false)
    private String city;

    @Column(name = "district", length = 50, nullable = false)
    private String district;

    @Column(name = "township", length = 50, nullable = false)
    private String township;

    @Column(name = "address_detail", length = 200, nullable = false)
    private String addressDetail;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "transport_info", columnDefinition = "TEXT", nullable = false)
    private String transportInfo;

    @Column(name = "accommodation_status", nullable = false) // 숙소 운영 상태
    private Integer accommodationStatus;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false) // 숙소 승인 상태
    private ApprovalStatus approvalStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "business_registration_number", length = 20, nullable = false)
    private String businessRegistrationNumber;

    @Column(name = "business_registration_image", columnDefinition = "LONGTEXT")
    private String businessRegistrationImage;

    @Column(name = "parking_info", length = 500, nullable = false)
    private String parkingInfo;

    @Column(name = "sns", length = 500)
    private String sns;

    @Column(name = "check_in_time", length = 20, nullable = false)
    private String checkInTime;

    @Column(name = "check_out_time", length = 20, nullable = false)
    private String checkOutTime;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    // 생성 시 기본값 세팅
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.accommodationStatus = 0; // 승인 대기 시 운영 상태는 0 (비활성)
        this.approvalStatus = ApprovalStatus.PENDING;
    }

    // ========================= JPA 사용할 때 사용 할 메서드 ==============================
    // 수정 메서드 (변경 가능한 필드만)
    public void update(String accommodationsDescription,
            String shortDescription,
            String transportInfo,
            Integer accommodationStatus,
            String parkingInfo,
            String sns,
            String phone,
            String checkInTime,
            String checkOutTime) {
        this.accommodationsDescription = accommodationsDescription;
        this.shortDescription = shortDescription;
        this.transportInfo = transportInfo;
        this.accommodationStatus = accommodationStatus;
        this.parkingInfo = parkingInfo;
        this.sns = sns;
        this.phone = phone;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    public void updateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }

    // 관리자 승인/반려 메서드
    public void updateApprovalStatus(ApprovalStatus status, String rejectionReason) {
        this.approvalStatus = status;
        this.rejectionReason = rejectionReason;

        // 승인 시 운영 상태를 1(운영 중)로 변경
        if (status == ApprovalStatus.APPROVED) {
            this.accommodationStatus = 1;
        }
    }

}
