-- 대기 목록(Waitlist) 시스템 강화를 위한 컬럼 추가
-- notified_at: 알림 발송 시각
-- expires_at: 예약 기회 만료 시각 (알림 후 24시간)

ALTER TABLE waitlist ADD COLUMN notified_at DATETIME NULL AFTER is_notified;
ALTER TABLE waitlist ADD COLUMN expires_at DATETIME NULL AFTER notified_at;

-- 만료된 알림 조회를 위한 인덱스
CREATE INDEX idx_waitlist_expires_at ON waitlist(is_notified, expires_at);

-- 체크인 날짜 기준 조회를 위한 인덱스
CREATE INDEX idx_waitlist_checkin ON waitlist(checkin);
