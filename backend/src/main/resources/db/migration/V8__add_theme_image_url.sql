-- Add theme_image_url column to theme table
-- ALTER TABLE theme ADD COLUMN theme_image_url VARCHAR(500);

-- Update theme image URLs for AROUND_THEME category
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1505142468610-359e7d316be0?w=500' WHERE theme_name = '바닷가';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=500' WHERE theme_name = '계곡';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=500' WHERE theme_name = '해수욕장';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500' WHERE theme_name = '수상레저';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=500' WHERE theme_name = '갯벌';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1567438050740-82a7e9c17f42?w=500' WHERE theme_name = '워터파크';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1548013146-72479768bada?w=500' WHERE theme_name = '문화유적';

-- Update theme image URLs for ACTIVITY_COMMUNITY category
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1525498128493-380d1990a112?w=500' WHERE theme_name = '불멍';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=500' WHERE theme_name = '포틀럭';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=500' WHERE theme_name = '러닝';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=500' WHERE theme_name = '서핑';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=500' WHERE theme_name = '하이킹';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=500' WHERE theme_name = '신나는 게스트하우스';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=500' WHERE theme_name = '조용한 게스트하우스';

-- Update theme image URLs for LOCATION category
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=500' WHERE theme_name = '공항 주변';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1495567720989-cebdbdd97913?w=500' WHERE theme_name = '노을 맛집(노을 명소)';

-- Update theme image URLs for LIFESTYLE category
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=500' WHERE theme_name = '여성 전용';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=500' WHERE theme_name = '1인실';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=500' WHERE theme_name = '독서';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1554048612-b6a482bc67e5?w=500' WHERE theme_name = '스냅 촬영';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=500' WHERE theme_name = 'MBTI E';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?w=500' WHERE theme_name = 'MBTI I';

-- Update theme image URLs for FOOD category
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?w=500' WHERE theme_name = '조식';
UPDATE theme SET theme_image_url = 'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=500' WHERE theme_name = '오마카세';
