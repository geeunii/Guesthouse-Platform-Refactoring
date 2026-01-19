package com.ssg9th2team.geharbang.domain.main.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ListDto {
    private Long accommodationsId;
    private String accommodationsName;
    private String shortDescription;
    private String city;
    private String district;
    private String township;
    private Double latitude;
    private Double longitude;
    private Long minPrice;
    private Double rating;
    private Integer reviewCount;
    private Integer maxGuests;
    private String imageUrl;

}
