package com.ssg9th2team.geharbang.domain.room.repository.jpa;

import com.ssg9th2team.geharbang.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomJpaRepository extends JpaRepository<Room, Long> {

        /**
         * 비관적 락(Pessimistic Lock)으로 Room 조회
         * 동시성 제어를 위해 먼저 조회하는 트랜잭션이 락을 획득함
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM Room r WHERE r.roomId = :id")
        Optional<Room> findByIdWithLock(@Param("id") Long id);

        @Modifying
        @Query("UPDATE Room r SET r.maxGuests = r.maxGuests - :guestCount WHERE r.roomId = :roomId AND r.maxGuests >= :guestCount")
        int decreaseMaxGuests(@Param("roomId") Long roomId, @Param("guestCount") Integer guestCount);

        @Query("""
                        SELECT COUNT(r) AS roomCount,
                               MAX(r.maxGuests) AS maxGuests,
                               MIN(r.price) AS minPrice
                        FROM Room r
                        WHERE r.accommodationsId = :accommodationId
                        """)
        RoomStats findRoomStats(@Param("accommodationId") Long accommodationId);

        @Query("""
                        SELECT r.accommodationsId AS accommodationsId,
                               MAX(r.maxGuests) AS maxGuests
                        FROM Room r
                        WHERE r.accommodationsId IN :accommodationIds
                          AND r.roomStatus = 1
                        GROUP BY r.accommodationsId
                        """)
        List<AccommodationGuestStats> findMaxGuestsByAccommodationIds(
                        @Param("accommodationIds") List<Long> accommodationIds);

        List<Room> findByAccommodationsId(Long accommodationsId);

        /**
         * 특정 숙소의 예약 가능한 객실 ID 목록 조회
         * - 활성 상태 객실만 (room_status = 1)
         * - 해당 기간에 확정/진행중 예약이 없는 객실
         */
        @Query(value = """
                WITH RECURSIVE stay_dates (stay_date) AS (
                    SELECT CAST(:checkin AS DATE) AS stay_date
                    UNION ALL
                    SELECT CAST(stay_date AS DATE) + INTERVAL '1' DAY
                    FROM stay_dates
                    WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL '1' DAY
                )
                SELECT r.room_id
                FROM room r
                WHERE r.accommodations_id = :accommodationsId
                  AND r.room_status = 1
                  AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                  AND (
                      ((:guestCount IS NULL OR :guestCount = 0)
                          AND NOT EXISTS (
                              SELECT 1
                              FROM reservation res
                              WHERE res.room_id = r.room_id
                                AND res.is_deleted = 0
                                AND res.reservation_status IN (2, 3)
                                AND res.checkin < :checkout
                                AND res.checkout > :checkin
                          ))
                      OR
                      ((:guestCount IS NOT NULL AND :guestCount > 0)
                          AND NOT EXISTS (
                              SELECT 1
                              FROM stay_dates d
                              LEFT JOIN reservation res
                                ON res.room_id = r.room_id
                               AND res.is_deleted = 0
                               AND res.reservation_status IN (2, 3)
                               AND d.stay_date >= CAST(res.checkin AS DATE)
                               AND d.stay_date < CAST(res.checkout AS DATE)
                              GROUP BY d.stay_date
                              HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > COALESCE(r.max_guests, 0)
                          ))
                  )
                """, nativeQuery = true)
        List<Long> findAvailableRoomIds(
                @Param("accommodationsId") Long accommodationsId,
                @Param("checkin") LocalDateTime checkin,
                @Param("checkout") LocalDateTime checkout,
                @Param("guestCount") Integer guestCount
        );
}
