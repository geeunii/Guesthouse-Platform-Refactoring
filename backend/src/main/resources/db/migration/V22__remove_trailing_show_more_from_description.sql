-- V22: Remove trailing '펼쳐보기' text from accommodation description
-- 숙소 설명(accommodations_description) 끝에 붙은 '펼쳐보기' 문자열 제거

-- First, let's see how many rows will be affected (for logging purposes)
-- SELECT COUNT(*) FROM accommodation WHERE accommodations_description LIKE '%펼쳐보기';

-- Update: Remove trailing '펼쳐보기' from description
UPDATE accommodation 
SET accommodations_description = TRIM(TRAILING '펼쳐보기' FROM accommodations_description)
WHERE accommodations_description LIKE '%펼쳐보기';
