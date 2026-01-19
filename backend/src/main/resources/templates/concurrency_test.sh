#!/bin/bash
# 동시성 테스트 스크립트 - 예약 시스템 비관적 락 테스트
# 사용법: ./concurrency_test.sh [JWT_TOKEN]

# 설정
API_URL="http://10.0.2.6:8080/api/reservations"
CONCURRENT_REQUESTS=10  # 동시 요청 수

# JWT 토큰 (인자로 받거나 기본값)
JWT_TOKEN="Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJsZWVrbWluMTgwNEBnbWFpbC5jb20iLCJhdXRoIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzY3OTE3NTcwLCJleHAiOjE3Njc5MjExNzB9.eQ-6T2FC77XIHlhMo9FnNKLI44FzQKp-6xdFz7YROEo4YC81DenoncKxGN9TXLZb-tILmsB3BTZ3Wis00sAsEQ"

# 테스트 데이터 (동일한 객실에 동시 예약)
# 실제 데이터에 맞게 수정 필요
ROOM_ID=230
ACCOMMODATION_ID=35
CHECKIN="2026-01-20T15:00:00Z"
CHECKOUT="2026-01-21T11:00:00Z"

echo "=========================================="
echo "동시성 테스트 시작"
echo "동시 요청 수: $CONCURRENT_REQUESTS"
echo "대상 객실 ID: $ROOM_ID"
echo "=========================================="

# 결과 저장 디렉토리
RESULT_DIR="/tmp/concurrency_test_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULT_DIR"

# 동시 예약 요청 함수
make_reservation() {
    local request_id=$1
    local guest_count=1  # 1명 고정 (테스트 명확성을 위해)
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -H "Authorization: $JWT_TOKEN" \
        -d "{
            \"accommodationsId\": $ACCOMMODATION_ID,
            \"roomId\": $ROOM_ID,
            \"checkin\": \"$CHECKIN\",
            \"checkout\": \"$CHECKOUT\",
            \"guestCount\": $guest_count,
            \"totalAmount\": 100000,
            \"reserverName\": \"테스트$request_id\",
            \"reserverPhone\": \"010-1234-000$request_id\"
        }")
    
    # HTTP 상태 코드 추출
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    echo "[요청 $request_id] HTTP $http_code - $body" >> "$RESULT_DIR/results.log"
    echo "[요청 $request_id] HTTP $http_code"
}

# 시작 시간
START_TIME=$(date +%s.%N)

# 동시 요청 실행
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    make_reservation $i &
done

# 모든 요청 완료 대기
wait

# 종료 시간
END_TIME=$(date +%s.%N)
ELAPSED=$(echo "$END_TIME - $START_TIME" | bc)

echo "=========================================="
echo "테스트 완료!"
echo "총 소요 시간: ${ELAPSED}초"
echo "결과 파일: $RESULT_DIR/results.log"
echo "=========================================="

# 결과 요약
echo ""
echo "=== 결과 요약 ==="
echo "성공(201): $(grep -c "HTTP 201" "$RESULT_DIR/results.log" 2>/dev/null || echo 0)"
echo "실패(400/409/500): $(grep -cE "HTTP (400|409|500)" "$RESULT_DIR/results.log" 2>/dev/null || echo 0)"
echo ""
echo "상세 결과:"
cat "$RESULT_DIR/results.log"
