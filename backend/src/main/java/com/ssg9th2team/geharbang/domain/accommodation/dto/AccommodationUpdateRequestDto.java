package com.ssg9th2team.geharbang.domain.accommodation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AccommodationUpdateRequestDto {
    // 숙소 업데이트용 DTO
    private Long roomId;
    private String accommodationsName;
    private String accommodationsDescription;  // 숙소 상세설명
    private String shortDescription;           // 한줄소개
    private String transportInfo;              // 주변교통정보
    private Integer accommodationStatus;       // 운영상태
    private String parkingInfo;                // 주차정보
    private String sns;                        // SNS
    private String phone;                      // 전화번호
    private String checkInTime;                // 체크인시간
    private String checkOutTime;               // 체크아웃시간
    private BigDecimal latitude;
    private BigDecimal longitude;


    // 편의시설 및 테마 (1:N 조인)
    private List<Long> amenityIds;
    private List<Long> themeIds;
    private List<AccommodationImageDto> images;
    // 객실 수정/추가 리스트
    private List<RoomData> rooms;

    // 정산계좌 정보 (조인)
    private String bankName;
    private String accountNumber;
    private String accountHolder;




    // roomId가 있으면 수정 없으면 추가
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoomData {
        private Long roomId; // 있으면 수정, 없으면 추가
        private String roomName;
        private Integer price;
        private Integer weekendPrice;
        private Integer minGuests;
        private Integer maxGuests;
        private String roomDescription;
        private String mainImageUrl;
        private Integer bathroomCount;
        private String roomType;
        private Integer bedCount;
        private Integer roomStatus;
    }
}
