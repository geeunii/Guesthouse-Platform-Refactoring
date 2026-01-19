package com.ssg9th2team.geharbang.domain.search.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchRepository extends JpaRepository<Accommodation, Long> {
    @Query(value = """
            SELECT DISTINCT a.accommodations_name
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.accommodations_name IS NOT NULL
              AND a.accommodations_name <> ''
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(a.accommodations_name) LIKE CONCAT('%', LOWER(:keyword), '%'))
            ORDER BY a.accommodations_name
            """, nativeQuery = true)
    List<String> suggestAccommodationNames(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT TRIM(CONCAT_WS(' ', a.city, a.district, a.township)) AS region
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND TRIM(CONCAT_WS(' ', a.city, a.district, a.township)) <> ''
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            ORDER BY region
            """, nativeQuery = true)
    List<String> suggestRegions(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT a.accommodations_id AS accommodationsId,
                   a.accommodations_name AS accommodationsName
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND LOWER(a.accommodations_name) = LOWER(:keyword)
            ORDER BY a.accommodations_id
            LIMIT 2
            """, nativeQuery = true)
    List<SearchResolveProjection> resolveAccommodationByName(@Param("keyword") String keyword);

    @Query(value = """
            WITH room_stats AS (
                SELECT accommodations_id,
                       MAX(max_guests) AS maxGuests,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                               ELSE 0
                           END) AS hasGuestCapacity,
                       MIN(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                           END) AS minPriceForGuest
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       CASE
                           WHEN a.short_description IS NOT NULL
                                AND TRIM(a.short_description) <> ''
                               THEN a.short_description
                           ELSE a.accommodations_description
                       END AS short_description,
                       a.city,
                       a.district,
                       a.township,
                       a.latitude,
                       a.longitude,
                       a.rating,
                       a.review_count,
                       COALESCE(rs.maxGuests, 0) AS maxGuests,
                       COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                       COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                       COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                       CASE
                           WHEN (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                       END AS effective_price
                FROM accommodation a
                LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                WHERE a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
            )
            SELECT
                pa.accommodations_id AS accommodationsId,
                pa.accommodations_name AS accommodationsName,
                pa.short_description AS shortDescription,
                pa.city AS city,
                pa.district AS district,
                pa.township AS township,
                pa.latitude AS latitude,
                pa.longitude AS longitude,
                pa.effective_price AS minPrice,
                pa.rating AS rating,
                pa.review_count AS reviewCount,
                pa.maxGuests AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
            FROM priced_accommodations pa
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = pa.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                   OR (:includeUnavailable = true
                       AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
              AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

            """, countQuery = """
            WITH room_stats AS (
                SELECT accommodations_id,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                               ELSE 0
                           END) AS hasGuestCapacity,
                       MIN(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                           END) AS minPriceForGuest
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       a.city,
                       a.district,
                       a.township,
                       COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                       COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                       COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                       CASE
                           WHEN (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                       END AS effective_price
                FROM accommodation a
                LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                WHERE a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
            )
            SELECT COUNT(*)
            FROM priced_accommodations pa
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                   OR (:includeUnavailable = true
                       AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
              AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicListNoDates(
            @Param("keyword") String keyword,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL '1' DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL '1' DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            )
            SELECT
                a.accommodations_id AS accommodationsId,
                a.accommodations_name AS accommodationsName,
                CASE
                    WHEN a.short_description IS NOT NULL
                         AND TRIM(a.short_description) <> ''
                        THEN a.short_description
                    ELSE a.accommodations_description
                END AS shortDescription,
                a.city AS city,
                a.district AS district,
                a.township AS township,
                a.latitude AS latitude,
                a.longitude AS longitude,
                CASE
                    WHEN (:checkin IS NULL OR :checkout IS NULL)
                         AND (:guestCount IS NULL OR :guestCount < 2)
                        THEN a.min_price
                    ELSE COALESCE(mp.min_price, a.min_price)
                END AS minPrice,
                a.rating AS rating,
                a.review_count AS reviewCount,
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(a.review_count, 0) * COALESCE(a.rating, 0.0) + 40.0) / (COALESCE(a.review_count, 0) + 10.0) AS bayesianScore
            FROM accommodation a
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id,
                       MAX(max_guests) AS maxGuests,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = a.accommodations_id
            LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room_candidates rc
                      WHERE rc.accommodations_id = a.accommodations_id
                   )
                   OR (:includeUnavailable = true
                       AND (COALESCE(rmax.activeRoomCount, 0) = 0
                            OR COALESCE(rmax.hasValidMaxGuests, 0) = 0)))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM available_rooms ar
                      WHERE ar.accommodations_id = a.accommodations_id
                   )
                   OR (:includeUnavailable = true
                       AND (COALESCE(rmax.activeRoomCount, 0) = 0
                            OR COALESCE(rmax.hasValidMaxGuests, 0) = 0)))
              AND (:minPrice IS NULL OR
                   (CASE
                       WHEN (:checkin IS NULL OR :checkout IS NULL)
                            AND (:guestCount IS NULL OR :guestCount < 2)
                           THEN a.min_price
                       ELSE COALESCE(mp.min_price, a.min_price)
                    END) >= :minPrice)
              AND (:maxPrice IS NULL OR
                   (CASE
                       WHEN (:checkin IS NULL OR :checkout IS NULL)
                            AND (:guestCount IS NULL OR :guestCount < 2)
                           THEN a.min_price
                       ELSE COALESCE(mp.min_price, a.min_price)
                    END) <= :maxPrice)

            """, countQuery = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL '1' DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL '1' DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            )
            SELECT COUNT(*)
            FROM accommodation a
            LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
            LEFT JOIN (
                SELECT accommodations_id,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = a.accommodations_id
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room_candidates rc
                      WHERE rc.accommodations_id = a.accommodations_id
                   )
                   OR (:includeUnavailable = true
                       AND (COALESCE(rmax.activeRoomCount, 0) = 0
                            OR COALESCE(rmax.hasValidMaxGuests, 0) = 0)))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM available_rooms ar
                      WHERE ar.accommodations_id = a.accommodations_id
                   )
                   OR (:includeUnavailable = true
                       AND (COALESCE(rmax.activeRoomCount, 0) = 0
                            OR COALESCE(rmax.hasValidMaxGuests, 0) = 0)))
               AND (:minPrice IS NULL OR
                    (CASE
                        WHEN (:checkin IS NULL OR :checkout IS NULL)
                             AND (:guestCount IS NULL OR :guestCount < 2)
                            THEN a.min_price
                        ELSE COALESCE(mp.min_price, a.min_price)
                     END) >= :minPrice)
               AND (:maxPrice IS NULL OR
                    (CASE
                        WHEN (:checkin IS NULL OR :checkout IS NULL)
                             AND (:guestCount IS NULL OR :guestCount < 2)
                            THEN a.min_price
                        ELSE COALESCE(mp.min_price, a.min_price)
                     END) <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicList(
            @Param("keyword") String keyword,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
            WITH room_stats AS (
                SELECT accommodations_id,
                       MAX(max_guests) AS maxGuests,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                               ELSE 0
                           END) AS hasGuestCapacity,
                       MIN(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                           END) AS minPriceForGuest
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       CASE
                           WHEN a.short_description IS NOT NULL
                                AND TRIM(a.short_description) <> ''
                               THEN a.short_description
                           ELSE a.accommodations_description
                       END AS short_description,
                       a.city,
                       a.district,
                       a.township,
                       a.latitude,
                       a.longitude,
                       a.rating,
                       a.review_count,
                       COALESCE(rs.maxGuests, 0) AS maxGuests,
                       COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                       COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                       COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                       CASE
                           WHEN (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                       END AS effective_price
                FROM accommodation a
                JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                WHERE at.theme_id IN (:themeIds)
                  AND a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
            )
            SELECT DISTINCT
                pa.accommodations_id AS accommodationsId,
                pa.accommodations_name AS accommodationsName,
                pa.short_description AS shortDescription,
                pa.city AS city,
                pa.district AS district,
                pa.township AS township,
                pa.latitude AS latitude,
                pa.longitude AS longitude,
                pa.effective_price AS minPrice,
                pa.rating AS rating,
                pa.review_count AS reviewCount,
                pa.maxGuests AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
            FROM priced_accommodations pa
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = pa.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                   OR (:includeUnavailable = true
                       AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
              AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

            """, countQuery = """
            WITH room_stats AS (
                SELECT accommodations_id,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                               ELSE 0
                           END) AS hasGuestCapacity,
                       MIN(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                           END) AS minPriceForGuest
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       a.city,
                       a.district,
                       a.township,
                       COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                       COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                       COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                       CASE
                           WHEN (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                       END AS effective_price
                FROM accommodation a
                JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                WHERE at.theme_id IN (:themeIds)
                  AND a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
            )
            SELECT COUNT(DISTINCT pa.accommodations_id)
            FROM priced_accommodations pa
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                   OR (:includeUnavailable = true
                       AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
              AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByThemeNoDates(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL '1' DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL '1' DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT DISTINCT a.accommodations_id,
                       a.accommodations_name,
                       CASE
                           WHEN a.short_description IS NOT NULL
                                AND TRIM(a.short_description) <> ''
                               THEN a.short_description
                           ELSE a.accommodations_description
                       END AS short_description,
                       a.city,
                       a.district,
                       a.township,
                       a.latitude,
                       a.longitude,
                       a.rating,
                       a.review_count,
                       CASE
                           WHEN (:checkin IS NULL OR :checkout IS NULL)
                                AND (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(mp.min_price, a.min_price)
                       END AS effective_price
                FROM accommodation a
                JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
                WHERE at.theme_id IN (:themeIds)
                  AND a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND (:keyword IS NULL OR :keyword = ''
                       OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0
                       OR EXISTS (
                          SELECT 1
                          FROM room_candidates rc
                          WHERE rc.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
                  AND (:checkin IS NULL OR :checkout IS NULL
                       OR EXISTS (
                          SELECT 1
                          FROM available_rooms ar
                          WHERE ar.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
            )
            SELECT
                pa.accommodations_id AS accommodationsId,
                pa.accommodations_name AS accommodationsName,
                pa.short_description AS shortDescription,
                pa.city AS city,
                pa.district AS district,
                pa.township AS township,
                pa.latitude AS latitude,
                pa.longitude AS longitude,
                pa.effective_price AS minPrice,
                pa.rating AS rating,
                pa.review_count AS reviewCount,
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
            FROM priced_accommodations pa
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = pa.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = pa.accommodations_id
            WHERE (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

            """, countQuery = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL '1' DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL '1' DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT DISTINCT a.accommodations_id,
                       a.accommodations_name,
                       a.city,
                       a.district,
                       a.township,
                       CASE
                           WHEN (:checkin IS NULL OR :checkout IS NULL)
                                AND (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(mp.min_price, a.min_price)
                       END AS effective_price
                FROM accommodation a
                JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
                WHERE at.theme_id IN (:themeIds)
                  AND a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND (:keyword IS NULL OR :keyword = ''
                       OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0
                       OR EXISTS (
                          SELECT 1
                          FROM room_candidates rc
                          WHERE rc.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
                  AND (:checkin IS NULL OR :checkout IS NULL
                       OR EXISTS (
                          SELECT 1
                          FROM available_rooms ar
                          WHERE ar.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
            )
            SELECT COUNT(*)
            FROM priced_accommodations pa
            WHERE (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByTheme(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
            WITH room_stats AS (
                SELECT accommodations_id,
                       MAX(max_guests) AS maxGuests,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                               ELSE 0
                           END) AS hasGuestCapacity,
                       MIN(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                           END) AS minPriceForGuest
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       CASE
                           WHEN a.short_description IS NOT NULL
                                AND TRIM(a.short_description) <> ''
                               THEN a.short_description
                           ELSE a.accommodations_description
                       END AS short_description,
                       a.city,
                       a.district,
                       a.township,
                       a.latitude,
                       a.longitude,
                       a.rating,
                       a.review_count,
                       COALESCE(rs.maxGuests, 0) AS maxGuests,
                       COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                       COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                       COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                       CASE
                           WHEN (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                       END AS effective_price
                FROM accommodation a
                LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                WHERE a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND a.latitude IS NOT NULL
                  AND a.longitude IS NOT NULL
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLng AND :maxLng
            )
            SELECT
                pa.accommodations_id AS accommodationsId,
                pa.accommodations_name AS accommodationsName,
                pa.short_description AS shortDescription,
                pa.city AS city,
                pa.district AS district,
                pa.township AS township,
                pa.latitude AS latitude,
                pa.longitude AS longitude,
                pa.effective_price AS minPrice,
                pa.rating AS rating,
                pa.review_count AS reviewCount,
                pa.maxGuests AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
            FROM priced_accommodations pa
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = pa.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                     OR (:includeUnavailable = true
                         AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
              AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

            """, countQuery = """
            WITH room_stats AS (
                SELECT accommodations_id,
                       MAX(max_guests) AS maxGuests,
                       COUNT(*) AS activeRoomCount,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) > 0 THEN 1
                               ELSE 0
                           END) AS hasValidMaxGuests,
                       MAX(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                               ELSE 0
                           END) AS hasGuestCapacity,
                       MIN(CASE
                               WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                           END) AS minPriceForGuest
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       a.city,
                       a.district,
                       a.township,
                       COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                       COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                       COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                       CASE
                           WHEN (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                       END AS effective_price
                FROM accommodation a
                LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                WHERE a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND a.latitude IS NOT NULL
                  AND a.longitude IS NOT NULL
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLng AND :maxLng
            )
            SELECT COUNT(DISTINCT pa.accommodations_id)
            FROM priced_accommodations pa
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                   OR (:includeUnavailable = true
                       AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
              AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByBoundsNoDates(
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL 1 DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL 1 DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       CASE
                           WHEN a.short_description IS NOT NULL
                                AND TRIM(a.short_description) <> ''
                               THEN a.short_description
                           ELSE a.accommodations_description
                       END AS short_description,
                       a.city,
                       a.district,
                       a.township,
                       a.latitude,
                       a.longitude,
                       a.rating,
                       a.review_count,
                       CASE
                           WHEN (:checkin IS NULL OR :checkout IS NULL)
                                AND (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(mp.min_price, a.min_price)
                       END AS effective_price
                FROM accommodation a
                LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
                WHERE a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND a.latitude IS NOT NULL
                  AND a.longitude IS NOT NULL
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLng AND :maxLng
                  AND (:keyword IS NULL OR :keyword = ''
                       OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0
                       OR EXISTS (
                          SELECT 1
                          FROM room_candidates rc
                          WHERE rc.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
                  AND (:checkin IS NULL OR :checkout IS NULL
                       OR EXISTS (
                          SELECT 1
                          FROM available_rooms ar
                          WHERE ar.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
            )
            SELECT
                pa.accommodations_id AS accommodationsId,
                pa.accommodations_name AS accommodationsName,
                pa.short_description AS shortDescription,
                pa.city AS city,
                pa.district AS district,
                pa.township AS township,
                pa.latitude AS latitude,
                pa.longitude AS longitude,
                pa.effective_price AS minPrice,
                pa.rating AS rating,
                pa.review_count AS reviewCount,
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
            FROM priced_accommodations pa
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = pa.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = pa.accommodations_id
            WHERE (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

            """, countQuery = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL 1 DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL 1 DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT a.accommodations_id,
                       a.accommodations_name,
                       a.city,
                       a.district,
                       a.township,
                       CASE
                           WHEN (:checkin IS NULL OR :checkout IS NULL)
                                AND (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(mp.min_price, a.min_price)
                       END AS effective_price
                FROM accommodation a
                LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
                WHERE a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND a.latitude IS NOT NULL
                  AND a.longitude IS NOT NULL
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLng AND :maxLng
                  AND (:keyword IS NULL OR :keyword = ''
                       OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0
                       OR EXISTS (
                          SELECT 1
                          FROM room_candidates rc
                          WHERE rc.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
                  AND (:checkin IS NULL OR :checkout IS NULL
                       OR EXISTS (
                          SELECT 1
                          FROM available_rooms ar
                          WHERE ar.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
            )
            SELECT COUNT(*)
            FROM priced_accommodations pa
            WHERE (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByBounds(
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
                WITH room_stats

            AS (
                    SELECT accommodations_id,
                           MAX(max_guests) AS maxGuests,
                           COUNT(*) AS activeRoomCount,
                           MAX(CASE
                                   WHEN COALESCE(max_guests, 0) > 0 THEN 1
                                   ELSE 0
                               END) AS hasValidMaxGuests,

            MAX(CASE
                                   WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                                   ELSE 0
                               END) AS hasGuestCapacity,
                           MIN(CASE

            WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                               END) AS minPriceForGuest
                    FROM room
                    WHERE room_status = 1
                    GROUP BY accommodations_id
                ),

            priced_accommodations AS (
                    SELECT a.accommodations_id,
                           a.accommodations_name,
                           CASE
                               WHEN a.short_description IS NOT NULL
                                    AND TRIM(a.short_description) <> ''
                                   THEN a.short_description
                               ELSE a.accommodations_description
                           END AS short_description,
                           a.city,
                           a.district,
                           a.township,
                           a.latitude,
                           a.longitude,
                           a.rating,
                           a.review_count,
                           COALESCE(rs.maxGuests, 0) AS maxGuests,
                           COALESCE(rs.activeRoomCount, 0) AS activeRoomCount,
                           COALESCE(rs.hasValidMaxGuests, 0) AS hasValidMaxGuests,
                           COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                           CASE
                               WHEN (:guestCount IS NULL OR :guestCount < 2)
                                   THEN a.min_price

            ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                           END AS effective_price
                    FROM accommodation a
                    JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                    LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                    WHERE

            at.theme_id IN (:themeIds)
                      AND a.accommodation_status = 1
                      AND a.approval_status = 'APPROVED'
                      AND a.latitude IS NOT NULL
                      AND a.longitude IS NOT NULL
                      AND a.latitude BETWEEN :minLat AND :maxLat
                      AND a.longitude BETWEEN :minLng AND :maxLng
                )
                SELECT
                    pa.accommodations_id AS accommodationsId,
                    pa.accommodations_name AS accommodationsName,
                    pa.short_description AS shortDescription,
                    pa.city AS city,
                    pa.district AS district,
                    pa.township AS township,
                    pa.latitude AS latitude,
                    pa.longitude AS longitude,
                    pa.effective_price AS minPrice,
                    pa.rating AS rating,
                    pa.review_count AS reviewCount,
                    pa.maxGuests AS maxGuests,
                    ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
                FROM priced_accommodations pa
                LEFT JOIN accommodation_image ai
                  ON ai.accommodations_id = pa.accommodations_id
                 AND ai.sort_order = 0
                 AND ai.image_type = 'banner'
                WHERE (:keyword IS NULL OR :keyword = ''

            OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township))

            LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1
                       OR (:includeUnavailable = true
                           AND (pa.activeRoomCount = 0 OR pa.hasValidMaxGuests = 0)))
                  AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
                  AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

                """, countQuery = """
                WITH room_stats AS (
                    SELECT accommodations_id,
                           MAX(max_guests) AS maxGuests,
                           COUNT(*) AS activeRoomCount,
                           MAX(CASE
                                   WHEN COALESCE(max_guests, 0) > 0 THEN 1
                                   ELSE 0
                               END) AS hasValidMaxGuests,

            MAX(CASE
                                   WHEN COALESCE(max_guests, 0) >= :guestCount THEN 1
                                   ELSE 0
                               END) AS hasGuestCapacity,
                           MIN(CASE

            WHEN COALESCE(max_guests, 0) >= :guestCount THEN price
                               END) AS minPriceForGuest
                    FROM room
                    WHERE room_status = 1
                    GROUP BY accommodations_id
                ),

            priced_accommodations AS (
                    SELECT a.accommodations_id,
                           a.accommodations_name,
                           a.city,
                           a.district,
                           a.township,
                           COALESCE(rs.hasGuestCapacity, 0) AS hasGuestCapacity,
                           CASE
                               WHEN (:guestCount IS NULL OR :guestCount < 2)
                                   THEN a.min_price

            ELSE COALESCE(rs.minPriceForGuest, a.min_price)
                           END AS effective_price
                    FROM accommodation a
                    JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                    LEFT JOIN room_stats rs ON rs.accommodations_id = a.accommodations_id
                    WHERE

            at.theme_id IN (:themeIds)
                      AND a.accommodation_status = 1
                      AND a.approval_status = 'APPROVED'
                      AND a.latitude IS NOT NULL
                      AND a.longitude IS NOT NULL
                      AND a.latitude BETWEEN :minLat AND :maxLat
                      AND a.longitude BETWEEN :minLng AND :maxLng
                )

            SELECT COUNT(DISTINCT pa.accommodations_id)
                FROM priced_accommodations pa
                WHERE (:keyword IS NULL OR :keyword = ''

            OR LOWER(CONCAT_WS(' ', pa.accommodations_name, pa.city, pa.district, pa.township))

            LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0 OR pa.hasGuestCapacity = 1)
                  AND (:minPrice IS NULL OR pa.effective_price >= :minPrice)
                  AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
                """, nativeQuery = true)

    Page<ListDtoProjection> searchPublicListByThemeAndBoundsNoDates(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);

    @Query(value = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL 1 DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL 1 DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT DISTINCT a.accommodations_id,
                       a.accommodations_name,
                       CASE
                           WHEN a.short_description IS NOT NULL
                                AND TRIM(a.short_description) <> ''
                               THEN a.short_description
                           ELSE a.accommodations_description
                       END AS short_description,
                       a.city,
                       a.district,
                       a.township,
                       a.latitude,
                       a.longitude,
                       a.rating,
                       a.review_count,
                       CASE
                           WHEN (:checkin IS NULL OR :checkout IS NULL)
                                AND (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(mp.min_price, a.min_price)
                       END AS effective_price
                FROM accommodation a
                JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
                WHERE at.theme_id IN (:themeIds)
                  AND a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND a.latitude IS NOT NULL
                  AND a.longitude IS NOT NULL
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLng AND :maxLng
                  AND (:keyword IS NULL OR :keyword = ''
                       OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0
                       OR EXISTS (
                          SELECT 1
                          FROM room_candidates rc
                          WHERE rc.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
                  AND (:checkin IS NULL OR :checkout IS NULL
                       OR EXISTS (
                          SELECT 1
                          FROM available_rooms ar
                          WHERE ar.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
            )
            SELECT
                pa.accommodations_id AS accommodationsId,
                pa.accommodations_name AS accommodationsName,
                pa.short_description AS shortDescription,
                pa.city AS city,
                pa.district AS district,
                pa.township AS township,
                pa.latitude AS latitude,
                pa.longitude AS longitude,
                pa.effective_price AS minPrice,
                pa.rating AS rating,
                pa.review_count AS reviewCount,
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl,
                (COALESCE(pa.review_count, 0) * COALESCE(pa.rating, 0.0) + 40.0) / (COALESCE(pa.review_count, 0) + 10.0) AS bayesianScore
            FROM priced_accommodations pa
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = pa.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = pa.accommodations_id
            WHERE (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)

            """, countQuery = """
            WITH RECURSIVE stay_dates (stay_date) AS (
                SELECT CAST(:checkin AS DATE) AS stay_date
                UNION ALL
                SELECT CAST(stay_date AS DATE) + INTERVAL 1 DAY
                FROM stay_dates
                WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL 1 DAY
            ),
            room_candidates (room_id, accommodations_id, price, max_guests) AS (
                SELECT r.room_id,
                       r.accommodations_id,
                       r.price,
                       COALESCE(r.max_guests, 0) AS max_guests
                FROM room r
                WHERE r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
            ),
            available_rooms (room_id, accommodations_id, price, max_guests) AS (
                SELECT rc.room_id,
                       rc.accommodations_id,
                       rc.price,
                       rc.max_guests
                FROM room_candidates rc
                WHERE (
                        (:guestCount IS NULL OR :guestCount = 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = rc.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        ))
                   OR (
                        (:guestCount IS NOT NULL AND :guestCount > 0)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM stay_dates d
                            LEFT JOIN reservation res
                              ON res.room_id = rc.room_id
                             AND res.is_deleted = 0
                             AND res.reservation_status IN (2, 3)
                             AND d.stay_date >= CAST(res.checkin AS DATE)
                             AND d.stay_date < CAST(res.checkout AS DATE)
                            GROUP BY d.stay_date
                            HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                        ))
            ),
            min_prices (accommodations_id, min_price) AS (
                SELECT accommodations_id,
                       MIN(price) AS min_price
                FROM available_rooms
                GROUP BY accommodations_id
            ),
            priced_accommodations AS (
                SELECT DISTINCT a.accommodations_id,
                       a.accommodations_name,
                       a.city,
                       a.district,
                       a.township,
                       CASE
                           WHEN (:checkin IS NULL OR :checkout IS NULL)
                                AND (:guestCount IS NULL OR :guestCount < 2)
                               THEN a.min_price
                           ELSE COALESCE(mp.min_price, a.min_price)
                       END AS effective_price
                FROM accommodation a
                JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id
                WHERE at.theme_id IN (:themeIds)
                  AND a.accommodation_status = 1
                  AND a.approval_status = 'APPROVED'
                  AND a.latitude IS NOT NULL
                  AND a.longitude IS NOT NULL
                  AND a.latitude BETWEEN :minLat AND :maxLat
                  AND a.longitude BETWEEN :minLng AND :maxLng
                  AND (:keyword IS NULL OR :keyword = ''
                       OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
                  AND (:guestCount IS NULL OR :guestCount = 0
                       OR EXISTS (
                          SELECT 1
                          FROM room_candidates rc
                          WHERE rc.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
                  AND (:checkin IS NULL OR :checkout IS NULL
                       OR EXISTS (
                          SELECT 1
                          FROM available_rooms ar
                          WHERE ar.accommodations_id = a.accommodations_id
                       )
                       OR (:includeUnavailable = true
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                               )
                               OR NOT EXISTS (
                                   SELECT 1
                                   FROM room r
                                   WHERE r.accommodations_id = a.accommodations_id
                                     AND r.room_status = 1
                                     AND COALESCE(r.max_guests, 0) > 0
                               )
                           )))
            )
            SELECT COUNT(*)
            FROM priced_accommodations pa
            WHERE (:minPrice IS NULL OR pa.effective_price >= :minPrice)
              AND (:maxPrice IS NULL OR pa.effective_price <= :maxPrice)
            """, nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByThemeAndBounds(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("includeUnavailable") boolean includeUnavailable,
            Pageable pageable);
}
