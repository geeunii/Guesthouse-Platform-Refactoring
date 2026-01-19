package com.ssg9th2team.geharbang.domain.geocoding;

import java.util.Optional;

public interface GeocodingClient {
    Optional<GeoPoint> geocode(String address);
}
