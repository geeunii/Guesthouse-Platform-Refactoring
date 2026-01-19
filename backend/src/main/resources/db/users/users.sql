CREATE TABLE IF NOT EXISTS users
(
    user_id         BIGINT               NOT NULL AUTO_INCREMENT COMMENT '회원 PK',
    email           VARCHAR(50)          NOT NULL COMMENT '로그인 ID, UNIQUE',
    password        VARCHAR(255)         NULL COMMENT '소셜 로그인 시 NULL 가능',
    phone           VARCHAR(50)          NOT NULL COMMENT '휴대폰 번호',
    role            ENUM ('HOST','USER') NULL COMMENT '호스트 / 유저',
    marketing_agree TINYINT              NOT NULL COMMENT '마케팅 동의 (0/1)',
    created_at      DATETIME             NOT NULL COMMENT '가입일',
    updated_at      DATETIME             NOT NULL COMMENT '수정일',
    host_approved   TINYINT              NULL COMMENT '호스트 승인 상태 (1 승인, 0 거절)',
    CONSTRAINT PK_USERS PRIMARY KEY (user_id),
    CONSTRAINT UQ_USERS_EMAIL UNIQUE (email)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS wishlist
(
    wish_id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '위시리스트 PK',
    accommodations_id BIGINT   NOT NULL COMMENT '숙소 PK',
    created_at        DATETIME NOT NULL COMMENT '추가 시각',
    user_id           BIGINT   NOT NULL COMMENT '회원 PK',
    CONSTRAINT PK_WISHLIST PRIMARY KEY (wish_id),
    CONSTRAINT FK_WISHLIST_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT FK_WISHLIST_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS user_theme
(
    id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '선택 PK',
    user_id  BIGINT NOT NULL COMMENT '회원 PK',
    theme_id BIGINT NOT NULL COMMENT '테마 PK',
    CONSTRAINT PK_USER_THEME PRIMARY KEY (id),
    CONSTRAINT FK_USER_THEME_USER FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT FK_USER_THEME_THEME FOREIGN KEY (theme_id) REFERENCES theme (theme_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS account_number
(
    account_number_id INT         NOT NULL AUTO_INCREMENT COMMENT '계좌 PK',
    bank_name         VARCHAR(50) NOT NULL COMMENT '은행명',
    account_number    VARCHAR(50) NOT NULL COMMENT '계좌번호',
    account_holder    VARCHAR(50) NULL COMMENT '예금주',
    CONSTRAINT PK_ACCOUNT_NUMBER PRIMARY KEY (account_number_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '유저-쿠폰 PK',
    coupon_id  BIGINT UNSIGNED NOT NULL COMMENT '쿠폰 PK',
    user_id    BIGINT          NOT NULL COMMENT '회원 PK',
    issued_at  DATETIME        NOT NULL COMMENT '발급 시각',
    used_at    DATETIME        NULL COMMENT '사용 시각',
    expired_at DATETIME        NULL COMMENT '만료 시각',
    status     VARCHAR(20)     NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED / USED / EXPIRED',
    CONSTRAINT PK_USER_COUPON PRIMARY KEY (id),
    CONSTRAINT FK_USER_COUPON_COUPON FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id),
    CONSTRAINT FK_USER_COUPON_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_social
(
    social_id     BIGINT                  NOT NULL AUTO_INCREMENT COMMENT '소셜 PK',
    user_id       BIGINT                  NOT NULL COMMENT '회원 PK',
    provider      ENUM ('GOOGLE','KAKAO') NOT NULL COMMENT '소셜 제공자',
    provider_uid  VARCHAR(255)            NOT NULL COMMENT '프로바이더 고유값',
    email         VARCHAR(255)            NULL COMMENT '소셜 이메일',
    profile_image VARCHAR(255)            NULL COMMENT '프로필 이미지',
    created_at    DATETIME                NOT NULL COMMENT '최초 로그인',
    CONSTRAINT PK_USER_SOCIAL PRIMARY KEY (social_id),
    CONSTRAINT UQ_USER_SOCIAL UNIQUE (provider, provider_uid),
    CONSTRAINT FK_USER_SOCIAL_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
