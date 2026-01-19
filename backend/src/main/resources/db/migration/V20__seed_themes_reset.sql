-- Clean up existing mappings
DELETE FROM accommodation_theme;
-- Reset Auto Increment to 1 (Requested by User)
ALTER TABLE accommodation_theme AUTO_INCREMENT = 1;

DELETE FROM user_theme;
-- Reset Auto Increment to 1 (Requested by User)
ALTER TABLE user_theme AUTO_INCREMENT = 1;

-- =================================================================
-- 1. Seed Accommodation Themes 
-- Scope: accommodations_id <= 209
-- Count: 2 ~ 5 DISTINCT random themes per accommodation
-- Logic: Uses Dynamic SQL (PREPARE) to respect variable LIMIT.
-- =================================================================
DROP PROCEDURE IF EXISTS seed_accommodation_themes;

DELIMITER $$
CREATE PROCEDURE seed_accommodation_themes()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE acc_id BIGINT;
    DECLARE num_themes INT;
    DECLARE cur CURSOR FOR SELECT accommodations_id FROM accommodation WHERE accommodations_id <= 209;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO acc_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Generate a random number between 2 and 5
        -- FLOOR(RAND() * (Max - Min + 1)) + Min
        -- FLOOR(RAND() * (5 - 2 + 1)) + 2 = FLOOR(RAND() * 4) + 2 => 2, 3, 4, 5
        SET num_themes = FLOOR(RAND() * 4) + 2;

        -- Use Prepared Statement to use variable LIMIT
        SET @sql_insert = CONCAT('INSERT IGNORE INTO accommodation_theme (accommodations_id, theme_id) SELECT ', acc_id, ', theme_id FROM theme ORDER BY RAND() LIMIT ', num_themes);
        
        PREPARE stmt FROM @sql_insert;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL seed_accommodation_themes();
DROP PROCEDURE seed_accommodation_themes;

-- =================================================================
-- 2. Seed User Themes
-- Scope: All users
-- Count: 3 DISTINCT random themes per user
-- ID Start: 1 (via ALTER TABLE above)
-- =================================================================
DROP PROCEDURE IF EXISTS seed_user_themes;

DELIMITER $$
CREATE PROCEDURE seed_user_themes()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE u_id BIGINT;
    DECLARE cur CURSOR FOR SELECT user_id FROM users;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO u_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Insert exactly 3 random themes
        -- Standard LIMIT 3 is fine here as it's constant
        INSERT IGNORE INTO user_theme (user_id, theme_id)
        SELECT u_id, theme_id 
        FROM theme 
        ORDER BY RAND() 
        LIMIT 3;

    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL seed_user_themes();
DROP PROCEDURE seed_user_themes;
