package com.ssg9th2team.geharbang.domain.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "accommodations_id", nullable = false)
    private Long accommodationsId;

    @Column(name = "room_name", length = 100, nullable = false)
    private String roomName;

    @Column(name = "price")
    private Integer price;

    @Column(name = "weekend_price")
    private Integer weekendPrice;

    @Column(name = "min_guests", nullable = false)
    private Integer minGuests;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "room_description", columnDefinition = "TEXT")
    private String roomDescription;

    @Column(name = "room_introduction", columnDefinition = "TEXT")
    private String roomIntroduction;

    @Column(name = "main_image_url", columnDefinition = "LONGTEXT")
    private String mainImageUrl;

    @Column(name = "room_status", nullable = false)
    private Integer roomStatus;

    @Column(name = "bathroom_count")
    private Integer bathroomCount;

    @Column(name = "room_type", length = 50)
    private String roomType;

    @Column(name = "bed_count")
    private Integer bedCount;

    @Column(name = "create_room")
    private LocalDateTime createRoom;

    @Column(name = "change_info_room")
    private LocalDateTime changeInfoRoom;

    // 생성 시 기본값 세팅
    @PrePersist
    public void prePersist() {
        this.createRoom = LocalDateTime.now();
        this.roomStatus = 1;
        if (this.minGuests == null) {
            this.minGuests = 2;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.changeInfoRoom = LocalDateTime.now();
    }

    // ================================== JPA에서 사용
    // ===========================================

    // 수정 메서드
    public void update(String roomName,
            Integer price,
            Integer weekendPrice,
            Integer minGuests,
            Integer maxGuests,
            String roomDescription,
            String roomIntroduction,
            String mainImageUrl,
            Integer roomStatus,
            Integer bathroomCount,
            String roomType,
            Integer bedCount) {
        this.roomName = roomName;
        this.price = price;
        this.weekendPrice = weekendPrice;
        this.minGuests = minGuests;
        this.maxGuests = maxGuests;
        this.roomDescription = roomDescription;
        this.roomIntroduction = roomIntroduction;
        this.mainImageUrl = mainImageUrl;
        this.roomStatus = roomStatus;
        this.bathroomCount = bathroomCount;
        this.roomType = roomType;
        this.bedCount = bedCount;
    }
}
