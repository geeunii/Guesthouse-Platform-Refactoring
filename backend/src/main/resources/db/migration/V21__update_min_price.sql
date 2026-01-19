-- Populate min_price in accommodation table from room table
UPDATE accommodation a
SET min_price = (
    SELECT MIN(price)
    FROM room r
    WHERE r.accommodations_id = a.accommodations_id
    AND r.price > 0
);

-- Set default 0 for any that remain null (optional, but good for display safety)
UPDATE accommodation
SET min_price = 0
WHERE min_price IS NULL;
