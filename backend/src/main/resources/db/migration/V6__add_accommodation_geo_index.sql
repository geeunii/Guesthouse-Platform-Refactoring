-- Create the geo index only if it doesn't already exist (idempotent for Flyway).
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'accommodation'
      AND index_name = 'idx_accommodation_geo'
);

SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_accommodation_geo ON accommodation (latitude, longitude)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
