-- 전체 테이블 드롭 스크립트 (의존 관계 역순)
-- 운영/테스트 DB에 자동 적용되지 않도록 reference 경로에만 둡니다.
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS inquiries;
DROP TABLE IF EXISTS notices;
DROP TABLE IF EXISTS admin_log;
DROP TABLE IF EXISTS admin;
DROP TABLE IF EXISTS host_daily_stats;
DROP TABLE IF EXISTS platform_daily_stats;

DROP TABLE IF EXISTS review_tag_map;
DROP TABLE IF EXISTS review_image;
DROP TABLE IF EXISTS review_replies;
DROP TABLE IF EXISTS review_reports;

DROP TABLE IF EXISTS payment_refund;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS accommodation_amenity;
DROP TABLE IF EXISTS accommodation_image;
DROP TABLE IF EXISTS accommodation_policy;
DROP TABLE IF EXISTS accommodation_theme;
DROP TABLE IF EXISTS user_theme;

DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS review_tag;
DROP TABLE IF EXISTS user_coupon;
DROP TABLE IF EXISTS user_social;
DROP TABLE IF EXISTS accommodation;
DROP TABLE IF EXISTS amenity;
DROP TABLE IF EXISTS account_number;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS theme;
DROP TABLE IF EXISTS admins;

SET FOREIGN_KEY_CHECKS = 1;
