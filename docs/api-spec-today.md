# 오늘 작성한 API 명세

## 공통
- Base URL: /api
- Content-Type: pplication/json

## API 목록

| 이름 | Method | Path | Query/Path | 설명 | 응답(요약) |
| --- | --- | --- | --- | --- | --- |
| 테마 목록 조회 | GET | /themes | - | 테마 목록 조회 | [{ id, themeCategory, themeName }] |
| 로그인 사용자 선호 테마 조회 | GET | /themes/me | - | 로그인 사용자 선호 테마(최대 3건) 조회 | [{ id, themeCategory, themeName }] |
| 테마별 숙소 리스트 | GET | /public/list | 	hemeIds (optional, 반복 가능) | 테마 필터 기반 숙소 리스트 조회 | [{ accommodationsId, accommodationsName, shortDescription, city, district, township, minPrice, rating, reviewCount, imageUrl }] |
| 숙소 상세 조회 | GET | /public/detail/{accommodationsId} | ccommodationsId (path) | 숙소 상세 조회 | AccommodationDetailDto |

## 상세 규격

### 1) 테마 목록 조회
- Method: GET
- Path: /api/themes
- Query: 없음
- 응답 예시:
`json
[
  {
    "id": 1,
    "themeCategory": "AROUND_THEME",
    "themeName": "바닷가"
  }
]
`

### 2) 로그인 사용자 선호 테마 조회
- Method: GET
- Path: /api/themes/me
- Header:
  - Authorization: Bearer <accessToken>
- Query: 없음
- 응답 예시:
`json
[
  {
    "id": 2,
    "themeCategory": "AROUND_THEME",
    "themeName": "계곡"
  }
]
`

### 3) 테마별 숙소 리스트
- Method: GET
- Path: /api/public/list
- Query:
  - 	hemeIds (optional, 반복 가능)
- 응답 예시:
`json
[
  {
    "accommodationsId": 1,
    "accommodationsName": "제주 비지터 게스트하우스",
    "shortDescription": "좋은 시설과 깨끗한 방에서의 편안한 휴식",
    "city": "제주",
    "district": "제주시",
    "township": "연동",
    "minPrice": 120000,
    "rating": 4.14,
    "reviewCount": 395,
    "imageUrl": "https://..."
  }
]
`

### 4) 숙소 상세 조회
- Method: GET
- Path: /api/public/detail/{accommodationsId}
- Path variable:
  - ccommodationsId: 숙소 ID
- 응답: AccommodationDetailDto

추가: 숙소 상세 응답에 리뷰 목록이 포함됩니다.
```json
{
  "reviews": [
    {
      "reviewId": 1,
      "accommodationsId": 10,
      "userId": 3,
      "authorName": "홍길동",
      "rating": 4.5,
      "content": "리뷰 본문",
      "createdAt": "2025-12-24T11:22:33",
      "images": [
        { "reviewImageId": 5, "imageUrl": "https://...", "sortOrder": 1 }
      ],
      "tags": [
        { "reviewTagId": 2, "reviewTagName": "깨끗해요" }
      ]
    }
  ]
}
```

## 비고
- 	hemeIds는 ?themeIds=1&themeIds=2 형태로 다중 전달 가능.
- 필드명은 실제 응답 기준으로 작성됨 (ccomodationsId 오타 포함).
- /api/themes/me는 로그인 필요 (Authorization 헤더 사용).
