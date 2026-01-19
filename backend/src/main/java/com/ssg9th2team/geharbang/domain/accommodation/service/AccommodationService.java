package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationUpdateRequestDto;

import java.util.List;

public interface AccommodationService {

    // 숙소 등록
    Long createAccommodation(Long userId, AccommodationCreateRequestDto createRequestDto);

    // 숙소 상세조회
    AccommodationResponseDto getAccommodation(Long accommodationsId);

    // 숙소 수정
    void updateAccommodation(Long accommodationsId, AccommodationUpdateRequestDto createRequestDto);

    // 숙소 삭제
    void deleteAccommodation(Long accommodationsId);

    // 숙소 일괄 삭제 (N+1 문제 해결)
    void deleteAccommodations(List<Long> accommodationIds);
}
