# AI 여행 가이드 기능 기술 문서

## 📌 개요
사용자가 자연어로 숙소를 검색하고 AI가 맞춤 추천해주는 대화형 가이드 기능

---

## 🤖 사용된 AI
| 항목 | 내용 |
|------|------|
| **AI 서비스** | Google Gemini API |
| **모델** | `gemini-flash-latest` (2026년 기준 최신) |
| **API 버전** | v1beta |
| **기능** | 멀티턴 대화, 자연어 이해, 숙소 추천 |

---

## 🛠 기술 스택

### Backend (Spring Boot)
- **Spring Boot** 3.x
- **Spring Data JPA** - 채팅 기록 저장
- **RestTemplate** - Gemini API 호출
- **MySQL** - 채팅방, 메시지 영구 저장

### Frontend (Vue.js)
- **Vue 3** (Composition API)
- **Vue Router** - 페이지 라우팅
- **marked** - 마크다운 렌더링

### API 연동
- **Gemini API** - AI 응답 생성
- **RESTful API** - 프론트-백엔드 통신

---

## 📁 주요 파일

### Backend
```
backend/src/main/java/.../ai_agent/
├── controller/AiAgentController.java  # API 엔드포인트
├── service/AiAgentService.java        # AI 로직 + Gemini 호출
├── entity/AgentChatRoom.java          # 채팅방 엔티티
├── entity/AgentChatMessage.java       # 메시지 엔티티
└── dto/AiAgentDto.java                # 요청/응답 DTO
```

### Frontend
```
frontend/src/
├── views/ai/AiAgentView.vue           # 채팅 UI
├── api/aiAgentApi.js                  # API 클라이언트
└── views/home/HomeView.vue            # 메인 배너
```

---

## 🔧 주요 기능

1. **멀티턴 대화** - 이전 대화 맥락 유지
2. **숙소 추천** - 조건에 맞는 숙소 카드 표시
3. **마크다운 렌더링** - 굵은 글씨, 제목, 리스트 지원
4. **대화 기록 저장** - MySQL에 영구 저장
5. **반응형 UI** - 모바일/PC 지원

---

## ⚙️ 설정

### application.properties
```properties
GEMINI_API_KEY=your-api-key
GEMINI_MODEL=gemini-flash-latest
```

---

## 📅 작성일
2026-01-14
