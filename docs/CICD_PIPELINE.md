# CI/CD 자동 배포 파이프라인 구축 가이드

## 📋 개요

이 문서는 GuestHouse 백엔드 프로젝트의 CI/CD 자동 배포 파이프라인 구축 과정을 정리한 것입니다.

### 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   GitHub    │ --> │   GitHub    │ --> │   Public    │ --> │  Private    │
│  Repository │     │   Actions   │     │   Server    │     │   Server    │
│  (develop)  │     │  (Build)    │     │ (Jump Host) │     │  (Docker)   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │                   │
  Push/Merge          JAR 빌드           JAR 전송          Docker 재시작
```

### 서버 구성

| 서버 | IP | 역할 |
|------|-----|------|
| Public Server | 49.50.138.206 | Jump Host (외부 접근 가능) |
| Private Server | 10.0.2.6 | Backend Docker Container 실행 |

---

## 🔧 구성 파일

### 1. Dockerfile

**경로**: `backend/Dockerfile`

```dockerfile
# Eclipse Temurin 17 기반 이미지 사용
FROM eclipse-temurin:17-jdk-alpine

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사 (와일드카드 사용으로 버전 독립적)
COPY *.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. docker-compose.yml

**경로**: `backend/docker-compose.yml`

```yaml
services:
  backend:
    build: .
    container_name: guesthouse-backend
    restart: always
    network_mode: host
    env_file:
      - .env
```

> **Note**: `network_mode: host`는 컨테이너가 호스트 네트워크를 직접 사용하여 MySQL(127.0.0.1:3306)에 접근할 수 있게 합니다.

### 3. GitHub Actions Workflow

**경로**: `.github/workflows/gradle.yml`

```yaml
name: Java CI with Gradle

on:
  push:
    branches: [ "develop" ]
  pull_request:
    branches: [ "develop" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v4
    
    - name: Build with Gradle Wrapper
      run: ./gradlew bootJar
      working-directory: ./backend
    
    - name: Upload JAR artifact
      uses: actions/upload-artifact@v4
      with:
        name: app-jar
        path: backend/build/libs/geharbang-0.0.1-SNAPSHOT.jar

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/develop'

    steps:
    - name: Download JAR artifact
      uses: actions/download-artifact@v4
      with:
        name: app-jar
        path: ./

    - name: Setup SSH Key
      run: |
        mkdir -p ~/.ssh
        echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
        chmod 600 ~/.ssh/id_rsa
        ssh-keyscan -H ${{ secrets.PUBLIC_SERVER_IP }} >> ~/.ssh/known_hosts

    - name: Deploy to Private Server via Public Server
      run: |
        # Public 서버로 JAR 전송
        scp -o StrictHostKeyChecking=no ./geharbang-0.0.1-SNAPSHOT.jar ${{ secrets.SSH_USERNAME }}@${{ secrets.PUBLIC_SERVER_IP }}:/tmp/
        
        # Public 서버에서 Private 서버로 JAR 전송 및 Docker 재시작
        ssh -o StrictHostKeyChecking=no ${{ secrets.SSH_USERNAME }}@${{ secrets.PUBLIC_SERVER_IP }} << 'EOF'
          scp /tmp/geharbang-0.0.1-SNAPSHOT.jar root@${{ secrets.PRIVATE_SERVER_IP }}:~/applications/thismo/
          ssh root@${{ secrets.PRIVATE_SERVER_IP }} "cd ~/applications/thismo && docker-compose up -d --build"
          rm /tmp/geharbang-0.0.1-SNAPSHOT.jar
        EOF

    - name: Deployment Complete
      run: echo "✅ Deployment to Private Server completed successfully!"
```

---

## 🔐 GitHub Secrets 설정

GitHub Repository → **Settings** → **Secrets and variables** → **Actions**에서 다음 Secrets를 설정합니다:

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `SSH_PRIVATE_KEY` | GitHub Actions용 SSH 개인키 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `SSH_USERNAME` | Public 서버 SSH 사용자명 | `root` |
| `PUBLIC_SERVER_IP` | Public 서버 IP | `49.50.138.206` |
| `PRIVATE_SERVER_IP` | Private 서버 IP | `10.0.2.6` |

---

## 🔑 SSH 키 설정

### 1. GitHub Actions → Public Server

로컬에서 SSH 키 생성:
```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/github_actions_key
```

공개키를 Public 서버의 `~/.ssh/authorized_keys`에 추가:
```bash
cat ~/.ssh/github_actions_key.pub >> ~/.ssh/authorized_keys
```

개인키를 GitHub Secrets `SSH_PRIVATE_KEY`에 저장.

### 2. Public Server → Private Server

Public 서버에서 SSH 키 생성:
```bash
ssh-keygen -t rsa -b 4096
```

공개키를 Private 서버의 `~/.ssh/authorized_keys`에 추가:
```bash
ssh-copy-id root@10.0.2.6
```

---

## 📁 Private 서버 환경 설정

### 디렉토리 구조

```
~/applications/thismo/
├── docker-compose.yml
├── Dockerfile
├── .env
└── geharbang-0.0.1-SNAPSHOT.jar
```

### .env 파일 (Private 서버)

```env
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/guesthouse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=thismo
SPRING_DATASOURCE_PASSWORD=thismo1234

# OAuth2
OAUTH2_REDIRECT_BASE_URL=http://49.50.138.206
OAUTH2_FRONTEND_BASE_URL=http://49.50.138.206

# NCloud Object Storage
NCLOUD_BUCKET=guesthouse
NCLOUD_ACCESS_KEY=your_access_key
NCLOUD_SECRET_KEY=your_secret_key

# Clova Chatbot
CLOVA_CHATBOT_INVOKE_URL=your_invoke_url
CLOVA_CHATBOT_SECRET_KEY=your_secret_key

# Holiday API
HOLIDAY_SERVICE_KEY=your_service_key

# OAuth2 - Google
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your_client_id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=your_client_secret
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=profile,email

# OAuth2 - Kakao
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_ID=your_client_id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KAKAO_CLIENT_SECRET=your_client_secret

# OAuth2 - Naver
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_ID=your_client_id
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_NAVER_CLIENT_SECRET=your_client_secret
```

> ⚠️ **보안 주의**: `.env` 파일은 Git에 커밋하지 않습니다. `.gitignore`에 이미 포함되어 있습니다.

---

## 🚀 배포 흐름

### 자동 배포 (develop 브랜치)

1. **코드 Push/Merge** → develop 브랜치
2. **GitHub Actions Build Job**
   - 코드 체크아웃
   - JDK 17 설정
   - Gradle bootJar 빌드
   - JAR 파일 artifact 업로드
3. **GitHub Actions Deploy Job**
   - JAR artifact 다운로드
   - SSH로 Public 서버에 JAR 전송
   - Public 서버에서 Private 서버로 JAR 전송
   - Private 서버에서 `docker-compose up -d --build` 실행

### 수동 배포

Private 서버에서 직접 실행:
```bash
cd ~/applications/thismo
docker-compose down
docker-compose up -d --build
docker-compose logs -f
```

---

## 🔍 모니터링 및 디버깅

### 컨테이너 상태 확인
```bash
docker ps
docker ps -a  # 종료된 컨테이너 포함
```

### 로그 확인
```bash
docker-compose logs --tail=100
docker-compose logs -f  # 실시간 로그
```

### 컨테이너 재시작
```bash
docker-compose restart
```

### 컨테이너 완전 재빌드
```bash
docker-compose down
docker-compose up -d --build
```

---

## 📈 Scouter 모니터링 (APM)

### 1. 연결 정보
- **Scouter Client**에서 Localhost 연결
- **ID/PW**: `admin` / `admin`

### 2. 접속 방법 (SSH 터널링)
로컬 PC에서 다음 명령어로 터널링을 설정합니다:

```bash
# Public 서버(Jump Host)를 통해 Private 서버(10.0.2.6)의 6100 포트로 터널링
ssh -L 6100:10.0.2.6:6100 root@49.50.138.206
```

### 3. 구성 요소
- **Scouter Server**: Private 서버(10.0.2.6)에서 실행 중
- **Scouter Agent**: Spring Boot Docker 컨테이너 내부에 포함됨 (Dockerfile 참조)


---

## ✅ 트러블슈팅

### 1. SSH 연결 실패
- GitHub Secrets의 `SSH_PRIVATE_KEY`가 올바른지 확인
- Public 서버의 `~/.ssh/authorized_keys`에 공개키가 있는지 확인
- 키 권한 확인: `chmod 600 ~/.ssh/id_rsa`

### 2. MySQL 연결 실패
- `.env`의 `SPRING_DATASOURCE_URL`이 올바른지 확인
- MySQL 컨테이너가 실행 중인지 확인: `docker ps | grep mysql`
- `network_mode: host` 사용 시 `127.0.0.1:3306`으로 연결

### 3. OAuth2 설정 오류
- 환경변수 이름이 정확한지 확인
- Spring Boot 형식: `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_<PROVIDER>_CLIENT_ID`

### 4. 환경변수 누락
- `.env` 파일에 필요한 모든 환경변수가 있는지 확인
- `docker-compose config`로 환경변수 확인

---

## 📊 배포 트리거 조건

| 이벤트 | 빌드 | 배포 |
|--------|------|------|
| feature 브랜치 push | ❌ | ❌ |
| develop 브랜치로 PR | ✅ | ❌ |
| develop 브랜치에 Merge | ✅ | ✅ |
| develop 브랜치 직접 push | ✅ | ✅ |

---

## 📅 작성일

- **최초 작성**: 2026-01-08
- **작성자**: GuestHouse Backend Team
