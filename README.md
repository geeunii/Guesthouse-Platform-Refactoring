# 🏠 지금이곳 - AI 호스트 기반 게스트하우스 플랫폼 (Refactoring)

<div align="center">

![Project Status](https://img.shields.io/badge/status-completed-success?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot)
![NCP](https://img.shields.io/badge/Naver_Cloud-Platform-03C75A?style=flat-square&logo=naver)
![Vue.js](https://img.shields.io/badge/Vue.js-3.x-4FC08D?style=flat-square&logo=vuedotjs)

**프로젝트명:** 지금 이곳 | **프로젝트 유형:** K-Digital Training 최종 프로젝트

**프로젝트 기간:** 2025.12.10 ~ 2026.01.15

**담당 파트:** 호스트 기능 및 페이지 & 관리자 기능 및 페이지 & AI 분석 로직

</div>

---

## 📑 목차
- [프로젝트 소개](#-프로젝트-소개)
- [기술 스택](#-기술-스택)
- [나의 담당 기능](#-나의-담당-기능)
- [시스템 아키텍처](#-시스템-아키텍처--인프라)
- [기술적 도전과 해결](#-기술적-도전과-해결)
- [트러블 슈팅 로그](#-트러블-슈팅-troubleshooting-log)
- [서비스 시연](#-서비스-시연)
- [프로젝트 구조](#-프로젝트-구조)

---

## 🎯 프로젝트 소개

**"호스트가 등록한 숙소도 AI 컨설팅을 받을 수 있을까?"** 라는 고민에서 출발했습니다.  
생성형 AI(Google Gemini)를 활용해 숙소 리뷰, 매출, 수요를 분석하고 운영 솔루션을 제안하는 **B2B 호스트 관리 플랫폼**입니다.  
기존의 모놀리식 구조를 개선하여, **NCP VPC 환경에서 User/Admin 서버를 물리적으로 분리**하고 보안을 강화한 버전입니다.

### 💡 기획 배경
- 신규 호스트는 리뷰 데이터가 부족하여 객관적인 숙소 상태 파악이 어려움
- 트래픽이 몰릴 경우, 관리자 기능(정산 등)까지 영향을 받는 구조적 문제 해결 필요
- 단순 통계를 넘어선, AI 기반의 인사이트 제공 니즈 증가

---

## 🛠️ 기술 스택

### Backend
- **Framework:** Spring Boot 3.4
- **Language:** Java 17
- **Database:** MySQL 8.0, Redis (Session/Cache)
- **ORM:** JPA (Hibernate), QueryDSL

### Infra & DevOps
- **Cloud:** Naver Cloud Platform (VPC)
- **Server:** Ubuntu 20.04, Nginx (Reverse Proxy)
- **CI/CD:** GitHub Actions (Self-hosted Runner)
- **Container:** Docker, Docker Compose

### AI & External API
- **AI:** Google Gemini Pro API
- **API:** Toss Payments, Kakao Map, CoolSMS

---

## 💼 나의 담당 기능

### 1. 관리자 서버 인프라 구축
- **관리자 서버 격리:** VPC 환경의 **Private Subnet에 관리자 서버를 배치**하여 외부 직접 접근 차단 및 보안 강화
- **서버 물리적 분리:** `User API Server`와 `Admin Server`를 분리 운영하여 사용자 트래픽 간섭 제거
- **Air-gap 배포:** 외부 망이 차단된 Private 서버에 배포하기 위해 Public Server를 경유하는 파이프라인 구축

### 2. 🤖 AI 리포트 로직 구현 (Backend)
- **Gemini API 연동:** `RestTemplate`을 활용한 프롬프트 엔지니어링 및 응답 처리
- **Cold Start 해결:** 리뷰 5개 미만 숙소를 위한 '트렌드 기반 컨설팅' 모드 개발
- **안정성 확보:** AI 응답 파싱 실패를 방어하는 `parseSafe` 로직 구현

---

## 🏗️ 시스템 아키텍처 & 인프라
> **보안 강화를 위한 VPC 네트워크 분리 (Public / Private Zone)**

![System Architecture](./images/system.jpeg)

- **Reverse Proxy (Nginx):** Public Zone에 배치하여 외부 트래픽을 수신하고, 내부 Private Zone의 서버로 라우팅
- **Security:** DB와 Redis, Admin Server는 외부 접근이 차단된 Private Subnet에 배치

---

## 🔥 기술적 도전과 해결

### 1️⃣ AI 응답 파싱 오류 해결 및 Cold Start 전략

#### ❌ 문제 상황
- 외부 AI API(Gemini)가 간헐적으로 JSON 형식이 아닌 일반 텍스트를 반환하여 `JsonMappingException` 발생.
- 리뷰가 0개인 신규 숙소는 분석할 데이터가 없어 서비스 이용 불가.

#### ✅ 해결 방법
1. **방어적 파싱 로직(`parseSafe`) 구현:** 응답 객체를 `Object`로 유연하게 받은 뒤, `instanceof`로 타입을 검사하고 강제로 리스트 형태로 정규화하여 **파싱 에러율 0%** 달성.
2. **이원화된 프롬프트 전략:**
   - **Data Sufficient:** `Review Analysis Mode` (리뷰 기반 장단점 분석)
   - **Data Insufficient:** `Trend Consulting Mode` (지역/시즌 트렌드 기반 조언 제공)

#### 📊 결과
- 리뷰가 0개인 숙소도 **100% 정보 제공**이 가능해져 초기 사용자 경험(UX) 개선.

---

### 2️⃣ 보안 강화를 위한 관리자 서버 분리 (Air-gap)

#### ❌ 문제 상황
- 단일 서버 운영 시, 사용자 트래픽 폭주가 관리자 기능(정산/운영) 마비로 이어질 위험 존재.
- 관리자 페이지가 Public IP로 노출되어 보안 취약점 발생.

#### ✅ 해결 방법
1. **서버 아키텍처 분리:** `User Server`(Public)와 `Admin Server`(Private)로 물리적 분리.
2. **폐쇄망 배포 파이프라인 구축:** 외부 인터넷이 차단된 Private Subnet에 배포하기 위해, Public Server를 경유(SCP)하여 Docker 이미지를 전송하는 Air-gap 방식 적용.
3. **ACG(Firewall) 최적화:** 내부 VPC 대역(`10.0.X.X`)과 Nginx IP에서만 접근 가능하도록 Inbound 규칙 엄격 제어.

---

## 🚒 트러블 슈팅 (Troubleshooting Log)
> **프로젝트 진행 중 발생한 주요 기술적 이슈와 해결 과정입니다. (클릭하여 상세보기)**

<details>
<summary>👉 <b>1. Docker "Bind for 0.0.0.0:8080 failed" (좀비 포트 & 포트 충돌)</b></summary>

**[문제 상황]**
- `docker run` 시 8080 포트가 이미 사용 중이라는 에러 발생.
- `netstat`으로 확인해도 점유 프로세스가 보이지 않는 'Ghost Port' 현상.
- 팀원이 배포한 `User Server`가 이미 8080을 점유하고 있어 `Admin Server` 배포 불가.

**[원인 분석]**
- 이전에 프로세스를 `kill -9`로 강제 종료하면서, Docker Proxy와 iptables 규칙이 정리되지 않아 OS와 Docker 데몬 간 불일치 발생.
- 단일 서버 내에서 두 개의 컨테이너가 동일한 Host Port(8080)를 요구함.

**[해결 과정]**
1. **소켓 및 런타임 완전 종료:** `systemctl stop docker.socket` 및 `containerd` 종료 후 `lsof`로 숨은 `docker-proxy` 프로세스 사살.
2. **포트 전략 수정:** Admin Server의 외부 포트를 `8081`로 변경하여 배포 (`-p 8081:8080`).

**[결과]**
- 포트 충돌 해결 및 컨테이너 정상 구동 완료.
</details>

<details>
<summary>👉 <b>2. Nginx 502 Bad Gateway (Reverse Proxy 라우팅 실패)</b></summary>

**[문제 상황]**
- 서버 분리 후, 관리자 페이지 접속 시 502 에러 발생하거나 사용자 서버(8080)로 요청이 잘못 전달됨.

**[원인 분석]**
- 물리적으로 서버(컨테이너)는 분리되었으나, Nginx 설정(`nginx.conf`)의 `proxy_pass`가 여전히 `localhost`를 참조하거나 경로 분기가 명확하지 않았음.
- Private Subnet 내의 IP가 아닌 Loopback 주소를 참조하여 연결 거부 발생.

**[해결 과정]**
- **Upstream 설정 구체화:** Nginx 설정에 `/api/admin/` 블록을 최상단에 추가하여 우선순위 확보.
- **명시적 IP 지정:** `proxy_pass http://{Admin_Private_IP}:8081;` 로 변경하여 정확한 목적지 지정.

**[결과]**
- 브라우저(Public) -> Nginx(Public) -> Admin Container(Private)로 이어지는 보안 라우팅 구조 완성.
</details>

<details>
<summary>👉 <b>3. AI 응답 불확실성 및 데이터 파싱 오류 (ClassCastException)</b></summary>

**[문제 상황]**
- Gemini API에게 `List<String>` 형태를 요청했으나, 간헐적으로 단순 `String`이나 포맷이 깨진 데이터를 반환하여 500 에러 발생.

**[해결 과정]**
- **방어적 파싱 로직(`parseSafe`) 구현:**
    - 응답을 `Object` 타입으로 유연하게 수신.
    - `instanceof`로 타입을 체크하여, String으로 온 경우 `split(",")`을 통해 강제로 List로 변환.
    - `null`일 경우 빈 리스트를 반환하여 NPE 방지.

**[결과]**
- AI 응답 형태와 무관하게 파싱 에러율 **0%** 달성 및 서비스 안정성 확보.
</details>

---

## 💻 서비스 시연
> **데이터 부족(Cold Start) 시에도 AI가 지역 트렌드를 분석하여 컨설팅을 제공합니다.**

![AI Demo](./images/demo.gif)

---

## 📂 프로젝트 구조
```bash
src
├── main
│   ├── java/com/geharbang
│   │   ├── domain
│   │   │   ├── host       # AI 리포트 생성 로직 (Prompt Engineering)
│   │   │   ├── settlement # 정산 및 매출 관리
│   │   │   └── ...
│   │   ├── global
│   │   │   ├── config     # NCP Object Storage & Gemini Config
│   │   │   └── security   # Spring Security 설정
...
