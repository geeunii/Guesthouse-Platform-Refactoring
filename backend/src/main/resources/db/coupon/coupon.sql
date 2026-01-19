CREATE TABLE IF NOT EXISTS coupon
(
    coupon_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '쿠폰 PK',
    code           VARCHAR(50)     NOT NULL COMMENT '쿠폰 식별 코드 (UNIQUE)',
    name           VARCHAR(100)    NOT NULL COMMENT '쿠폰 이름',
    description    VARCHAR(255)    NULL COMMENT '쿠폰 설명',
    discount_type  VARCHAR(20)     NOT NULL COMMENT 'AMOUNT / PERCENT',
    discount_value INT             NOT NULL COMMENT '할인 금액 또는 할인율',
    min_price      INT             NOT NULL COMMENT '최소 적용 금액',
    max_discount   INT             NULL COMMENT '정율 할인 상한',
    valid_from     DATETIME        NOT NULL COMMENT '시작 일시',
    valid_to       DATETIME        NOT NULL COMMENT '종료 일시',
    is_active      TINYINT         NOT NULL DEFAULT 1 COMMENT '사용 가능 여부 (0/1)',
    created_at     DATETIME        NOT NULL COMMENT '생성 시각',
    CONSTRAINT PK_COUPON PRIMARY KEY (coupon_id),
    CONSTRAINT UQ_COUPON_CODE UNIQUE (code)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4;

ALTER TABLE coupon ADD COLUMN trigger_type VARCHAR(30);      -- SIGNUP, REVIEW_3, EVENT 등
ALTER TABLE coupon ADD COLUMN validity_type VARCHAR(20);     -- DAYS_FROM_ISSUE 발급일 기준 N일 / FIXED_PERIOD 고정 기간
ALTER TABLE coupon ADD COLUMN validity_days INT;             -- 발급일 기준 유효일수 (90일 등)
ALTER TABLE coupon ADD COLUMN accommodations_id BIGINT;      -- 숙소별 쿠폰일 경우 (nullable)

CREATE TABLE IF NOT EXISTS coupon_inventory
(
    inventory_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '선착순 쿠폰 재고 PK',
    coupon_id        BIGINT UNSIGNED NOT NULL UNIQUE COMMENT '쿠폰 PK',
    daily_limit      INT             NOT NULL COMMENT '하루 발급 가능 수량',
    available_today  INT             NOT NULL COMMENT '오늘 남은 수량',
    last_reset_date  DATE            NULL COMMENT '마지막 초기화 일자',
    CONSTRAINT PK_COUPON_INVENTORY PRIMARY KEY (inventory_id),
    CONSTRAINT FK_COUPON_INVENTORY_COUPON FOREIGN KEY (coupon_id) REFERENCES coupon (coupon_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================
-- 1. 자동 발급 쿠폰 (시스템에서 조건 충족 시 자동 발급)
-- ============================================

-- 회원가입 축하 쿠폰 (자동 발급)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('WELCOME', '회원가입 축하 쿠폰', '첫 예약에 사용하세요!', 'FIXED', 5000, 30000, 'SIGNUP', 'DAYS_FROM_ISSUE', 30, 1);

-- 리뷰 3회 달성 쿠폰 (자동 발급)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, is_active)
VALUES ('REVIEW_3_REWARD', '리뷰 감사 쿠폰', '리뷰 3회 작성 감사합니다!', 'PERCENT', 10, 50000, 15000, 'REVIEW_3', 'DAYS_FROM_ISSUE', 90, 1);

-- 리뷰 10회 달성 쿠폰 (자동 발급)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, is_active)
VALUES ('REVIEW_10_REWARD', '리뷰 마니아 쿠폰', '리뷰 10회 달성! VIP 고객님 감사합니다', 'PERCENT', 20, 100000, 30000, 'REVIEW_10', 'DAYS_FROM_ISSUE', 180, 1);

-- 첫 예약 완료 쿠폰 (자동 발급)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('FIRST_BOOKING', '첫 예약 감사 쿠폰', '다음 예약에 사용하세요!', 'FIXED', 10000, 50000, 'FIRST_RESERVATION', 'DAYS_FROM_ISSUE', 60, 1);


-- ============================================
-- 2. 다운로드 쿠폰 (유저가 직접 받기 버튼 클릭)
-- ============================================

-- 사이트 이벤트 쿠폰 - 여름 특가 (고정 기간)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, valid_from, valid_to, is_active)
VALUES ('SUMMER_2025', '여름 특가 쿠폰', '시원한 여름 휴가를 게하방과 함께!', 'PERCENT', 15, 80000, 25000, 'DOWNLOAD', 'FIXED_PERIOD', '2025-07-01 00:00:00', '2025-08-31 23:59:59', 1);

-- 사이트 이벤트 쿠폰 - 신년 특가 (고정 기간)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, valid_from, valid_to, is_active)
VALUES ('NEWYEAR_2025', '새해 특별 할인', '2025년 새해 복 많이 받으세요!', 'FIXED', 15000, 70000, 'DOWNLOAD', 'FIXED_PERIOD', '2025-01-01 00:00:00', '2025-01-31 23:59:59', 1);

-- 사이트 이벤트 쿠폰 - 주말 특가 (발급일 기준)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, is_active)
VALUES ('WEEKEND_SALE', '주말 특가 쿠폰', '주말 예약 시 할인!', 'PERCENT', 12, 60000, 20000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 14, 1);

-- 매일 00시에 초기화되는 선착순 쿠폰 (하루 50장 한정)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('DAILY_FLASH50', '오늘의 선착순 50', '매일 자정 50장 한정, 7천원 즉시 할인', 'FIXED', 7000, 30000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 2, 1);

INSERT INTO coupon_inventory (coupon_id, daily_limit, available_today, last_reset_date)
VALUES ((SELECT coupon_id FROM coupon WHERE code = 'DAILY_FLASH50'), 50, 50, CURRENT_DATE());

-- 봄철 벚꽃 여행 프로모션 (고정 기간)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, valid_from, valid_to, is_active)
VALUES ('SPRING_BLOSSOM', '벚꽃 여행 기획전', '3~4월 벚꽃 시즌 게스트하우스 전용 18% 할인', 'PERCENT', 18, 70000, 30000, 'DOWNLOAD', 'FIXED_PERIOD', '2025-03-15 00:00:00', '2025-04-30 23:59:59', 1);

-- 평일 전용 힐링 쿠폰 (발급일 기준 21일)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('WEEKDAY_CHILL', '평일 힐링 8천원 쿠폰', '월~목 체크인 고객을 위한 정액 할인', 'FIXED', 8000, 40000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 21, 1);

-- 3박 이상 장기 숙박 프로모션
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, is_active)
VALUES ('LONGSTAY_3N', '3박 이상 장기숙박 할인', '게스트하우스에서 3박 이상 예약 시 20% 할인', 'PERCENT', 20, 150000, 40000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 30, 1);

-- 비 오는 날 감성 스테이 이벤트
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('RAINY_DAY_STAY', '우천 예보 한정 12,000원', '비 오는 날에도 아늑한 감성 스테이를 즐겨보세요', 'FIXED', 12000, 50000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 10, 1);

-- 파티형 게스트하우스 전용 쿠폰 (accommodations_id = 32 가정)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, accommodations_id, is_active)
VALUES ('PARTY_GH_25', '파티 게스트하우스 25% 할인', '제주 게토 등 파티형 게스트하우스 한정 혜택', 'PERCENT', 25, 120000, 60000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 20, 32, 1);

-- 숙소 전용 쿠폰 - 제주 리조트 (accommodations_id = 1 가정)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, accommodations_id, is_active)
VALUES ('JEJU_RESORT_10', '제주 리조트 전용 쿠폰', '제주 리조트에서만 사용 가능', 'PERCENT', 10, 100000, 20000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 30, 1, 1);

-- 숙소 전용 쿠폰 - 서울 게스트하우스 (accommodations_id = 2 가정)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, accommodations_id, is_active)
VALUES ('SEOUL_GH_5000', '서울 게스트하우스 할인', '서울 게스트하우스 전용 5천원 할인', 'FIXED', 5000, 30000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 30, 2, 1);

-- 숙소 전용 쿠폰 - 부산 오션뷰 (accommodations_id = 3 가정)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, accommodations_id, is_active)
VALUES ('BUSAN_OCEAN_15', '부산 오션뷰 특별 할인', '부산 바다가 보이는 숙소 전용', 'PERCENT', 15, 80000, 25000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 45, 3, 1);

-- 파티형 게스트하우스 전용 쿠폰 (accommodations_id = 32 가정)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, accommodations_id, is_active)
VALUES ('PARTY_GH_25', '파티 게스트하우스 25% 할인', '제주 게토 등 파티형 게스트하우스 한정 혜택', 'PERCENT', 25, 120000, 60000, 'DOWNLOAD', 'DAYS_FROM_ISSUE', 20, 32, 1);


-- ============================================
-- 3. 관리자 수동 지급 쿠폰 (EVENT)
-- ============================================

-- VIP 고객 전용 쿠폰
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('VIP_SPECIAL', 'VIP 특별 쿠폰', 'VIP 고객님을 위한 특별 혜택', 'FIXED', 30000, 100000, 'EVENT', 'DAYS_FROM_ISSUE', 90, 1);

-- 고객 불만 처리용 쿠폰
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('SORRY_COUPON', '불편 사과 쿠폰', '불편을 드려 죄송합니다', 'FIXED', 20000, 50000, 'EVENT', 'DAYS_FROM_ISSUE', 60, 1);

-- 프로모션 당첨 쿠폰
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, is_active)
VALUES ('LUCKY_WINNER', '행운의 당첨 쿠폰', '이벤트 당첨을 축하드립니다!', 'PERCENT', 25, 80000, 40000, 'EVENT', 'DAYS_FROM_ISSUE', 30, 1);

-- 야간 체크인 웰컴 기프트
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, trigger_type, validity_type, validity_days, is_active)
VALUES ('LATE_CHECKIN', '야간 체크인 힐링 쿠폰', '장거리 이동 후 늦은 도착 고객을 위한 15,000원 할인', 'FIXED', 15000, 60000, 'EVENT', 'DAYS_FROM_ISSUE', 45, 1);

-- 리오프닝 및 신규 호스트 런칭 프로모션
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_price, max_discount, trigger_type, validity_type, validity_days, is_active)
VALUES ('REOPENING_STAR', '리오프닝 축하 30% 쿠폰', '신규 호스트 런칭 & 리노베이션 기념 한정 혜택', 'PERCENT', 30, 100000, 70000, 'EVENT', 'DAYS_FROM_ISSUE', 25, 1);


# coupon_id        | PK
#   code             | REVIEW_3_REWARD, SIGNUP_WELCOME, EVENT_SUMMER_2025
#   name             | 리뷰 감사 쿠폰, 회원가입 축하 쿠폰
#   discount_type    | PERCENT / FIXED
#   discount_value   | 10 (10%) / 5000 (5000원)
#   trigger_type     | SIGNUP / REVIEW_3 / REVIEW_10 / EVENT / DOWNLOAD
#   validity_type    | DAYS_FROM_ISSUE / FIXED_PERIOD
#   validity_days    | 90 (DAYS_FROM_ISSUE일 때 사용)
#   valid_from       | 2025-01-01 (FIXED_PERIOD일 때 사용)
#   valid_to         | 2025-01-31 (FIXED_PERIOD일 때 사용)
#   accommodations_id| NULL 또는 숙소ID (숙소별 쿠폰)
#   is_active        | true/false


ALTER TABLE coupon MODIFY COLUMN min_price INT NULL DEFAULT 0;
ALTER TABLE coupon MODIFY COLUMN valid_from DATETIME NULL;
ALTER TABLE coupon MODIFY COLUMN valid_to DATETIME NULL;
ALTER TABLE coupon MODIFY COLUMN created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP;
