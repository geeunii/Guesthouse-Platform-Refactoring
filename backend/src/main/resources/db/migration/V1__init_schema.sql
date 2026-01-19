create table accommodation
(
    accommodations_id            bigint auto_increment
        primary key,
    account_number_id            bigint                                                                         default 1                 not null,
    user_id                      bigint                                                                         default 1                 not null,
    accommodations_name          varchar(100)                                                                                             not null,
    accommodations_category      enum ('PENSION', 'GUESTHOUSE', 'HOTEL', 'MOTEL', 'RESORT', 'HANOK', 'CAMPING') default 'GUESTHOUSE'      not null,
    accommodations_description   text                                                                                                     null,
    short_description            varchar(100)                                                                                             null,
    city                         varchar(50)                                                                                              null,
    district                     varchar(50)                                                                                              null,
    township                     varchar(50)                                                                                              null,
    parking_info                 varchar(500)                                                                                             null,
    address_detail               varchar(200)                                                                                             null,
    latitude                     decimal(10, 7)                                                                                           null,
    longitude                    decimal(10, 7)                                                                                           null,
    transport_info               text                                                                                                     null,
    accommodation_status         int                                                                                                      not null,
    rejection_reason             text                                                                                                     null,
    approval_status              varchar(255)                                                                                             null,
    created_at                   datetime                                                                       default CURRENT_TIMESTAMP not null,
    phone                        varchar(50)                                                                                              null,
    source_url                   varchar(1000)                                                                                            null,
    source_platform              varchar(50)                                                                                              null,
    business_registration_number varchar(20)                                                                                              null,
    business_registration_image  longtext                                                                                                 null,
    sns                          varchar(500)                                                                                             null,
    check_in_time                varchar(20)                                                                                              null,
    check_out_time               varchar(20)                                                                                              null,
    rating                       double                                                                                                   null,
    review_count                 int                                                                                                      null,
    min_price                    int                                                                                                      null
)
    charset = utf8mb4;

create table accommodation_image
(
    image_id          bigint unsigned auto_increment
        primary key,
    accommodations_id bigint                                     not null,
    image_url         longtext                                   null,
    image_type        enum ('banner', 'detail') default 'banner' not null,
    sort_order        int                       default 0        not null,
    constraint accommodation_image_ibfk_1
        foreign key (accommodations_id) references accommodation (accommodations_id)
            on delete cascade
)
    charset = utf8mb4;

create index accommodations_id
    on accommodation_image (accommodations_id);

create table account_number
(
    account_number_id int auto_increment comment '계좌 PK'
        primary key,
    bank_name         varchar(50) not null comment '은행명',
    account_number    varchar(50) not null comment '계좌번호',
    account_holder    varchar(50) null comment '예금주'
)
    charset = utf8mb4;

create table admins
(
    admin_id       bigint auto_increment comment '관리자 PK'
        primary key,
    admin_username varchar(100) not null comment '로그인 ID(UNIQUE)',
    admin_password varchar(255) not null comment '비밀번호',
    created_at     datetime     not null comment '생성일',
    constraint UQ_ADMINS_USERNAME
        unique (admin_username)
)
    charset = utf8mb4;

create table admin_log
(
    log_id      bigint auto_increment comment '로그 PK'
        primary key,
    admin_id    bigint                             not null comment '관리자 PK',
    target_type varchar(20)                        not null comment 'USER, ACC, RSV 등',
    target_id   bigint                             not null comment '대상 ID',
    action_type varchar(50)                        not null comment 'APPROVE, REJECT, BAN 등',
    reason      text                               null comment '사유',
    created_at  datetime default CURRENT_TIMESTAMP not null comment '발생 시각',
    constraint FK_ADMIN_LOG_ADMIN
        foreign key (admin_id) references admins (admin_id)
)
    charset = utf8mb4;

create table amenity
(
    amenity_id    bigint auto_increment
        primary key,
    amenity_code  varchar(50)          not null,
    amenity_name  varchar(50)          not null,
    is_active     tinyint(1) default 1 not null,
    amenity_icon  text                 null,
    display_order int        default 0 not null,
    constraint UQ_AMENITY_CODE
        unique (amenity_code)
)
    charset = utf8mb4;

create table accommodation_amenity
(
    accommodation_amenity_id bigint auto_increment
        primary key,
    accommodations_id        bigint not null,
    amenity_id               bigint not null,
    constraint accommodation_amenity_ibfk_1
        foreign key (accommodations_id) references accommodation (accommodations_id)
            on delete cascade,
    constraint accommodation_amenity_ibfk_2
        foreign key (amenity_id) references amenity (amenity_id)
            on delete cascade
)
    charset = utf8mb4;

create index accommodations_id
    on accommodation_amenity (accommodations_id);

create index amenity_id
    on accommodation_amenity (amenity_id);

create table coupon
(
    coupon_id      bigint unsigned auto_increment comment '쿠폰 PK'
        primary key,
    code           varchar(50)       not null comment '쿠폰 식별 코드 (UNIQUE)',
    name           varchar(100)      not null comment '쿠폰 이름',
    description    varchar(255)      null comment '쿠폰 설명',
    discount_type  varchar(20)       not null comment 'AMOUNT / PERCENT',
    discount_value int               not null comment '할인 금액 또는 할인율',
    min_price      int               not null comment '최소 적용 금액',
    max_discount   int               null comment '정율 할인 상한',
    valid_from     datetime          not null comment '시작 일시',
    valid_to       datetime          not null comment '종료 일시',
    is_active      tinyint default 1 not null comment '사용 가능 여부 (0/1)',
    created_at     datetime          not null comment '생성 시각',
    constraint UQ_COUPON_CODE
        unique (code)
)
    charset = utf8mb4;

create table inquiries
(
    inquiry_id  bigint auto_increment comment '문의 PK'
        primary key,
    type        varchar(20) default 'GENERAL'         null comment '문의 유형',
    title       varchar(200)                          not null comment '문의 제목',
    content     text                                  not null comment '문의 내용',
    answer      text                                  null comment '관리자 답변',
    status      varchar(20) default 'WAIT'            not null comment 'WAIT / DONE',
    created_at  datetime    default CURRENT_TIMESTAMP not null comment '등록 시각',
    answered_at datetime                              null comment '답변 완료 시각'
)
    charset = utf8mb4;

create table notices
(
    notice_id  bigint auto_increment comment '공지 PK'
        primary key,
    title      varchar(200)                       not null comment '제목',
    content    text                               not null comment '내용',
    is_popup   tinyint  default 0                 not null comment '팝업 여부',
    view_count int      default 0                 not null comment '조회수',
    created_at datetime default CURRENT_TIMESTAMP null comment '작성 시간',
    updated_at datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '수정 시간'
)
    charset = utf8mb4;

create table platform_daily_stats
(
    stat_date              date          not null comment '기준 일자'
        primary key,
    total_hosts            bigint        not null,
    new_hosts              bigint        not null,
    total_accommodations   bigint        not null,
    new_accommodations     bigint        not null,
    total_reservations     bigint        not null,
    reservations_success   bigint        not null,
    reservations_failed    bigint        not null,
    total_revenue          bigint        not null,
    active_guests          bigint        not null,
    active_hosts           bigint        not null,
    occupancy_rate         decimal(5, 2) not null comment '객실 점유율(%)',
    created_at             datetime      not null comment '생성 시각(배치 실행 시간)',
    cancel_count           bigint        not null,
    open_reports           bigint        not null,
    pending_accommodations bigint        not null,
    refund_amount          bigint        not null,
    refund_count           bigint        not null
)
    charset = utf8mb4;

create table review_tag
(
    review_tag_id    int auto_increment comment '태그 PK'
        primary key,
    review_tag_name  varchar(50)  not null comment '태그 이름',
    is_active        bit          not null,
    review_tag_image varchar(255) null
)
    charset = utf8mb4;

create table room
(
    room_id           bigint unsigned auto_increment
        primary key,
    accommodations_id bigint                             not null,
    room_name         varchar(100)                       not null,
    price             int                                null,
    weekend_price     int                                null,
    min_guests        int      default 2                 not null,
    max_guests        int                                null,
    room_description  text                               null,
    main_image_url    longtext                           null,
    room_status       int                                not null,
    bathroom_count    int                                null,
    room_type         varchar(50)                        null,
    bed_count         int                                null,
    create_room       datetime default CURRENT_TIMESTAMP null,
    change_info_room  datetime                           null,
    constraint room_ibfk_1
        foreign key (accommodations_id) references accommodation (accommodations_id)
            on delete cascade
)
    charset = utf8mb4;

create index accommodations_id
    on room (accommodations_id);

create table theme
(
    theme_id       bigint auto_increment
        primary key,
    theme_category varchar(50) not null,
    theme_name     varchar(50) not null,
    constraint UQ_THEME_NAME
        unique (theme_name)
)
    charset = utf8mb4;

create table accommodation_theme
(
    theme_mapping_id  bigint auto_increment
        primary key,
    theme_id          bigint not null,
    accommodations_id bigint not null,
    constraint accommodation_theme_ibfk_1
        foreign key (theme_id) references theme (theme_id)
            on delete cascade,
    constraint accommodation_theme_ibfk_2
        foreign key (accommodations_id) references accommodation (accommodations_id)
            on delete cascade
)
    charset = utf8mb4;

create index accommodations_id
    on accommodation_theme (accommodations_id);

create index theme_id
    on accommodation_theme (theme_id);

create table users
(
    user_id         bigint auto_increment comment '회원 PK'
        primary key,
    email           varchar(50)                                not null comment '로그인 ID, UNIQUE',
    password        varchar(255)                               null comment '소셜 로그인 시 NULL 가능',
    phone           varchar(50)                                null,
    role            varchar(20)                                not null,
    marketing_agree bit                                        not null,
    created_at      datetime                                   not null comment '가입일',
    updated_at      datetime                                   not null comment '수정일',
    host_approved   bit                                        null,
    name            varchar(100)                               null,
    nickname        varchar(50)                                not null,
    gender          varchar(255)                               null,
    social_id       varchar(100)                               null,
    social_provider enum ('GOOGLE', 'KAKAO', 'LOCAL', 'NAVER') null,
    constraint UQ_USERS_EMAIL
        unique (email),
    constraint nickname
        unique (nickname)
)
    charset = utf8mb4;

create table host_daily_stats
(
    user_id           bigint        not null comment '호스트 user_id (users.user_id)',
    stat_date         date          not null comment '기준 일자',
    reservation_count int           not null comment '예약 건수',
    reserved_nights   int           not null comment '총 숙박 박수',
    total_guests      int           not null comment '총 투숙 인원',
    revenue           int           not null comment '결제 완료 금액 합',
    canceled_count    int           not null comment '취소/실패 건수',
    avg_price         int           null comment '평균 요금(1박 기준)',
    review_count      int           null comment '등록 리뷰 수',
    avg_rating        decimal(2, 1) null comment '신규 리뷰 평균 평점',
    occupancy_rate    decimal(5, 2) null comment '객실 점유율 %',
    created_at        datetime      not null comment '생성 시각',
    updated_at        datetime      not null comment '갱신 시각',
    primary key (user_id, stat_date),
    constraint FK_HOST_DAILY_STATS_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create index IDX_HOST_DAILY_STATS_DATE
    on host_daily_stats (stat_date);

create table reservation
(
    reservation_id         bigint auto_increment comment '예약 PK'
        primary key,
    accommodations_id      bigint                   not null comment '숙소 PK',
    room_id                bigint                   null,
    user_id                bigint                   not null comment '예약자 회원 PK',
    checkin                datetime                 not null comment '체크인',
    checkout               datetime                 not null comment '체크아웃',
    stay_nights            int                      not null,
    guest_count            int                      not null,
    reservation_status     int                      not null,
    total_amount_before_dc int unsigned             not null comment '할인 전 합계',
    coupon_discount_amount int unsigned default '0' not null comment '쿠폰 할인액',
    final_payment_amount   int unsigned             not null comment '최종 결제 금액',
    payment_status         int                      not null,
    reserver_name          varchar(50)              not null comment '예약자 이름',
    reserver_phone         varchar(20)              not null comment '예약자 전화번호',
    created_at             datetime                 not null comment '생성 시각',
    updated_at             datetime                 not null comment '수정 시각',
    constraint FK_RESERVATION_ACC
        foreign key (accommodations_id) references accommodation (accommodations_id),
    constraint FK_RESERVATION_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table chat_room
(
    room_id           bigint auto_increment comment '채팅방 PK'
        primary key,
    accommodations_id bigint                             not null comment '숙소 PK',
    reservation_id    bigint                             not null comment '예약 PK',
    created_at        datetime default CURRENT_TIMESTAMP not null comment '생성 시각',
    constraint FK_CHAT_ROOM_ACC
        foreign key (accommodations_id) references accommodation (accommodations_id),
    constraint FK_CHAT_ROOM_RSV
        foreign key (reservation_id) references reservation (reservation_id)
)
    charset = utf8mb4;

create table chat_message
(
    message_id bigint auto_increment comment '메시지 PK'
        primary key,
    room_id    bigint                             not null comment '채팅방 PK',
    user_id    bigint                             not null comment '보낸 회원 PK',
    content    text                               not null comment '메시지 내용',
    sent_at    datetime default CURRENT_TIMESTAMP not null comment '전송 시각',
    constraint FK_CHAT_MESSAGE_ROOM
        foreign key (room_id) references chat_room (room_id),
    constraint FK_CHAT_MESSAGE_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table payment
(
    payment_id       bigint auto_increment comment '결제 PK'
        primary key,
    reservation_id   bigint       not null comment '예약 PK',
    pg_provider_code varchar(20)  not null comment 'PG사 코드 (TOSS 등)',
    payment_method   varchar(20)  not null comment '결제 수단',
    order_id         varchar(50)  not null comment '우리 주문번호',
    pg_payment_key   varchar(100) null comment 'PG 결제키',
    request_amount   int          not null comment '요청 금액',
    approved_amount  int          null comment '승인 금액',
    currency_code    varchar(3)   not null,
    payment_status   int          not null,
    failure_code     varchar(50)  null comment 'PG 실패 코드',
    failure_message  varchar(255) null comment '실패 사유',
    approved_at      datetime     null comment '승인 시각',
    created_at       datetime     not null comment '생성 시각',
    updated_at       datetime     not null comment '수정 시각',
    constraint FK_PAYMENT_RESERVATION
        foreign key (reservation_id) references reservation (reservation_id)
)
    charset = utf8mb4;

create table payment_refund
(
    refund_id         bigint auto_increment comment '환불 PK'
        primary key,
    payment_id        bigint       not null comment '결제 PK',
    refund_amount     int          not null comment '환불 요청 금액',
    refund_status     int          not null,
    pg_refund_key     varchar(100) null comment 'PG 환불 키',
    pg_transaction_id varchar(100) null comment 'PG 트랜잭션 ID',
    failure_code      varchar(50)  null comment 'PG 실패 코드',
    failure_message   varchar(255) null comment '실패 사유',
    reason_code       varchar(50)  null comment '환불 사유 코드',
    reason_message    varchar(255) null comment '환불 사유 상세',
    requested_by      varchar(50)  not null comment '요청 주체',
    requested_at      datetime     not null comment '요청 시각',
    approved_at       datetime     null comment '성공 시각',
    created_at        datetime     not null comment '생성 시각',
    updated_at        datetime     not null comment '수정 시각',
    constraint FK_REFUND_PAYMENT
        foreign key (payment_id) references payment (payment_id)
)
    charset = utf8mb4;

create table review
(
    review_id         bigint auto_increment comment '리뷰 PK'
        primary key,
    accommodations_id bigint                               not null comment '숙소 PK',
    user_id           bigint                               not null comment '회원 PK',
    rating            decimal(2, 1)                        not null comment '평점 0.0~5.0',
    content           text                                 not null comment '리뷰 본문',
    created_at        datetime   default CURRENT_TIMESTAMP not null comment '작성 시각',
    updated_at        datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '수정 시각',
    is_deleted        tinyint(1) default 1                 not null comment '삭제 플래그 (0/1)',
    author_name       varchar(100)                         null,
    is_crawled        bit                                  not null,
    visit_date        varchar(50)                          null,
    constraint FK_REVIEW_ACC
        foreign key (accommodations_id) references accommodation (accommodations_id),
    constraint FK_REVIEW_USER
        foreign key (user_id) references users (user_id),
    constraint CK_REVIEW_RATING
        check ((`rating` >= 0.0) and (`rating` <= 5.0))
)
    charset = utf8mb4;

create table review_image
(
    review_image_id  bigint auto_increment comment '리뷰 이미지 PK'
        primary key,
    review_id        bigint                             not null comment '리뷰 PK',
    review_image_url varchar(500)                       not null,
    sort_order       int      default 1                 null comment '노출 순서',
    created_at       datetime default CURRENT_TIMESTAMP not null comment '생성 시각',
    constraint FK_REVIEW_IMAGE_REVIEW
        foreign key (review_id) references review (review_id)
)
    charset = utf8mb4;

create table review_replies
(
    reply_id   bigint auto_increment comment '답글 PK'
        primary key,
    review_id  bigint                             not null comment '리뷰 PK',
    user_id    bigint                             not null comment '회원 PK',
    content    text                               not null comment '답글 내용',
    created_at datetime default CURRENT_TIMESTAMP not null comment '생성 시각',
    updated_at datetime                           null comment '수정 시각',
    constraint FK_REVIEW_REPLIES_REVIEW
        foreign key (review_id) references review (review_id),
    constraint FK_REVIEW_REPLIES_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table review_reports
(
    report_id  bigint auto_increment comment '신고 PK'
        primary key,
    review_id  bigint                             not null comment '리뷰 PK',
    user_id    bigint                             not null comment '회원 PK',
    reason     text                               not null comment '신고 사유',
    state      varchar(20)                        not null comment 'WAIT / CHECKED / BLINDED 등',
    created_at datetime default CURRENT_TIMESTAMP not null comment '신고 접수 시각',
    updated_at datetime                           null comment '처리 완료 시각',
    constraint FK_REVIEW_REPORT_REVIEW
        foreign key (review_id) references review (review_id),
    constraint FK_REVIEW_REPORT_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table review_tag_map
(
    id            bigint unsigned auto_increment comment '매핑 PK'
        primary key,
    review_tag_id int    not null comment '태그 PK',
    review_id     bigint not null comment '리뷰 PK',
    constraint FK_REVIEW_TAG_MAP_REVIEW
        foreign key (review_id) references review (review_id),
    constraint FK_REVIEW_TAG_MAP_TAG
        foreign key (review_tag_id) references review_tag (review_tag_id)
)
    charset = utf8mb4;

create table user_coupon
(
    id         bigint unsigned auto_increment comment '유저-쿠폰 PK'
        primary key,
    coupon_id  bigint unsigned              not null comment '쿠폰 PK',
    user_id    bigint                       not null comment '회원 PK',
    issued_at  datetime                     not null comment '발급 시각',
    used_at    datetime                     null comment '사용 시각',
    expired_at datetime                     null comment '만료 시각',
    status     varchar(20) default 'ISSUED' not null comment 'ISSUED / USED / EXPIRED',
    constraint FK_USER_COUPON_COUPON
        foreign key (coupon_id) references coupon (coupon_id),
    constraint FK_USER_COUPON_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table user_social
(
    social_id     bigint auto_increment comment '소셜 PK'
        primary key,
    user_id       bigint                                     not null comment '회원 PK',
    provider      enum ('GOOGLE', 'KAKAO', 'LOCAL', 'NAVER') not null,
    provider_uid  varchar(255)                               not null comment '프로바이더 고유값',
    email         varchar(255)                               null comment '소셜 이메일',
    profile_image varchar(255)                               null comment '프로필 이미지',
    created_at    datetime                                   not null comment '최초 로그인',
    constraint UQ_USER_SOCIAL
        unique (provider, provider_uid),
    constraint FK_USER_SOCIAL_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table user_theme
(
    id       bigint auto_increment comment '선택 PK'
        primary key,
    user_id  bigint not null comment '회원 PK',
    theme_id bigint not null comment '테마 PK',
    constraint FK_USER_THEME_THEME
        foreign key (theme_id) references theme (theme_id),
    constraint FK_USER_THEME_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

create table wishlist
(
    wish_id           bigint auto_increment comment '위시리스트 PK'
        primary key,
    accommodations_id bigint   not null comment '숙소 PK',
    created_at        datetime not null comment '추가 시각',
    user_id           bigint   not null comment '회원 PK',
    constraint FK_WISHLIST_ACC
        foreign key (accommodations_id) references accommodation (accommodations_id),
    constraint FK_WISHLIST_USER
        foreign key (user_id) references users (user_id)
)
    charset = utf8mb4;

