CREATE TABLE IF NOT EXISTS review_tag
(
    review_tag_id   INT         NOT NULL AUTO_INCREMENT COMMENT '태그 PK',
    review_tag_name VARCHAR(50) NOT NULL COMMENT '태그 이름',
    is_active       TINYINT     NOT NULL DEFAULT 1 COMMENT '사용 여부 (0/1)',
    CONSTRAINT PK_REVIEW_TAG PRIMARY KEY (review_tag_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS review
(
    review_id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '리뷰 PK',
    accommodations_id BIGINT        NOT NULL COMMENT '숙소 PK',
    user_id           BIGINT        NOT NULL COMMENT '회원 PK',
    rating            DECIMAL(2, 1) NOT NULL COMMENT '평점 0.0~5.0',
    content           TEXT          NOT NULL COMMENT '리뷰 본문',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시각',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    is_deleted        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '삭제 플래그 (0/1)',
    CONSTRAINT PK_REVIEW PRIMARY KEY (review_id),
    CONSTRAINT FK_REVIEW_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_REVIEW_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT CK_REVIEW_RATING CHECK (rating >= 0.0 AND rating <= 5.0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS review_image
(
    review_image_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '리뷰 이미지 PK',
    review_id        BIGINT       NOT NULL COMMENT '리뷰 PK',
    review_image_url VARCHAR(255) NOT NULL COMMENT '이미지 URL',
    sort_order       INT          NULL     DEFAULT 1 COMMENT '노출 순서',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    CONSTRAINT PK_REVIEW_IMAGE PRIMARY KEY (review_image_id),
    CONSTRAINT FK_REVIEW_IMAGE_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_tag_map
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '매핑 PK',
    review_tag_id INT             NOT NULL COMMENT '태그 PK',
    review_id     BIGINT          NOT NULL COMMENT '리뷰 PK',
    CONSTRAINT PK_REVIEW_TAG_MAP PRIMARY KEY (id),
    CONSTRAINT FK_REVIEW_TAG_MAP_TAG FOREIGN KEY (review_tag_id) REFERENCES review_tag (review_tag_id),
    CONSTRAINT FK_REVIEW_TAG_MAP_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_replies
(
    reply_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '답글 PK',
    review_id  BIGINT   NOT NULL COMMENT '리뷰 PK',
    user_id    BIGINT   NOT NULL COMMENT '회원 PK',
    content    TEXT     NOT NULL COMMENT '답글 내용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME NULL COMMENT '수정 시각',
    CONSTRAINT PK_REVIEW_REPLIES PRIMARY KEY (reply_id),
    CONSTRAINT FK_REVIEW_REPLIES_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id),
    CONSTRAINT FK_REVIEW_REPLIES_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS review_reports
(
    report_id  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '신고 PK',
    review_id  BIGINT      NOT NULL COMMENT '리뷰 PK',
    user_id    BIGINT      NOT NULL COMMENT '회원 PK',
    reason     TEXT        NOT NULL COMMENT '신고 사유',
    state      VARCHAR(20) NOT NULL COMMENT 'WAIT / CHECKED / BLINDED 등',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신고 접수 시각',
    updated_at DATETIME    NULL COMMENT '처리 완료 시각',
    CONSTRAINT PK_REVIEW_REPORTS PRIMARY KEY (report_id),
    CONSTRAINT FK_REVIEW_REPORT_REVIEW FOREIGN KEY (review_id) REFERENCES review (review_id),
    CONSTRAINT FK_REVIEW_REPORT_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;