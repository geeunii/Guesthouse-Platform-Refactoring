CREATE TABLE IF NOT EXISTS platform_daily_stats
(
    stat_date            DATE          NOT NULL COMMENT '기준 일자',
    total_hosts          BIGINT        NOT NULL COMMENT '전체 호스트 수',
    new_hosts            BIGINT        NOT NULL COMMENT '신규 호스트 수',
    total_accommodations BIGINT        NOT NULL COMMENT '전체 숙소 수',
    new_accommodations   BIGINT        NOT NULL COMMENT '신규 숙소 수',
    total_reservations   BIGINT        NOT NULL COMMENT '생성된 예약 건수',
    reservations_success BIGINT        NOT NULL COMMENT '결제 성공 건수',
    reservations_failed  BIGINT        NOT NULL COMMENT '결제 실패/취소 건수',
    total_revenue        BIGINT        NOT NULL COMMENT '결제 완료 금액 합',
    cancel_count         BIGINT        NOT NULL COMMENT '예약 취소 건수',
    refund_count         BIGINT        NOT NULL COMMENT '환불 요청/완료 건수',
    refund_amount        BIGINT        NOT NULL COMMENT '환불 금액 합계',
    pending_accommodations BIGINT      NOT NULL COMMENT '승인 대기 숙소 수',
    open_reports         BIGINT        NOT NULL COMMENT '미처리 신고 수',
    active_guests        BIGINT        NOT NULL COMMENT '활동 게스트 수',
    active_hosts         BIGINT        NOT NULL COMMENT '활동 호스트 수',
    occupancy_rate       DECIMAL(5, 2) NOT NULL COMMENT '객실 점유율(%)',
    created_at           DATETIME      NOT NULL COMMENT '생성 시각(배치 실행 시간)',
    CONSTRAINT PK_PLATFORM_DAILY_STATS PRIMARY KEY (stat_date)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS host_daily_stats
(
    user_id           BIGINT        NOT NULL COMMENT '호스트 회원 PK',
    stat_date         DATE          NOT NULL COMMENT '기준 일자',
    reservation_count INT           NOT NULL COMMENT '예약 건수',
    reserved_nights   INT           NOT NULL COMMENT '총 숙박 박수',
    total_guests      INT           NOT NULL COMMENT '총 투숙 인원',
    revenue           INT           NOT NULL COMMENT '결제 완료 금액 합',
    canceled_count    INT           NOT NULL COMMENT '취소/실패 건수',
    avg_price         INT           NULL DEFAULT NULL COMMENT '평균 요금(1박 기준)',
    review_count      INT           NULL COMMENT '등록 리뷰 수',
    avg_rating        DECIMAL(2, 1) NULL COMMENT '신규 리뷰 평균 평점',
    occupancy_rate    DECIMAL(5, 2) NULL COMMENT '객실 점유율 %',
    created_at        DATETIME      NOT NULL COMMENT '생성 시각',
    updated_at        DATETIME      NOT NULL COMMENT '갱신 시각',
    CONSTRAINT PK_HOST_DAILY_STATS PRIMARY KEY (user_id, stat_date)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS admins
(
    admin_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '관리자 PK',
    admin_username VARCHAR(100) NOT NULL COMMENT '로그인 ID(UNIQUE)',
    admin_password VARCHAR(255) NOT NULL COMMENT '비밀번호',
    created_at     DATETIME     NOT NULL COMMENT '생성일',
    CONSTRAINT PK_ADMINS PRIMARY KEY (admin_id),
    CONSTRAINT UQ_ADMINS_USERNAME UNIQUE (admin_username)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS admin_log
(
    log_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    admin_id    BIGINT      NOT NULL COMMENT '관리자 PK',
    target_type VARCHAR(20) NOT NULL COMMENT 'USER, ACC, RSV 등',
    target_id   BIGINT      NOT NULL COMMENT '대상 ID',
    action_type VARCHAR(50) NOT NULL COMMENT 'APPROVE, REJECT, BAN 등',
    reason      TEXT        NULL COMMENT '사유',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생 시각',
    CONSTRAINT PK_ADMIN_LOG PRIMARY KEY (log_id),
    CONSTRAINT FK_ADMIN_LOG_ADMIN FOREIGN KEY (admin_id) REFERENCES admins (admin_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS notices
(
    notice_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공지 PK',
    title      VARCHAR(200) NOT NULL COMMENT '제목',
    content    TEXT         NOT NULL COMMENT '내용',
    is_popup   TINYINT      NOT NULL DEFAULT 0 COMMENT '팝업 여부',
    view_count INT          NOT NULL DEFAULT 0 COMMENT '조회수',
    created_at DATETIME     NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시간',
    updated_at DATETIME     NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
    CONSTRAINT PK_NOTICES PRIMARY KEY (notice_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS inquiries
(
    inquiry_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '문의 PK',
    type        VARCHAR(20)  NULL     DEFAULT 'GENERAL' COMMENT '문의 유형',
    title       VARCHAR(200) NOT NULL COMMENT '문의 제목',
    content     TEXT         NOT NULL COMMENT '문의 내용',
    answer      TEXT         NULL COMMENT '관리자 답변',
    status      VARCHAR(20)  NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT / DONE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    answered_at DATETIME     NULL COMMENT '답변 완료 시각',
    CONSTRAINT PK_INQUIRIES PRIMARY KEY (inquiry_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;
