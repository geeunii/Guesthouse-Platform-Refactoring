CREATE TABLE IF NOT EXISTS accommodation
(
    accommodations_id            BIGINT                                 NOT NULL AUTO_INCREMENT PRIMARY KEY ,
    account_number_id            BIGINT                                 NOT NULL DEFAULT 1,
    user_id                      BIGINT                                 NOT NULL DEFAULT 1,
    accommodations_name          VARCHAR(100)                           NOT NULL,
    accommodations_category      ENUM ('PENSION','GUESTHOUSE')          NOT NULL DEFAULT 'GUESTHOUSE',
    accommodations_description   TEXT                                   NULL,
    short_description            VARCHAR(100)                           NULL,
    city                         VARCHAR(50)                            NULL,
    district                     VARCHAR(50)                            NULL,
    township                     VARCHAR(50)                            NULL,
    parking_info                 TEXT                                   NULL,
    address_detail               VARCHAR(200)                           NULL,
    latitude                     DECIMAL(10, 7)                         NULL,
    longitude                    DECIMAL(10, 7)                         NULL,
    transport_info               TEXT                                   NULL,
    accommodation_status         TINYINT(1)                             NOT NULL DEFAULT 1,
    rejection_reason             TEXT                                   NULL,
    approval_status              ENUM ('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'APPROVED',
    created_at                   DATETIME                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    phone                        VARCHAR(50)                            NULL,
    source_url                   VARCHAR(1000)                          NULL,
    source_platform              VARCHAR(50)                            NULL,
    business_registration_number VARCHAR(15)                            NULL,
    business_registration_image  TEXT                                   NULL,
    sns                          VARCHAR(1000)                          NULL,
    check_in_time                VARCHAR(50)                            NULL,
    check_out_time               VARCHAR(50)                            NULL,
    rating                       DOUBLE                                 NULL,
    review_count                 INT                                    NULL
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS accommodation_amenity
(
    accommodation_amenity_id BIGINT NOT NULL AUTO_INCREMENT,
    accommodations_id        BIGINT NOT NULL,
    amenity_id               BIGINT NOT NULL,
    PRIMARY KEY (accommodation_amenity_id),
    FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id) ON DELETE CASCADE,
    FOREIGN KEY (amenity_id) REFERENCES amenity (amenity_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS amenity
(
    amenity_id    BIGINT       NOT NULL AUTO_INCREMENT,
    amenity_code  VARCHAR(50)  NOT NULL,
    amenity_name  VARCHAR(50)  NOT NULL,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    amenity_icon  TEXT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (amenity_id),
    UNIQUE KEY UQ_AMENITY_CODE (amenity_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- Accommodation image table
CREATE TABLE IF NOT EXISTS accommodation_image
(
    image_id          BIGINT UNSIGNED          NOT NULL AUTO_INCREMENT,
    accommodations_id BIGINT                   NOT NULL,
    image_url         VARCHAR(500)             NOT NULL,
    image_type        ENUM ('banner','detail') NOT NULL DEFAULT 'banner',
    sort_order        INT                      NOT NULL DEFAULT 0,
    PRIMARY KEY (image_id),
    FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- Accommodation-Theme mapping table
CREATE TABLE IF NOT EXISTS accommodation_theme
(
    theme_mapping_id  BIGINT NOT NULL AUTO_INCREMENT,
    theme_id          BIGINT NOT NULL,
    accommodations_id BIGINT NOT NULL,
    PRIMARY KEY (theme_mapping_id),
    FOREIGN KEY (theme_id) REFERENCES theme (theme_id) ON DELETE CASCADE,
    FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS room
(
    room_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    accommodations_id BIGINT          NOT NULL,
    room_name         VARCHAR(100)    NOT NULL,
    price             INT             NULL,
    weekend_price     INT             NULL,
    min_guests        INT             NOT NULL DEFAULT 2,
    max_guests        INT             NULL,
    room_description  TEXT            NULL,
    main_image_url    VARCHAR(500)    NULL,
    room_status       TINYINT(1)      NOT NULL DEFAULT 1,
    bathroom_count    INT             NULL,
    room_type         VARCHAR(50)     NULL,
    bed_count         INT             NULL,
    create_room       DATETIME        NULL DEFAULT CURRENT_TIMESTAMP,
    change_info_room  DATETIME        NULL,
    PRIMARY KEY (room_id),
    FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;




ALTER TABLE accommodation MODIFY business_registration_image LONGTEXT NULL;

ALTER TABLE accommodation_image MODIFY image_url LONGTEXT NOT NULL;
ALTER TABLE room MODIFY main_image_url LONGTEXT NULL;

ALTER TABLE accommodation MODIFY accommodations_category ENUM('PENSION', 'GUESTHOUSE', 'HOTEL', 'MOTEL', 'RESORT', 'HANOK', 'CAMPING') NOT NULL DEFAULT 'GUESTHOUSE';

INSERT IGNORE INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved)
VALUES (1, 'host@test.com', 'password', '010-1234-5678', 'HOST', 1, NOW(), NOW(), 1);