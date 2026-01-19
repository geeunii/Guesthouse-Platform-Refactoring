package com.ssg9th2team.geharbang.domain.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityDetailDto {
    private String amenityName;
    private String amenityIcon;
}
