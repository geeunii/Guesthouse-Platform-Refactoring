-- ============================================
-- 이미지 URL 업데이트 (폴더 구조 유지 버전)
-- ============================================

-- accommodation_image: 폴더 구조 유지
UPDATE accommodation_image
SET image_url = REPLACE(
    image_url,
    '/accommodation_image/',
    '/resizing_accommodation/'
)
WHERE image_url LIKE '%kr.object.ncloudstorage.com/guesthouse/accommodation_image/%';

-- 확인
SELECT image_id, image_url FROM accommodation_image WHERE image_url LIKE '%resizing_accommodation%' LIMIT 5;

-- room: 폴더 구조 유지
UPDATE room
SET main_image_url = REPLACE(
    main_image_url,
    '/room/',
    '/resizing_room/'
)
WHERE main_image_url LIKE '%kr.object.ncloudstorage.com/guesthouse/room/%';

-- 확인
SELECT room_id, main_image_url FROM room WHERE main_image_url LIKE '%resizing_room%' LIMIT 5;

SELECT image_id, image_url FROM accommodation_image WHERE image_id = 1;

-- 1. accommodation_image 테이블에 데이터가 있나?
SELECT COUNT(*) as total_count FROM accommodation_image;

-- 2. 어떤 image_id들이 있나?
SELECT image_id, image_url FROM accommodation_image LIMIT 10;

-- 3. URL이 바뀐 게 있나?
SELECT COUNT(*) FROM accommodation_image WHERE image_url LIKE '%resizing_accommodation%';

-- 4. 원본 URL이 남아 있나?
SELECT COUNT(*) FROM accommodation_image WHERE image_url LIKE '%accommodation_image/%';


-- 혹시 이미 UPDATE 했다면 롤백
UPDATE accommodation_image
SET image_url = REPLACE(image_url, '/resizing_accommodation/', '/accommodation_image/')
WHERE image_url LIKE '%resizing_accommodation%';

UPDATE room
SET main_image_url = REPLACE(main_image_url, '/resizing_room/', '/room/')
WHERE main_image_url LIKE '%resizing_room%';
