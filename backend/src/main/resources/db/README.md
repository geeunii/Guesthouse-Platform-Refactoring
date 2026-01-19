# DB 마이그레이션 가이드

이 디렉터리는 DB 스키마를 버전 관리하기 위한 자리입니다. 
Flyway 기준 예시이며, 처음 쓰는 사람도 아래 순서만 따르면 됩니다. Liquibase를 써도 동일한 위치를 사용할 수 있습니다.

## 한눈에 요약 (처음 쓰는 사람용)
1) `build.gradle` 의존성 추가(이미 반영됨)
   - `implementation "org.flywaydb:flyway-core"`
   - `implementation "org.flywaydb:flyway-mysql"` (MySQL용)
2) DB 접속 정보가 `application.properties`에 있어야 함 (MySQL).
3) 마이그레이션 파일 생성: `db/migration/V001__init_schema.sql`처럼 번호+설명으로 SQL 작성.
4) 적용 테스트:
   - 로컬 DB: `./gradlew flywayMigrate`
   - 테스트 프로파일 예시: `./gradlew flywayMigrate -Dspring.profiles.active=test`
5) PR에 마이그레이션 파일 포함 → 리뷰 → main/dev 머지. CI는 루트 `.github/workflows/ci.yml`에서 백엔드 빌드까지 실행함.

## 폴더 구조
- `db/migration/` : 마이그레이션 SQL 파일(.sql) 보관
- `db/reference/` : 팀원/외부 DDL 참고용(선택)
- `db/docs/` : 지표 정의, 배치 쿼리 설명 등 문서(선택)

## 테스트 DB 설정 (공통 안내)
- 프로파일: `test` (실행 시 `SPRING_PROFILES_ACTIVE=test` 지정)
- 접속 정보: 환경변수로 덮어쓰거나 `application-test.properties` 수정
  - `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/guesthouse?...`
  - `SPRING_DATASOURCE_USERNAME=사용자명`
  - `SPRING_DATASOURCE_PASSWORD=비밀번호`
- 마이그레이션 적용: DB에 사전 적용 후 `./gradlew test` 실행 (현재 `build.gradle`에는 Flyway 플러그인이 포함되어 있지 않음)

## 네이밍 규칙(Flyway 예시)
- 파일명: `V###__description.sql` (예: `V001__init_schema.sql`)
- ###는 3자리 숫자. 같은 버전 번호 충돌 시 더 큰 숫자로 새 파일 추가(기존 파일 수정 지양).
- description은 소문자/언더스코어로 간단히 요약.

## 작성/반영 흐름
1) 새 스키마 변경이 필요하면 `db/migration`에 새 버전 파일 추가.
2) DDL/인덱스/뷰/비정규화 테이블 생성/변경을 SQL로 작성.
3) 로컬에서 적용 및 테스트:
   - Flyway 사용 시: `./gradlew flywayMigrate` (필요하면 `-Dspring.profiles.active=test` 등으로 테스트 DB 지정)
   - Liquibase 사용 시: `./gradlew update`
4) PR에 마이그레이션 파일 포함 → 리뷰 → main/dev에 머지.
5) CI/CD에서 애플리케이션 기동 전 동일 마이그레이션을 실행.

## 준비 사항 (선택)
- 테스트 DB 프로파일(`application-test.properties`)을 만들어 CI에서 H2나 테스트용 MySQL 컨테이너로 돌리면 안전합니다.

## 비정규화/지표 문서화 팁
- 마이그레이션 파일 옆에 간단한 설명을 남기거나, `db/docs/README.md`에 지표 정의, 배치 주기, 생성 쿼리를 기록하면 이후 QA/운영에서 도움이 됩니다.
