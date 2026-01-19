-- Add indexes to speed up availability checks for search.
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'room'
      AND index_name = 'idx_room_acc_status_guests'
);

SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_room_acc_status_guests ON room (accommodations_id, room_status, max_guests)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'reservation'
      AND index_name = 'idx_reservation_room_status_dates'
);

SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_reservation_room_status_dates ON reservation (room_id, is_deleted, reservation_status, checkin, checkout)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
