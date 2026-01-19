package com.ssg9th2team.geharbang.domain.main.dto;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AmenityDetailDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseListDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.util.StringUtils;

@Getter
@Builder
public class AccommodationDetailDto {
    private Long accommodationsId;
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
    private String parkingInfo;
    private String sns;
    private String phone;
    private String checkInTime;
    private String checkOutTime;
    private Integer minPrice;
    private Double rating;
    private Integer reviewCount;
    private List<ReviewResponseDto> reviews;
    private List<String> amenities;
    private List<AmenityDetailDto> amenityDetails;
    private List<String> themes;
    private List<RoomResponseListDto> rooms;
    private List<AccommodationImageDto> images;

    public static AccommodationDetailDto from(AccommodationResponseDto dto) {
        if (dto == null) {
            return null;
        }

        // description이 null이거나 빈 문자열이면 shortDescription을 사용
        String description = dto.getAccommodationsDescription();
        if (!StringUtils.hasText(description)) {
            description = dto.getShortDescription();
        }

        return AccommodationDetailDto.builder()
                .accommodationsId(dto.getAccommodationsId())
                .accommodationsName(dto.getAccommodationsName())
                .accommodationsCategory(dto.getAccommodationsCategory())
                .accommodationsDescription(description)
                .shortDescription(dto.getShortDescription())
                .city(dto.getCity())
                .district(dto.getDistrict())
                .township(dto.getTownship())
                .addressDetail(dto.getAddressDetail())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .transportInfo(dto.getTransportInfo())
                .parkingInfo(dto.getParkingInfo())
                .sns(dto.getSns())
                .phone(dto.getPhone())
                .checkInTime(dto.getCheckInTime())
                .checkOutTime(dto.getCheckOutTime())
                .minPrice(dto.getMinPrice())
                .rating(dto.getRating())
                .reviewCount(dto.getReviewCount())
                .reviews(dto.getReviews())
                .amenities(dto.getAmenities())
                .amenityDetails(dto.getAmenityDetails())
                .themes(dto.getThemes())
                .rooms(dto.getRooms())
                .images(dto.getImages())
                .build();
    }
}
