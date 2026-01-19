# 🏠 지금이곳 - AI 기반 게스트하우스 플랫폼

> **호스트에게는 데이터 기반의 AI 인사이트를, 게스트에게는 취향 저격 감성 숙소를 제공하는 올인원 게스트하우스 플랫폼**

<br/>

## 📖 프로젝트 소개
지금이곳은 단순한 숙소 예약 서비스를 넘어, **Google Gemini AI**를 활용하여 호스트에게 실질적인 운영 솔루션을 제공하는 플랫폼입니다.
게스트는 카카오맵 기반 및 AI 검색으로 원하는 테마의 게스트하우스를 손쉽게 찾고 예약할 수 있으며 자유롭게 호스트 변환이 가능합니다.
호스트는 AI가 분석한 리뷰 요약, 수요 예측, 테마 트렌드 리포트를 통해 매출 증대 전략을 수립할 수 있습니다.

<br/>

## 🛠️ Tech Stack

### **Frontend**
<img src="https://img.shields.io/badge/Vue.js 3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white"> <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white"> <img src="https://img.shields.io/badge/Pinia-FFE4C4?style=for-the-badge&logo=pinia&logoColor=black"> <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white"> <img src="https://img.shields.io/badge/Tailwind CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white">

### **Backend**
<img src="https://img.shields.io/badge/Java 17-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot 3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/MyBatis-000000?style=for-the-badge&logo=mybatis&logoColor=white"> <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">

### **Database & Infrastructure**
<img src="https://img.shields.io/badge/MySQL 8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"> <img src="https://img.shields.io/badge/NCP (Naver Cloud)-03C75A?style=for-the-badge&logo=naver&logoColor=white">

### **External APIs**
<img src="https://img.shields.io/badge/Google Gemini AI-8E75B2?style=for-the-badge&logo=googlebard&logoColor=white"> <img src="https://img.shields.io/badge/Toss Payments-0050FF?style=for-the-badge&logo=toss&logoColor=white"> <img src="https://img.shields.io/badge/Kakao Map-FFCD00?style=for-the-badge&logo=kakao&logoColor=black"> <img src="https://img.shields.io/badge/NCP Object Storage-03C75A?style=for-the-badge&logo=naver&logoColor=white">

<br/>

## 🏗️ System Architecture

서비스의 안정성과 확장성을 고려하여 **3-Tier 분산 서버 구조**로 설계되었습니다.

```
graph TD
    User[Client (Web/Mobile)] -->|HTTPS| Nginx[Nginx (Reverse Proxy)]
    
    subgraph "Server Cluster"
        Nginx -->|Port 8080| MainServer[Server 2: Main API Server]
        Nginx -->|Port 8081| AdminServer[Server 3: Admin API Server]
        
        MainServer -->|Cache/Session| Redis[Redis (Server 2)]
        MainServer -->|Data| DB[MySQL (Server 1 / Docker)]
        AdminServer -->|Data| DB
    end
    
    subgraph "External Services"
        MainServer -->|Payment| Toss[Toss Payments API]
        MainServer -->|AI Analysis| Gemini[Google Gemini API]
        MainServer -->|Image Upload| S3[NCP Object Storage]
        User -->|Map| Kakao[Kakao Map API]
    end
```

*   **Server 1 (Database):** Docker 컨테이너 기반의 MySQL (Port 3306) 운영.
*   **Server 2 (Main Application):** 사용자 및 호스트용 메인 비즈니스 로직 처리 (Port 8080) 및 Redis (Port 6379)를 통한 캐싱/세션 관리.
*   **Server 3 (Admin Application):** 관리자 전용 백오피스 서버 분리 (Port 8081).
*   **Storage:** NCP Object Storage(S3 호환)를 이용한 이미지 파일 관리.

<br/>

## ✨ Key Features

### 1. 🤖 호스트 전용 AI 인사이트 (Gemini Pro 연동)
*   **리뷰 분석 리포트:** 수집된 리뷰를 AI가 분석하여 '총평', '강점', '개선 포인트', '실행 액션'을 도출.
*   **테마 인기 트렌드:** 지역별/시즌별 인기 테마를 분석하고 매출/예약 증대를 위한 전략 제안.
*   **수요 예측 (Demand Forecast):** 과거 예약 데이터를 기반으로 향후 30일간의 수요를 예측하고 가격 전략 제시.
*   **Cold Start 대응:** 데이터가 부족한 신규 숙소에게도 지역 트렌드 기반의 맞춤형 가이드 제공.

### 2. 🏨 게스트하우스 예약 및 결제
*   **카카오맵 기반 검색:** 지도 UI를 통해 위치 기반으로 직관적인 숙소 탐색.
*   **실시간 예약/결제:** Toss Payments 연동을 통한 안전하고 빠른 결제 시스템.
*   **테마별 검색:** 파티, 촌캉스, 오션뷰 등 MZ세대 맞춤형 테마 필터링.

### 3. 💬 소통 및 편의 기능
*   **실시간 챗봇:** 예약 문의 및 자주 묻는 질문 자동 응답 처리.
*   **리뷰 및 별점:** 투명한 리뷰 시스템과 태그 기반의 키워드 평가.

### 4. 👑 관리자(Admin) 기능
*   호스트 입점 승인 및 반려 프로세스.
*   전체 회원 및 예약 현황 대시보드 관리.
*   쿠폰 및 프로모션 발행 관리.

<br/>

## 📂 Project Structure

```text
GuestHouse
├── backend                 # Spring Boot Backend
│   ├── src/main/java       # Source code
│   │   └── domain          # Domain-driven design (accommodation, booking, report, ai...)
│   └── src/main/resources  # Config & Mapper xml
├── frontend                # Vue.js Frontend
│   ├── src
│   │   ├── api             # Axios API modules
│   │   ├── components      # Reusable Vue components
│   │   ├── views           # Page views (Host, Guest, Admin)
│   │   └── stores          # Pinia state management
└── README.md
```

<br/>

## 🚀 Getting Started

### Backend
```bash
cd backend
./gradlew clean build
java -jar build/libs/geharbang-0.0.1-SNAPSHOT.jar
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

<br/>

## 👨‍💻 Team Members

| Role |   Name    | Position |            GitHub            |
|:---:|:---------:|:---:|:----------------------------:|
| **Leader** | **[엄현석]** | **PM, Infra, Backend(Report/AI)** | [@User](https://github.com/) |
| Member |   [김형근]   | Backend (Booking/Payment) | [@User](https://github.com/) |
| Member |   [이경민]   | Frontend (Guest UI/Map) | [@User](https://github.com/) |
| Member |   [이재훈]   | Frontend (Host/Admin UI) | [@User](https://github.com/) |
| Member |   [장현우]   | Database & API | [@User](https://github.com/) |

<br/>

---
*This project was developed as a final team project.*
