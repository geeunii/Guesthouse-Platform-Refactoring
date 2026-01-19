ALTER TABLE admin_log
    ADD COLUMN request_ip VARCHAR(45) NULL COMMENT '요청 IP' AFTER metadata_json,
    ADD COLUMN user_agent VARCHAR(512) NULL COMMENT 'User-Agent' AFTER request_ip;
