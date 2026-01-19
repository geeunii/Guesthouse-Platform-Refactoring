-- 테스트용 기본 참조 데이터
-- 모든 테스트에서 공통으로 사용

-- account_number 기본 데이터 (외래 키 참조용)
INSERT IGNORE INTO account_number (account_number_id, bank_name, account_number, account_holder)
VALUES (1, '테스트은행', '000-0000-0000', '테스트계좌');

-- users 기본 데이터 (외래 키 참조용)
INSERT IGNORE INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved, nickname)
VALUES (1, 'test@example.com', 'password123', '010-0000-0000', 'HOST', 0, NOW(), NOW(), 1, 'testhost1');

-- 추가 테스트 유저들 (WishlistServiceTest에서 userId 2~7 사용)
INSERT IGNORE INTO users (user_id, email, password, phone, role, marketing_agree, created_at, updated_at, host_approved, nickname)
VALUES
(2, 'test2@example.com', 'password123', '010-0000-0002', 'USER', 0, NOW(), NOW(), NULL, 'testuser2'),
(3, 'test3@example.com', 'password123', '010-0000-0003', 'USER', 0, NOW(), NOW(), NULL, 'testuser3'),
(4, 'test4@example.com', 'password123', '010-0000-0004', 'USER', 0, NOW(), NOW(), NULL, 'testuser4'),
(5, 'test5@example.com', 'password123', '010-0000-0005', 'USER', 0, NOW(), NOW(), NULL, 'testuser5'),
(6, 'test6@example.com', 'password123', '010-0000-0006', 'USER', 0, NOW(), NOW(), NULL, 'testuser6'),
(7, 'test7@example.com', 'password123', '010-0000-0007', 'USER', 0, NOW(), NOW(), NULL, 'testuser7');

-- 편의시설 기본 데이터 (AccommodationServiceTest에서 사용)
INSERT IGNORE INTO amenity (amenity_id, amenity_code, amenity_name, is_active, display_order)
VALUES
(1, 'WIFI', '무료 와이파이', 1, 1),
(2, 'PARKING', '주차장', 1, 2),
(3, 'BREAKFAST', '조식 제공', 1, 3);

-- 테마 기본 데이터 (AccommodationServiceTest에서 사용)
INSERT IGNORE INTO theme (theme_id, theme_category, theme_name)
VALUES
(1, 'NATURE', '자연'),
(2, 'CITY', '도시');
