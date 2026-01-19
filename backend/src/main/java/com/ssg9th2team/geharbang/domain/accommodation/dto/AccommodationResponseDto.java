package com.ssg9th2team.geharbang.domain.accommodation.dto;

import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseListDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccommodationResponseDto {
    // 숙소 조회용 DTO
    private Long accommodationsId;
    private Long accountNumberId;
    private Long userId;
    private String accommodationsName;
    private String accommodationsCategory;
    private String accommodationsDescription;
    private String shortDescription;
    private String city;
    private String district;
    private String township;
    private String addressDetail;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String transportInfo;
    private Integer accommodationStatus;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private String phone;
    private String businessRegistrationNumber;
    private String businessRegistrationImage;
    private String parkingInfo;
    private String sns;
    private String checkInTime;
    private String checkOutTime;
    private String rejectionReason;
    private Integer minPrice;
    private Double rating;
    private Integer reviewCount;
    private List<ReviewResponseDto> reviews;


    // 대표 이미지 URL (리스트 조회용)
    private String mainImageUrl;

    // 정산계좌 정보 (조인)
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // 편의시설 및 테마 (1:N 조인)
    // 프론트엔드가 화면에 표시할 때는 ID가 아니라 실제 이름이 필요하니까 request랑 다르게 String타입으로 받기
    private List<String> amenities;  //AmenityDetailDto안에 amenities(편의시설이름)있음 중복됨
    private List<AmenityDetailDto> amenityDetails;
    private List<String> themes;  // 테마 이름

    // 숙소 상세보기에서 객실 리스트
    private List<RoomResponseListDto> rooms;

    // 숙소 이미지 (1:N 조인)
    private List<AccommodationImageDto> images;

    // 편의시설/테마 ID 목록 (수정 페이지용)
    private List<Long> amenityIds;
    private List<Long> themeIds;

}
