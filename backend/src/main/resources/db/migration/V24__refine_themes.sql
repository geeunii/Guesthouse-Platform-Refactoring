-- V24__refine_themes.sql

-- 1. 포틀럭 카테고리 변경
UPDATE theme SET theme_category = 'PARTY' WHERE theme_name = '포틀럭';

-- 2. 미사용 테마 삭제
DELETE FROM accommodation_theme WHERE theme_id IN (SELECT theme_id FROM theme WHERE theme_name IN ('자쿠지', '넷플릭스/OTT', '스탠바이미'));
DELETE FROM user_theme WHERE theme_id IN (SELECT theme_id FROM theme WHERE theme_name IN ('자쿠지', '넷플릭스/OTT', '스탠바이미'));
DELETE FROM theme WHERE theme_name IN ('자쿠지', '넷플릭스/OTT', '스탠바이미');

-- 3. 신규 테마 추가
INSERT IGNORE INTO theme (theme_category, theme_name, theme_image_url) VALUES 
('ACTIVITY', '스냅 촬영', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Camera%20with%20flash/3D/camera_with_flash_3d.png'),
('NATURE', '마운틴뷰', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Mountain/3D/mountain_3d.png');

-- 4. 테마 이름 변경
UPDATE theme SET theme_name = '외향인 파티' WHERE theme_name = '소규모 파티';
UPDATE theme SET theme_name = '한옥/구옥' WHERE theme_name = '한옥';

-- 5. 반려동물 테마 통합
UPDATE theme SET theme_name = '반려동물' WHERE theme_name = '냥집사/멍집사';
DELETE FROM theme WHERE theme_name = '반려동물 동반';

-- 6. 테마 순서 변경 (솔로 <-> 바비큐)
-- ID 기반 스왑은 로컬 DB 상태에 의존하므로, 이름 기반으로 안전하게 업데이트 (Unique Key 충돌 방지 포함)
UPDATE theme SET theme_name = 'TEMP_SWAP_THEME' WHERE theme_name = '솔로 파티';

UPDATE theme 
SET theme_name = '솔로 파티', 
    theme_image_url = 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Heart%20on%20fire/3D/heart_on_fire_3d.png' 
WHERE theme_name = '바비큐 파티';

UPDATE theme 
SET theme_name = '바비큐 파티', 
    theme_image_url = 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Meat%20on%20bone/3D/meat_on_bone_3d.png' 
WHERE theme_name = 'TEMP_SWAP_THEME';

-- 7. 숙소 매핑 추가
INSERT IGNORE INTO accommodation_theme (accommodations_id, theme_id) 
SELECT accommodations_id, (SELECT theme_id FROM theme WHERE theme_name='러닝' LIMIT 1) FROM accommodation WHERE accommodations_description LIKE '%러닝%' OR accommodations_description LIKE '%달리기%' OR accommodations_description LIKE '%조깅%';

INSERT IGNORE INTO accommodation_theme (accommodations_id, theme_id) 
SELECT accommodations_id, (SELECT theme_id FROM theme WHERE theme_name='등산' LIMIT 1) FROM accommodation WHERE accommodations_description LIKE '%등산%' OR accommodations_description LIKE '%한라산%' OR accommodations_description LIKE '%오름%' OR accommodations_description LIKE '%트레킹%';

INSERT IGNORE INTO accommodation_theme (accommodations_id, theme_id) 
SELECT accommodations_id, (SELECT theme_id FROM theme WHERE theme_name='스냅 촬영' LIMIT 1) FROM accommodation WHERE accommodations_description LIKE '%스냅%' OR accommodations_description LIKE '%사진%' OR accommodations_description LIKE '%촬영%';

INSERT IGNORE INTO accommodation_theme (accommodations_id, theme_id) 
SELECT accommodations_id, (SELECT theme_id FROM theme WHERE theme_name='마운틴뷰' LIMIT 1) FROM accommodation WHERE accommodations_description LIKE '%마운틴뷰%' OR accommodations_description LIKE '%산 전망%' OR accommodations_description LIKE '%오름%' OR accommodations_description LIKE '%숲%';
