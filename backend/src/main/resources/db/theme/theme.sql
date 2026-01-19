CREATE TABLE IF NOT EXISTS theme
(
    theme_id       BIGINT       NOT NULL AUTO_INCREMENT,
    theme_category VARCHAR(50)  NOT NULL,
    theme_name     VARCHAR(50)  NOT NULL,
    theme_image_url VARCHAR(500),
    PRIMARY KEY (theme_id),
    UNIQUE KEY UQ_THEME_NAME (theme_name)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO theme (theme_category, theme_name, theme_image_url) VALUES
                                                   ('ACTIVITY_COMMUNITY', '불멍', 'https://images.unsplash.com/photo-1543888362-e64e9a03b5f9?w=500'),
                                                   ('ACTIVITY_COMMUNITY', '포틀럭', 'https://images.unsplash.com/photo-1549422005-7f938ae1c463?w=500'),
                                                   ('ACTIVITY_COMMUNITY', '러닝', 'https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=500'),
                                                   ('ACTIVITY_COMMUNITY', '서핑', 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=500'),
                                                   ('ACTIVITY_COMMUNITY', '하이킹', 'https://images.unsplash.com/photo-1551632811-561732d1e306?w=500'),
                                                   ('ACTIVITY_COMMUNITY', '신나는 게스트하우스', 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=500'),
                                                   ('ACTIVITY_COMMUNITY', '조용한 게스트하우스', 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=500'),

                                                   ('LOCATION', '문화유적', 'https://images.unsplash.com/photo-1577717903215-ce1b0e51e13b?w=500'),
                                                   ('LOCATION', '공항 주변', 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=500'),
                                                   ('LOCATION', '노을 맛집(노을 명소)', 'https://images.unsplash.com/photo-1495567720989-cebdbdd97913?w=500'),

                                                   ('LIFESTYLE', '여성 전용', 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=500'),
                                                   ('LIFESTYLE', '1인실', 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=500'),
                                                   ('LIFESTYLE', '독서', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=500'),
                                                   ('LIFESTYLE', '스냅 촬영', 'https://images.unsplash.com/photo-1554048612-b6a482bc67e5?w=500'),
                                                   ('LIFESTYLE', 'MBTI E', 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=500'),
                                                   ('LIFESTYLE', 'MBTI I', 'https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?w=500'),

                                                   ('FOOD', '조식', 'https://images.unsplash.com/photo-1482049016555-c54df661271e?w=500'),
                                                   ('FOOD', '오마카세', 'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=500');