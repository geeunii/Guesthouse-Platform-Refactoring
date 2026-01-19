# [Perf] 검색 속도 개선 트러블슈팅 리포트

## 1. 요약
- 문제: 검색 이후 List 화면 로딩 지연
- 대상: List 페이지(`frontend/src/views/home/List.vue`)
- 조치: API 분리/페이징, DB 필터, N+1 제거, 인덱스 추가
- 결과: 초기 로딩 페이로드 감소 및 체감 속도 개선(정량 측정은 미수행)

## 2. Before / After
| 항목 | Before | After |
| --- | --- | --- |
| List API | `/api/public/list` | `/api/public/search` |
| 필터 위치 | 프론트 | DB |
| 이미지 조회 | N+1 | 배치/조인 |
| 로딩 방식 | 전체 일괄 | 페이지 기반(더 보기) |

## 3. 적용한 조치(4)
1) API 분리 + 페이징
- `/api/public/search` 신설, List는 페이지/사이즈 기반으로 로딩
- 관련: `backend/.../MainController.java`, `backend/.../PublicListResponse.java`, `frontend/src/api/list.js`, `frontend/src/views/home/List.vue`

2) 키워드 필터 DB 이관
- 숙소명/지역 필터를 DB에서 처리해 대상 축소
- 관련: `backend/.../MainRepository.java`, `backend/.../BaseMainService.java`

3) 대표 이미지 N+1 제거
- 이미지 조회를 배치 또는 조인 기반으로 변경
- 관련: `backend/.../BaseMainService.java`, `backend/.../MainRepository.java`

4) 검색 인덱스 추가
- 상태/승인, 숙소명, 지역 컬럼 인덱스 추가
- 관련: `backend/src/main/resources/db/migration/V2__add_accommodation_search_indexes.sql`

## 4. 검증
- 테스트(백엔드)
```bash
cd backend
.\gradlew.bat test --tests com.ssg9th2team.geharbang.domain.main.service.MainServiceTest --no-daemon --console=plain
```
- 수동 체크
  - 검색 후 List 진입 시 첫 페이지가 빠르게 렌더링되는지
  - “더 보기” 클릭 시 결과가 append되는지
  - 검색어 변경 시 페이지가 초기화되는지

## 5. 리스크/후속
- `LIKE '%keyword%'`는 인덱스 효율이 제한됨(데이터 증가 시 재검토 필요)
- Map 화면은 아직 `/api/public/search` 적용 전(후속 개선 후보)
- 추가 개선: FULLTEXT/검색엔진 도입, 캐시
