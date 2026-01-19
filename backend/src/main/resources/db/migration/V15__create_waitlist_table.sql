DROP TABLE IF EXISTS waitlist;
CREATE TABLE waitlist (
    waitlist_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    accommodations_id BIGINT NOT NULL,

    checkin DATETIME(6) NOT NULL,
    checkout DATETIME(6) NOT NULL,
    guest_count INTEGER NOT NULL,
    is_notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    
    INDEX idx_waitlist_room_date (room_id, checkin, checkout),
    INDEX idx_waitlist_user (user_id),
    
    CONSTRAINT fk_waitlist_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_waitlist_room FOREIGN KEY (room_id) REFERENCES room (room_id),
    CONSTRAINT fk_waitlist_accommodation FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
