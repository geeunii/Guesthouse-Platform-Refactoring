# PPT 버전 관리

## 📋 버전 히스토리

### v1.1 (현재 - 2026-01-16) ⭐
- **Main Slide (Slide 1) 디자인 개편**
    - 홈페이지 메인 배너 스타일 적용
    - `TMoneyDungunbaram`, `NanumSquareRound` 폰트 적용
    - 배너 이미지 콘텐츠 삽입 및 CSS 스타일링
    - 타이틀/태그라인 레이아웃 변경

### v1.0 (2026-01-16)
- 시스템 아키텍처 이미지 적용 (`sys_architechture.png`)
- IA 구조도 실제 프로젝트 기반으로 업데이트 (50+ 화면)
- 기능 슬라이드(10-13) 레이아웃 통일 (이미지 왼쪽, 텍스트 오른쪽)
- 스마트폰 비율 프레임 적용 (320px × 694px, 비율 1:2.17)
- point-item 크기 및 텍스트 확대
- 기술 스택에 Kakao Map, Toss Payments 추가
- 슬라이드 1 로고 확대, "지금 이곳" 중복 텍스트 제거
- 슬라이드 9 why-card 기본 hover 효과 적용
- 기술 스택 카드 디자인 개선

---

## 📁 폴더 구조

```
PPT/
├── index.html          # 초기 버전 (레거시)
├── styles.css
├── VERSION.md          # 버전 관리 문서
├── IA_structure.png    # 초기 기획 IA 구조도
├── sys_architechture.png # 시스템 아키텍처 이미지
├── v1.1/               # 현재 최신 버전 ⭐
│   ├── index.html
│   └── styles.css
├── v1.0/               
│   ├── index.html
│   └── styles.css
└── archive/            # 이전 버전 보관
    ├── v0.2/
    ├── v0.3/
    ├── v0.4/
    ├── v0.5/
    └── v0.6/
```

## 🔄 버전 관리 규칙

- **Major (x.0)**: 대규모 구조 변경, 새로운 섹션 추가
- **Minor (x.x)**: 기능 개선, 스타일 수정, 콘텐츠 업데이트
- **현재 작업 버전**: `v1.1`
- **다음 업데이트 시**: `v1.2`로 새 폴더 생성
