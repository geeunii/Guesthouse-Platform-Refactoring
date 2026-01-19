ALTER TABLE admin_log
    ADD COLUMN metadata_json TEXT NULL COMMENT '추가 메타데이터 JSON' AFTER reason;
