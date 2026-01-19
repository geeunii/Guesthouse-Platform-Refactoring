CREATE TABLE IF NOT EXISTS reservation
(
    reservation_id         BIGINT           NOT NULL AUTO_INCREMENT COMMENT '예약 PK',
    accommodations_id      BIGINT           NOT NULL COMMENT '숙소 PK',
    user_id                BIGINT           NOT NULL COMMENT '예약자 회원 PK',
    checkin                DATETIME         NOT NULL COMMENT '체크인',
    checkout               DATETIME         NOT NULL COMMENT '체크아웃',
    stay_nights            TINYINT UNSIGNED NOT NULL COMMENT '숙박 박수',
    guest_count            TINYINT UNSIGNED NOT NULL COMMENT '총 인원',
    reservation_status     TINYINT(1)       NOT NULL COMMENT '0 임시, 1 요청, 2 확정, 3 체크인완료, 9 취소',
    total_amount_before_dc INT UNSIGNED     NOT NULL COMMENT '할인 전 합계',
    coupon_discount_amount INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '쿠폰 할인액',
    final_payment_amount   INT UNSIGNED     NOT NULL COMMENT '최종 결제 금액',
    payment_status         TINYINT(1)       NOT NULL COMMENT '0 미결제, 1 완료, 2 실패, 3 환불완료',
    reserver_name          VARCHAR(50)      NOT NULL COMMENT '예약자 이름',
    reserver_phone         VARCHAR(20)      NOT NULL COMMENT '예약자 전화번호',
    created_at             DATETIME         NOT NULL COMMENT '생성 시각',
    updated_at             DATETIME         NOT NULL COMMENT '수정 시각',
    CONSTRAINT PK_RESERVATION PRIMARY KEY (reservation_id),
    CONSTRAINT FK_RESERVATION_ACC FOREIGN KEY (accommodations_id) REFERENCES accommodation (accommodations_id),
    CONSTRAINT FK_RESERVATION_USER FOREIGN KEY (user_id) REFERENCES users (user_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS payment
(
    payment_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '결제 PK',
    reservation_id   BIGINT       NOT NULL COMMENT '예약 PK',
    pg_provider_code VARCHAR(20)  NOT NULL COMMENT 'PG사 코드 (TOSS 등)',
    payment_method   VARCHAR(20)  NOT NULL COMMENT '결제 수단',
    order_id         VARCHAR(50)  NOT NULL COMMENT '우리 주문번호',
    pg_payment_key   VARCHAR(100) NULL COMMENT 'PG 결제키',
    request_amount   INTEGER      NOT NULL COMMENT '요청 금액',
    approved_amount  INTEGER      NULL COMMENT '승인 금액',
    currency_code    CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT '통화 코드',
    payment_status   TINYINT(1)   NOT NULL COMMENT '0 요청, 1 성공, 2 실패, 3 취소',
    failure_code     VARCHAR(50)  NULL COMMENT 'PG 실패 코드',
    failure_message  VARCHAR(255) NULL COMMENT '실패 사유',
    approved_at      DATETIME     NULL COMMENT '승인 시각',
    created_at       DATETIME     NOT NULL COMMENT '생성 시각',
    updated_at       DATETIME     NOT NULL COMMENT '수정 시각',
    CONSTRAINT PK_PAYMENT PRIMARY KEY (payment_id),
    CONSTRAINT FK_PAYMENT_RESERVATION FOREIGN KEY (reservation_id) REFERENCES reservation (reservation_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS payment_refund
(
    refund_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '환불 PK',
    payment_id        BIGINT       NOT NULL COMMENT '결제 PK',
    refund_amount     INTEGER      NOT NULL COMMENT '환불 요청 금액',
    refund_status     TINYINT(1)   NOT NULL COMMENT '0 요청, 1 성공, 2 실패',
    pg_refund_key     VARCHAR(100) NULL COMMENT 'PG 환불 키',
    pg_transaction_id VARCHAR(100) NULL COMMENT 'PG 트랜잭션 ID',
    failure_code      VARCHAR(50)  NULL COMMENT 'PG 실패 코드',
    failure_message   VARCHAR(255) NULL COMMENT '실패 사유',
    reason_code       VARCHAR(50)  NULL COMMENT '환불 사유 코드',
    reason_message    VARCHAR(255) NULL COMMENT '환불 사유 상세',
    requested_by      VARCHAR(50)  NOT NULL COMMENT '요청 주체',
    requested_at      DATETIME     NOT NULL COMMENT '요청 시각',
    approved_at       DATETIME     NULL COMMENT '성공 시각',
    created_at        DATETIME     NOT NULL COMMENT '생성 시각',
    updated_at        DATETIME     NOT NULL COMMENT '수정 시각',
    CONSTRAINT PK_PAYMENT_REFUND PRIMARY KEY (refund_id),
    CONSTRAINT FK_REFUND_PAYMENT FOREIGN KEY (payment_id) REFERENCES payment (payment_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;