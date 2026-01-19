# 공공데이터 OpenAPI (공휴일) - 개발 정보

## 문서 정보
- 파일명: OpenAPI활용가이드_한국천문연구원_천문우주정보__특일_정보제공_서비스_v1.4

## 문서 목차(발췌)
- 1. 서비스 사용
  - 1.1. 서비스 신규 신청
    - 가. 서비스 활용 신청
- 2. 제공 서비스 목록
- 3. API 서비스 제공자 정보
- 4. 서비스 명세
  - 4.1. 특일 정보제공 서비스
    - 가. 서비스 개요
    - 나. 오퍼레이션 목록
    - 다. Datekind 분류

## 개정 이력
| 버전 | 변경일 | 변경 사유 | 변경 내용 | 작성자 | 승인 |
| --- | --- | --- | --- | --- | --- |
| 1.0 | 2015-11-06 | 최초작성 | OpenAPI 활용가이드 작성 및 승인 | 최성오 | 김성규 |
| 1.0 | 2016-06-15 | 내용수정 | 담당자 추가 및 일부 내용 수정 | 유솔 | 복은경 |
| 1.1 | 2017-05-04 | 내용수정 | 데이터 갱신주기 추가 및 실호출명 수정 | 유솔 | 복은경 |
| 1.2 | 2018-07-30 | 내용수정 | 오퍼레이션명 변경 및 휴일정보조회 예시 변경 | 유솔 | 복은경 |
| 1.3 | 2019-12-05 | 내용수정 | 입력 파라메터 변경(필수, 옵션), 오퍼레이션추가 (기념일정보조회-getAnniversaryInfo) | 김성규 | 복은경 |
| 1.4 | 2020-06-09 | 내용수정 | 입력 파라메터 변경(필수, 옵션), DateKind 추가 | 유솔 | 정회성 |

## 서비스 사용
### 서비스 신규 신청
#### 서비스 활용 신청
- 공공데이터포털(https://data.go.kr)에서 "특일 정보" 검색
- "활용신청" 버튼 클릭 후 신청
- 활용정보를 입력하고 신청 버튼을 클릭(활용 신청 승인은 자동 진행됨)
- 신청 완료 후 신청목록에서 신청 현황 조회 가능

## 제공 서비스 목록
| 일련번호 | 서비스 ID | 서비스명(국문) | 서비스명(영문) |
| --- | --- | --- | --- |
| 1 | SC-OA-09-04 | 특일 제공 서비스 | SpcdeInfoService |

## API 서비스 제공자 정보
| 이름 | 부서 | 연락처 | 이메일 |
| --- | --- | --- | --- |
| 유솔 | 천문전산융합센터 | 042-865-2188 | sol0993@kasi.re.kr |

## 제공기관
- 한국천문연구원 (공공데이터개방활용관리체계)

## 서비스 정보
- 데이터 포맷: XML
- 서비스 URL: http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService
- 요청 URL: http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo

## 개발 단계
- 활용승인 절차: 자동승인
- 신청 가능 트래픽: 10,000

## 운영 단계
- 활용승인 절차: 자동승인
- 신청 가능 트래픽: 활용사례 등록 후 증량 신청 가능

## 요청 파라미터
- ServiceKey (필수)
- solYear (필수, YYYY)
- solMonth (선택, MM)
- _type (선택, json 타입 요청 시)
- numOfRows (선택)
- pageNo (선택)

## 활용 가이드
1. 포털에서 발급된 서비스키를 설정합니다.
   - 서버 환경 변수 `HOLIDAY_SERVICE_KEY` 또는 `application-secret.properties` 사용.
2. 연도만 요청하거나, 연+월로 요청합니다.
3. 응답의 `resultCode`/`resultMsg`로 성공 여부를 확인합니다.
4. 키 인코딩 주의:
   - 키에 `%2B`, `%3D`가 포함되어 있으면 추가 인코딩하지 않습니다.
   - 키에 `+`, `=`가 있다면 URL 인코딩 1회가 필요합니다.

## 요청 예시
```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?ServiceKey=YOUR_KEY&solYear=2025&solMonth=01&numOfRows=50&pageNo=1
```

```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?ServiceKey=YOUR_KEY&solYear=2025&numOfRows=200&pageNo=1
```

## 서비스 명세
### 특일 정보제공 서비스
#### 서비스 개요
| 항목 | 내용 |
| --- | --- |
| 서비스 ID | SC-OA-09-04 |
| 서비스명(국문) | 특일 정보제공 서비스 |
| 서비스명(영문) | SpcdeInfoService |
| 서비스 설명 | 국경일정보, 공휴일정보, 24절기정보, 잡절정보를 조회하는 서비스 입니다. |
| 데이터 갱신주기 | 연 1회 |

#### 서비스 보안
- 서비스 인증/권한: Service Key
- 메시지 레벨 암호화: 없음
- 전송 레벨 암호화: SSL

#### 적용 기술 수준
- 인터페이스 표준: REST (GET, POST, PUT, DELETE)
- 교환 데이터 표준: XML, JSON

#### 서비스 URL
- 개발환경: http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService
- 운영환경: http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService

#### 서비스 WADL
- 개발환경: http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService
- 운영환경: http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService

#### 서비스 배포 정보
- 서비스 버전: 미기재
- 유효일자: 미기재
- 배포 일자: 2015. 12. 22.

#### 메시지 로깅 수준
- 성공: Header
- 실패: Header, Body

## 오퍼레이션 목록
| 일련번호 | 서비스명(국문) | 오퍼레이션명(영문) | 오퍼레이션명(국문) |
| --- | --- | --- | --- |
| 1 | 특일 정보제공 서비스 | getHoliDeInfo | 국경일 정보조회 |
| 2 | 특일 정보제공 서비스 | getRestDeInfo | 공휴일 정보조회 |
| 3 | 특일 정보제공 서비스 | getAnniversaryInfo | 기념일 정보조회 |
| 4 | 특일 정보제공 서비스 | get24DivisionsInfo | 24절기 정보조회 |
| 5 | 특일 정보제공 서비스 | getSundryDayInfo | 잡절 정보조회 |

## 요청 메시지 명세(공통)
| 항목명(영문) | 항목명(국문) | 항목구분 | 샘플데이터 | 항목설명 |
| --- | --- | --- | --- | --- |
| solYear | 연 | 필수 | 2019 | 연 |
| solMonth | 월 | 선택 | 03 | 월 |
| ServiceKey | 서비스키 | 필수 | 발급받은 서비스키 | 서비스키 |
| _type | json 타입 | 선택 | json | json 타입으로 활용 시 추가(디폴트 XML) |
| numOfRows | 한페이지 결과 수 | 선택 | 20 | 한페이지에 모든 결과를 확인할 때 추가(디폴트 10) |
| pageNo | 페이지 | 선택 | 1 | 페이지 |

## 응답 메시지 명세(공통)
| 항목명(영문) | 항목명(국문) | 항목구분 | 샘플데이터 | 항목설명 |
| --- | --- | --- | --- | --- |
| resultCode | 결과코드 | 필수 | 00 | 00: 성공 |
| resultMsg | 결과메시지 | 필수 | NORMAL SERVICE | 결과 메시지 |
| locdate | 날짜 | 필수 | 20190301 | 날짜(YYYYMMDD) |
| seq | 순번 | 필수 | 1 | 순번 |
| dateKind | 종류 | 필수 | 01 | 종류 |
| isHoliday | 공공기관 휴일여부 | 필수 | Y | 공공기관 휴일여부 |
| dateName | 명칭 | 필수 | 삼일절 | 명칭 |
| numOfRows | 페이지당항목수 | 필수 | 10 | 페이지당 항목 수 |
| pageNo | 페이지 | 필수 | 1 | 페이지 |
| totalCount | 모든항목수 | 필수 | 17 | 전체 항목 수 |

## 오퍼레이션 상세
### 국경일 정보 조회 (getHoliDeInfo)
- 설명: 연, 월별로 구분(국경일), 요일, 공휴일 여부 등의 정보를 제공한다.
- 비고: 제헌절은 해당 오퍼레이션에 포함되며 `isHoliday=N`으로 출력된다.

요청 예시:
```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getHoliDeInfo?solYear=2019&solMonth=03&ServiceKey=서비스키
```

응답 예시:
```xml
<response>
  <header>
    <resultCode>00</resultCode>
    <resultMsg>NORMAL SERVICE.</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <dateKind>01</dateKind>
        <dateName>삼일절</dateName>
        <isHoliday>Y</isHoliday>
        <locdate>20190301</locdate>
        <seq>1</seq>
      </item>
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>1</totalCount>
  </body>
</response>
```

### 공휴일 정보 조회 (getRestDeInfo)
- 설명: 월별로 구분(공휴일), 요일, 공휴일 여부 등의 정보를 제공한다.
- 비고: 제헌절은 해당 오퍼레이션에서 제공되지 않는다.

요청 예시:
```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?solYear=2019&solMonth=03&ServiceKey=서비스키
```

응답 예시:
```xml
<response>
  <header>
    <resultCode>00</resultCode>
    <resultMsg>NORMAL SERVICE.</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <dateKind>01</dateKind>
        <dateName>삼일절</dateName>
        <isHoliday>Y</isHoliday>
        <locdate>20190301</locdate>
        <seq>1</seq>
      </item>
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>1</totalCount>
  </body>
</response>
```

### 기념일 정보 조회 (getAnniversaryInfo)
- 설명: 월별로 구분(기념일), 요일, 공휴일 여부 등의 정보를 제공한다.

요청 예시:
```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getAnniversaryInfo?solYear=2015&solMonth=09&ServiceKey=서비스키
```

응답 예시:
```xml
<response>
  <header>
    <resultCode>00</resultCode>
    <resultMsg>NORMAL SERVICE.</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <dateKind>02</dateKind>
        <dateName>납세자의 날</dateName>
        <isHoliday>N</isHoliday>
        <locdate>20190303</locdate>
        <seq>1</seq>
      </item>
      <item>
        <dateKind>02</dateKind>
        <dateName>3·8 민주의거 기념일</dateName>
        <isHoliday>N</isHoliday>
        <locdate>20190308</locdate>
        <seq>1</seq>
      </item>
      <item>
        <dateKind>02</dateKind>
        <dateName>3·15 의거 기념일</dateName>
        <isHoliday>N</isHoliday>
        <locdate>20190315</locdate>
        <seq>1</seq>
      </item>
      <item>
        <dateKind>02</dateKind>
        <dateName>상공의 날</dateName>
        <isHoliday>N</isHoliday>
        <locdate>20190320</locdate>
        <seq>1</seq>
      </item>
      <item>
        <dateKind>02</dateKind>
        <dateName>서해수호의 날</dateName>
        <isHoliday>N</isHoliday>
        <locdate>20190322</locdate>
        <seq>1</seq>
      </item>
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>5</totalCount>
  </body>
</response>
```

### 24절기 정보 조회 (get24DivisionsInfo)
- 설명: 월별로 구분(24절기), 요일, 공휴일 여부 등의 정보를 제공한다.
- 추가 응답 항목: `kst`, `sunLongitude`

요청 예시:
```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/get24DivisionsInfo?solYear=2019&solMonth=03&ServiceKey=서비스키
```

응답 예시:
```xml
<response>
  <header>
    <resultCode>00</resultCode>
    <resultMsg>NORMAL SERVICE.</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <dateKind>03</dateKind>
        <dateName>경칩</dateName>
        <isHoliday>N</isHoliday>
        <kst>0610</kst>
        <locdate>20190306</locdate>
        <seq>1</seq>
        <sunLongitude>345</sunLongitude>
      </item>
      <item>
        <dateKind>03</dateKind>
        <dateName>춘분</dateName>
        <isHoliday>N</isHoliday>
        <kst>0658</kst>
        <locdate>20190321</locdate>
        <seq>1</seq>
        <sunLongitude>0</sunLongitude>
      </item>
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>2</totalCount>
  </body>
</response>
```

### 잡절 정보 조회 (getSundryDayInfo)
- 설명: 월별로 구분(잡절), 요일, 공휴일 여부 등의 정보를 제공한다.
- 추가 응답 항목: `kst`, `sunLongitude`

요청 예시:
```text
http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getSundryDayInfo?solYear=2015&solMonth=01&ServiceKey=서비스키
```

응답 예시:
```xml
<response>
  <header>
    <resultCode>00</resultCode>
    <resultMsg>NORMAL SERVICE.</resultMsg>
  </header>
  <body>
    <items>
      <item>
        <dateKind>04</dateKind>
        <dateName>토왕용사</dateName>
        <isHoliday>N</isHoliday>
        <kst>2002</kst>
        <locdate>20150117</locdate>
        <seq>1</seq>
        <sunLongitude>297</sunLongitude>
      </item>
    </items>
    <numOfRows>10</numOfRows>
    <pageNo>1</pageNo>
    <totalCount>1</totalCount>
  </body>
</response>
```

## Datekind 분류
| Datekind | 항목명 | 예시 |
| --- | --- | --- |
| 01 | 국경일 | 어린이 날, 광복절, 개천절 |
| 02 | 기념일 | 의병의 날, 정보보호의 날, 4·19 혁명 기념일 |
| 03 | 24절기 | 청명, 경칩, 하지 |
| 04 | 잡절 | 단오, 한식 |

## 기타 참고사항
- 데이터 업데이트는 각 오퍼레이션별로 연 1회 진행됩니다(1년치 데이터 일괄 업데이트).
- 특일정보는 6~8월 경 과학기술정보통신부 월력요항 발표 이후 차차년도 데이터를 먼저 업데이트합니다(현재연도 기준 +2년).
- 기념일, 24절기, 잡절은 11월경 업데이트됩니다.
- 임시공휴일 등 갑작스러운 데이터 발생 시 최대 1일 이내 업데이트됩니다.
- 대체공휴일이 추가 지정되는 경우 대통령령 공식 시행 이후 업데이트됩니다.

## 참고
- 서비스키는 포털에서 발급된 값을 사용합니다.
- 키 문자열에 `%2B`, `%3D`처럼 URL 인코딩된 문자가 포함돼 있다면 추가 인코딩 없이 전달하세요.
