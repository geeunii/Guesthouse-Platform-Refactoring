package com.ssg9th2team.geharbang.domain.main.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainAccommodationListResponse {
    private List<ListDto> recommendedAccommodations;
    private List<ListDto> generalAccommodations;
}
