package com.ssg9th2team.geharbang.domain.accommodation.dto;

import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AccommodationCreateRequestDto {
    // 숙소 생성용 DTO
    @NotNull
    private String accommodationsName;

    @NotNull
    private String accommodationsCategory;       // PENSION, GUESTHOUSE

    @NotNull
    private String accommodationsDescription;

    @NotNull
    private String shortDescription;

    @NotNull
    private String city;

    @NotNull
    private String district;

    @NotNull
    private String township;

    @NotNull
    private String addressDetail;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    @NotNull
    private String transportInfo;

    @NotNull
    private String businessRegistrationNumber;

    @NotNull
    private String businessRegistrationImage;

    @NotNull
    private String parkingInfo;


    private String sns;

    @NotBlank
    private String phone;

    @NotNull
    private String checkInTime;

    @NotNull
    private String checkOutTime;



    // ===== 연관 테이블 =====

    // 편의시설, 테마는 이미 DB에 존재 -> 숙소는 선택해서 연결만 하면 됨 ( 그래서 아이디만 필요,  DTO로 다시 받을 필요없음 )
    @NotEmpty(message = "편의시설을 최소 1개 이상 선택해주세요")
    private List<Long> amenityIds;                    // 편의시설 ID 목록

    @NotEmpty(message = "테마를 최소 1개 이상 선택해주세요")
    private List<Long> themeIds;                      // 테마 ID 목록

    // 이미지, 객실 정보는 숙소 등록할때 새로 등록되야 하는 정보니까 DTO 타입으로 받음
    private List<AccommodationImageDto> images;       // 이미지 목록
    private List<RoomCreateDto> rooms;                // 객실 목록


    // 정산계좌 정보 (등록 시 함께 생성)
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}
