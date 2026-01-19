package com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationImageDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccountNumberDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.HostAccommodationSummaryResponse;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccommodationMapper {

    // ===== 숙소 CRUD =====

    void insertAccommodation(Accommodation accommodation); // 숙소 등록

    AccommodationResponseDto selectAccommodationById(@Param("accommodationsId") Long accommodationsId); // 숙소 조회

    void updateAccommodation(@Param("accommodationsId") Long accommodationsId, // 숙소 수정
            @Param("Accommodation") Accommodation accommodation);

    void deleteAccommodation(@Param("accommodationsId") Long accommodationsId); // 숙소 삭제

    // 객실 목록 조회 (서비스 로직용)
    List<RoomResponseListDto> selectRoomsByAccommodationId(@Param("accommodationsId") Long accommodationsId);

    // ===== 연관 테이블 (이미지, 편의시설, 테마) =====

    void deleteAccommodationImages(@Param("accommodationsId") Long accommodationsId); // 이미지 삭제

    void insertAccommodationImages(@Param("accommodationsId") Long accommodationsId, // 이미지 등록
            @Param("images") List<AccommodationImageDto> images);

    void insertAccommodationAmenities(@Param("accommodationsId") Long accommodationsId, // 편의시설 등록
            @Param("amenityIds") List<Long> amenityIds);

    void deleteAccommodationAmenities(@Param("accommodationsId") Long accommodationsId); // 편의시설 삭제

    void insertAccommodationThemes(@Param("accommodationsId") Long accommodationsId, // 테마 등록
            @Param("themeIds") List<Long> themeIds);

    void deleteAccommodationThemes(@Param("accommodationsId") Long accommodationsId); // 테마 삭제

    // ===== 일괄 삭제 (N+1 문제 해결) =====

    void deleteAccommodations(@Param("accommodationIds") List<Long> accommodationIds);

    void deleteAccommodationImagesIn(@Param("accommodationIds") List<Long> accommodationIds);

    void deleteAccommodationAmenitiesIn(@Param("accommodationIds") List<Long> accommodationIds);

    void deleteAccommodationThemesIn(@Param("accommodationIds") List<Long> accommodationIds);

    // 숙소 대표 이미지 조회 (sort_order = 0)
    String selectMainImageUrl(@Param("accommodationsId") Long accommodationsId);

    // 여러 숙소의 대표 이미지 배치 조회
    List<AccommodationImageDto> selectMainImagesByAccommodationIds(
            @Param("accommodationIds") List<Long> accommodationIds);

    // ===== 정산계좌 =====

    void insertAccountNumber(AccountNumberDto accountNumberDto);

    void updateAccountNumber(@Param("accountNumberId") Long accountNumberId,
            @Param("AccountNumberDto") AccountNumberDto accountNumberDto);

    // ===== 최소 가격 업데이트 =====

    void updateMinPrice(@Param("accommodationsId") Long accommodationsId);

    // ===== Host dashboard list =====

    List<HostAccommodationSummaryResponse> selectHostAccommodations(@Param("hostId") Long hostId);

    HostAccommodationSummaryResponse selectHostAccommodationById(
            @Param("hostId") Long hostId,
            @Param("accommodationsId") Long accommodationsId);

}
