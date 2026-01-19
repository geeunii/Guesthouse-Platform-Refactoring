-- Add composite index for accommodation_image to optimize search query performance.
-- This index will significantly speed up the JOIN in search queries that filter by:
-- accommodations_id, image_type = 'banner', and sort_order = 0

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'accommodation_image'
      AND index_name = 'idx_accommodation_image_lookup'
);

SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_accommodation_image_lookup ON accommodation_image (accommodations_id, image_type, sort_order)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
