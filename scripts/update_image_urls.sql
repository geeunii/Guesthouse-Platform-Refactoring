-- ============================================
-- 이미지 URL 업데이트 스크립트
-- 기존: accommodation_image/4/xxx.jpg
-- 신규: resizing_accommodation/xxx.jpg
-- ============================================

-- 1. accommodation_image 테이블 백업 (선택사항, 안전을 위해)
-- CREATE TABLE accommodation_image_backup AS SELECT * FROM accommodation_image;

-- 2. accommodation_image 테이블 URL 업데이트
UPDATE accommodation_image
SET image_url = CONCAT(
    'https://kr.object.ncloudstorage.com/guesthouse/resizing_accommodation/',
    SUBSTRING_INDEX(image_url, '/', -1)  -- 파일명만 추출
)
WHERE image_url LIKE '%kr.object.ncloudstorage.com/guesthouse/accommodation_image/%';

-- 확인
SELECT 
    image_id,
    image_url,
    '변경됨' as status
FROM accommodation_image 
WHERE image_url LIKE '%resizing_accommodation%'
LIMIT 5;

-- ============================================
-- 3. room 테이블 백업 (선택사항)
-- CREATE TABLE room_backup AS SELECT * FROM room;

-- 4. room 테이블 URL 업데이트
UPDATE room
SET main_image_url = CONCAT(
    'https://kr.object.ncloudstorage.com/guesthouse/resizing_room/',
    SUBSTRING_INDEX(main_image_url, '/', -1)  -- 파일명만 추출
)
WHERE main_image_url LIKE '%kr.object.ncloudstorage.com/guesthouse/room/%';

-- 확인
SELECT 
    room_id,
    main_image_url,
    '변경됨' as status
FROM room 
WHERE main_image_url LIKE '%resizing_room%'
LIMIT 5;

-- ============================================
-- 전체 통계 확인
-- ============================================

-- accommodation_image 업데이트 건수
SELECT 
    'accommodation_image' as table_name,
    COUNT(*) as updated_count
FROM accommodation_image 
WHERE image_url LIKE '%resizing_accommodation%';

-- room 업데이트 건수
SELECT 
    'room' as table_name,
    COUNT(*) as updated_count
FROM room 
WHERE main_image_url LIKE '%resizing_room%';

-- ============================================
-- 롤백 방법 (문제 발생 시)
-- ============================================
-- 백업 테이블에서 복원:
-- UPDATE accommodation_image a 
-- INNER JOIN accommodation_image_backup b ON a.image_id = b.image_id
-- SET a.image_url = b.image_url;

-- UPDATE room r 
-- INNER JOIN room_backup b ON r.room_id = b.room_id
-- SET r.main_image_url = b.main_image_url;
