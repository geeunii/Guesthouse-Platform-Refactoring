package com.ssg9th2team.geharbang.domain.accommodation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.GeoBackfillResponse;
import com.ssg9th2team.geharbang.domain.geocoding.GeoPoint;
import com.ssg9th2team.geharbang.domain.geocoding.GeocodingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationGeoService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final AccommodationJpaRepository accommodationRepository;
    private final GeocodingClient geocodingClient;

    @Transactional
    public GeoBackfillResponse backfillMissingCoordinates(int limit) {
        int normalizedLimit = normalizeLimit(limit);
        List<Accommodation> targets = accommodationRepository.findMissingCoordinates(PageRequest.of(0, normalizedLimit));
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (Accommodation accommodation : targets) {
            String address = buildAddress(accommodation);
            if (!StringUtils.hasText(address)) {
                skipped++;
                continue;
            }
            try {
                Optional<GeoPoint> point = geocodingClient.geocode(address);
                if (point.isEmpty()) {
                    skipped++;
                    continue;
                }
                accommodation.updateCoordinates(point.get().latitude(), point.get().longitude());
                updated++;
            } catch (Exception ex) {
                failed++;
                log.warn("Geocode failed for accommodationId={}, address={}",
                        accommodation.getAccommodationsId(), address, ex);
            }
        }

        accommodationRepository.flush();
        long remaining = accommodationRepository.countByLatitudeIsNullOrLongitudeIsNull();
        return new GeoBackfillResponse(
                normalizedLimit,
                targets.size(),
                updated,
                skipped,
                failed,
                remaining
        );
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String buildAddress(Accommodation accommodation) {
        List<String> parts = new ArrayList<>(4);
        appendPart(parts, accommodation.getCity());
        appendPart(parts, accommodation.getDistrict());
        appendPart(parts, accommodation.getTownship());
        appendPart(parts, accommodation.getAddressDetail());
        return String.join(" ", parts);
    }

    private void appendPart(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }
}
