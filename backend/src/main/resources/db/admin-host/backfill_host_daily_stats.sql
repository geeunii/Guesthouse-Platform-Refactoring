-- Backfill host_daily_stats from reservation data.
-- Assumptions:
-- - host_daily_stats PK = (user_id, stat_date)
-- - host user_id is accommodation.user_id
-- - stat_date uses reservation checkout date for revenue alignment
-- - Only confirmed/checked-in and paid reservations are counted (adjust as needed)

-- Set the target window (inclusive start, exclusive end).
SET @start_date = '2025-12-01';
SET @end_date = '2026-01-01';

INSERT INTO host_daily_stats (user_id,
                              stat_date,
                              reservation_count,
                              reserved_nights,
                              total_guests,
                              revenue,
                              canceled_count,
                              avg_price,
                              review_count,
                              avg_rating,
                              occupancy_rate,
                              created_at,
                              updated_at)
SELECT a.user_id                                                                  AS user_id,
       DATE(r.checkout)                                                           AS stat_date,
       COUNT(*)                                                                   AS reservation_count,
       COALESCE(SUM(r.stay_nights), 0)                                            AS reserved_nights,
       COALESCE(SUM(r.guest_count), 0)                                            AS total_guests,
       COALESCE(SUM(r.final_payment_amount), 0)                                   AS revenue,
       0                                                                          AS canceled_count,
       COALESCE(ROUND(AVG(r.final_payment_amount / NULLIF(r.stay_nights, 0))), 0) AS avg_price,
       NULL                                                                       AS review_count,
       NULL                                                                       AS avg_rating,
       NULL                                                                       AS occupancy_rate,
       NOW()                                                                      AS created_at,
       NOW()                                                                      AS updated_at
FROM reservation r
         JOIN accommodation a
              ON a.accommodations_id = r.accommodations_id
WHERE r.checkout >= @start_date
  AND r.checkout < @end_date
  AND r.reservation_status IN (2, 3)
  AND r.payment_status = 1
GROUP BY a.user_id, DATE(r.checkout)
ON DUPLICATE KEY UPDATE reservation_count = VALUES(reservation_count),
                        reserved_nights   = VALUES(reserved_nights),
                        total_guests      = VALUES(total_guests),
                        revenue           = VALUES(revenue),
                        canceled_count    = VALUES(canceled_count),
                        avg_price         = VALUES(avg_price),
                        review_count      = VALUES(review_count),
                        avg_rating        = VALUES(avg_rating),
                        occupancy_rate    = VALUES(occupancy_rate),
                        updated_at        = VALUES(updated_at);

-- Notes:
-- - canceled_count/review_count/avg_rating/occupancy_rate can be filled later
--   once business rules are confirmed.
-- - If you want to include pending reservations, adjust reservation_status/payment_status filters.
