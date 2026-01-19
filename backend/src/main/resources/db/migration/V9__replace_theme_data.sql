-- ========================================
-- 테마 데이터 교체 및 숙소-테마 재매핑 스크립트
-- ========================================

-- 1. 기존 숙소-테마 매핑 삭제
DELETE FROM accommodation_theme;

-- 2. 기존 user_theme 삭제 (선호 테마)
DELETE FROM user_theme;

-- 3. 기존 테마 삭제
DELETE FROM theme;

-- 4. 새 테마 삽입 (theme_id 자동 증가)
INSERT INTO theme (theme_category, theme_name, theme_image_url) VALUES
-- 1. NATURE (자연)
('NATURE', '바닷가', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Beach%20with%20umbrella/3D/beach_with_umbrella_3d.png'),
('NATURE', '해수욕장', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Umbrella%20on%20ground/3D/umbrella_on_ground_3d.png'),
('NATURE', '계곡', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/National%20park/3D/national_park_3d.png'),
('NATURE', '갯벌', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Crab/3D/crab_3d.png'),
('NATURE', '숲세권', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Deciduous%20tree/3D/deciduous_tree_3d.png'),
('NATURE', '노을 맛집', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Sunset/3D/sunset_3d.png'),
('NATURE', '별구경 명소', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Milky%20Way/3D/milky_way_3d.png'),

-- 2. CULTURE (문화)
('CULTURE', '문화유적', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Classical%20building/3D/classical_building_3d.png'),

-- 3. ACTIVITY (액티비티)
('ACTIVITY', '서핑', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Person%20surfing%3A%20light%20skin%20tone/3D/person_surfing_light_skin_tone_3d.png'),
('ACTIVITY', '수상레저', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Speedboat/3D/speedboat_3d.png'),
('ACTIVITY', '워터파크', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Playground%20slide/3D/playground_slide_3d.png'),
('ACTIVITY', '러닝', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Person%20running/3D/person_running_3d.png'),
('ACTIVITY', '하이킹', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Hiking%20boot/3D/hiking_boot_3d.png'),
('ACTIVITY', '요가&명상', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Person%20in%20lotus%20position/3D/person_in_lotus_position_3d.png'),
('ACTIVITY', '낚시', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Fishing%20pole/3D/fishing_pole_3d.png'),
('ACTIVITY', '스냅 촬영', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Camera%20with%20flash/3D/camera_with_flash_3d.png'),

-- 4. VIBE (분위기)
('VIBE', 'MBTI E', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Loudspeaker/3D/loudspeaker_3d.png'),
('VIBE', 'MBTI I', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Headphone/3D/headphone_3d.png'),
('VIBE', '신나는 게스트하우스', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Mirror%20ball/3D/mirror_ball_3d.png'),
('VIBE', '조용한 게스트하우스', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Candle/3D/candle_3d.png'),
('VIBE', '불멍', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Fire/3D/fire_3d.png'),
('VIBE', '촌캉스', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Woman%E2%80%99s%20hat/3D/woman%E2%80%99s_hat_3d.png'),
('VIBE', '한옥', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Japanese%20castle/3D/japanese_castle_3d.png'),
('VIBE', '감성숙소', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Sparkles/3D/sparkles_3d.png'),
('VIBE', 'LP/바이닐', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Musical%20notes/3D/musical_notes_3d.png'),

-- 5. PARTY (파티/주류)
('PARTY', '소규모 파티', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Clinking%20glasses/3D/clinking_glasses_3d.png'),
('PARTY', '클럽/EDM', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Party%20popper/3D/party_popper_3d.png'),
('PARTY', '혼술 환영', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Tropical%20drink/3D/tropical_drink_3d.png'),
('PARTY', '와인/위스키', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Tumbler%20glass/3D/tumbler_glass_3d.png'),
('PARTY', '전통주/막걸리', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Sake/3D/sake_3d.png'),
('PARTY', '맥주 무제한', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Beer%20mug/3D/beer_mug_3d.png'),

-- 6. MEETING (만남/로맨스)
('MEETING', '썸 맛집', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Heart%20on%20fire/3D/heart_on_fire_3d.png'),
('MEETING', '솔로 탈출', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Key/3D/key_3d.png'),

-- 7. PERSONA (성향/동반)
('PERSONA', '반려동물 동반', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Dog%20face/3D/dog_face_3d.png'),
('PERSONA', '냥집사/멍집사', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Paw%20prints/3D/paw_prints_3d.png'),
('PERSONA', '워케이션', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Laptop/3D/laptop_3d.png'),
('PERSONA', '독서', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Books/3D/books_3d.png'),
('PERSONA', '뚜벅이', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Bus/3D/bus_3d.png'),
('PERSONA', '알쓰 환영', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Beverage%20box/3D/beverage_box_3d.png'),

-- 8. FACILITY (시설/편의)
('FACILITY', '1인실', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Bust%20in%20silhouette/3D/bust_in_silhouette_3d.png'),
('FACILITY', '여성 전용', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Women%E2%80%99s%20room/3D/women%E2%80%99s_room_3d.png'),
('FACILITY', '공항 주변', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Airplane/3D/airplane_3d.png'),
('FACILITY', '자쿠지', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Bathtub/3D/bathtub_3d.png'),
('FACILITY', '넷플릭스/OTT', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Popcorn/3D/popcorn_3d.png'),
('FACILITY', '스탠바이미', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Television/3D/television_3d.png'),
('FACILITY', '거울셀카존', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Mirror/3D/mirror_3d.png'),

-- 9. FOOD (음식)
('FOOD', '조식', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Cooking/3D/cooking_3d.png'),
('FOOD', '오마카세', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Sushi/3D/sushi_3d.png'),
('FOOD', '포틀럭', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Pizza/3D/pizza_3d.png'),
('FOOD', '바비큐 파티', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Meat%20on%20bone/3D/meat_on_bone_3d.png'),
('FOOD', '심야식당', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Pot%20of%20food/3D/pot_of_food_3d.png'),
('FOOD', '배달맛집권', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Motor%20scooter/3D/motor_scooter_3d.png'),

-- 10. PLAY (놀거리)
('PLAY', '닌텐도/게임', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Video%20game/3D/video_game_3d.png'),
('PLAY', '보드게임', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Game%20die/3D/game_die_3d.png'),
('PLAY', '만화방', 'https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Closed%20book/3D/closed_book_3d.png');

-- 5. 테마 ID 범위 확인 (삽입된 테마 ID 확인용)
-- SELECT MIN(theme_id) as min_id, MAX(theme_id) as max_id FROM theme;
