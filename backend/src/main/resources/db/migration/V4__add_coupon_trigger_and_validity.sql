-- 쿠폰 테이블 확장: 트리거 타입, 유효기간 타입 컬럼 추가

-- 1. 기존 컬럼 NULL 허용 및 기본값 설정 (DAYS_FROM_ISSUE 타입 쿠폰을 위해)
ALTER TABLE coupon MODIFY COLUMN min_price INT NULL DEFAULT 0;
ALTER TABLE coupon MODIFY COLUMN valid_from DATETIME NULL;
ALTER TABLE coupon MODIFY COLUMN valid_to DATETIME NULL;
ALTER TABLE coupon MODIFY COLUMN created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. 새 컬럼 추가
-- trigger_type: 쿠폰 발급 조건 (SIGNUP, REVIEW_3, REVIEW_10, FIRST_RESERVATION, DOWNLOAD, EVENT)
ALTER TABLE coupon ADD COLUMN trigger_type VARCHAR(30) NULL;

-- validity_type: 유효기간 계산 방식 (DAYS_FROM_ISSUE: 발급일 기준 N일, FIXED_PERIOD: 고정 기간)
ALTER TABLE coupon ADD COLUMN validity_type VARCHAR(20) NULL;

-- validity_days: 발급일 기준 유효일수 (validity_type = DAYS_FROM_ISSUE일 때 사용)
ALTER TABLE coupon ADD COLUMN validity_days INT NULL;

-- accommodations_id: 숙소별 이벤트 쿠폰일 경우 (nullable)
ALTER TABLE coupon ADD COLUMN accommodations_id BIGINT NULL;

-- 3. 인덱스 추가
CREATE INDEX idx_coupon_trigger_type ON coupon(trigger_type);
CREATE INDEX idx_coupon_accommodations_id ON coupon(accommodations_id);

-- 4. 기존 데이터 마이그레이션 (REVIEW_3_REWARD 쿠폰이 있다면 trigger_type 설정)
UPDATE coupon SET trigger_type = 'REVIEW_3', validity_type = 'DAYS_FROM_ISSUE', validity_days = 90
WHERE code = 'REVIEW_3_REWARD';
