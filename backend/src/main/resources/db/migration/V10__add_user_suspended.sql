ALTER TABLE users
    ADD COLUMN is_suspended TINYINT(1) NOT NULL DEFAULT 0 COMMENT '계정 정지 여부' AFTER host_approved;
