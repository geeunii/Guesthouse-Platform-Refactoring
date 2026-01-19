package com.ssg9th2team.geharbang.domain.main.repository;

public interface ListDtoProjection {
    Long getAccommodationsId();

    String getAccommodationsName();

    String getShortDescription();

    String getCity();

    String getDistrict();

    String getTownship();

    Double getLatitude();

    Double getLongitude();

    Long getMinPrice();

    Double getRating();

    Integer getReviewCount();

    Integer getMaxGuests();

    String getImageUrl();

    Double getBayesianScore();
}
