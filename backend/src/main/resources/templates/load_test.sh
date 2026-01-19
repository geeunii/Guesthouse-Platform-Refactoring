#!/bin/bash
# 간단한 부하 테스트 스크립트 (인증 없이 공개 API 테스트)
# 사용법: ./load_test.sh

API_URL="http://10.0.2.6:8080/api/public/accommodations"
CONCURRENT=10
DURATION=10

echo "=========================================="
echo "부하 테스트 시작"
echo "URL: $API_URL"
echo "동시 연결: $CONCURRENT"
echo "Duration: ${DURATION}초"
echo "=========================================="

# wrk가 있으면 사용
if command -v wrk &> /dev/null; then
    echo "wrk로 테스트 중..."
    wrk -t2 -c$CONCURRENT -d${DURATION}s $API_URL
elif command -v ab &> /dev/null; then
    echo "Apache Bench로 테스트 중..."
    ab -n 100 -c $CONCURRENT $API_URL
else
    echo "간단한 curl 테스트 중..."
    START_TIME=$(date +%s.%N)
    
    for i in $(seq 1 50); do
        for j in $(seq 1 $CONCURRENT); do
            curl -s $API_URL > /dev/null &
        done
        wait
        echo "Batch $i 완료"
    done
    
    END_TIME=$(date +%s.%N)
    ELAPSED=$(echo "$END_TIME - $START_TIME" | bc)
    echo "총 요청: $((50 * CONCURRENT)), 소요 시간: ${ELAPSED}초"
fi

echo "=========================================="
echo "Scouter에서 TPS, XLog, Heap 그래프 확인하세요!"
echo "=========================================="
