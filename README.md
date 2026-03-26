# CampusForm Server

CampusForm Server는 Google Form/Sheet 기반 지원자 모집 운영을 위한 백엔드 API 서버입니다.  
프로젝트 생성, 지원자 동기화, 심사, 댓글 협업, 면접 운영, 알림/SMS 처리까지 운영 플로우를 하나로 묶는 데 초점을 둡니다.

## 프로젝트 소개

CampusForm은 스프레드시트에 쌓이는 지원 데이터를 프로젝트 단위로 관리하고, 운영진이 같은 기준으로 심사와 면접을 진행할 수 있게 만드는 서비스입니다.  
이 서버는 그 중심에서 인증, 프로젝트 관리, 지원자 관리, 인터뷰 운영, 알림 기능을 제공합니다.

## 해결하려는 문제

기존 모집 운영은 폼, 시트, 메신저, 문서가 분산되어 있어 상태 관리와 협업 기록이 끊기기 쉽습니다.  
CampusForm Server는 Google Sheet를 입력 소스로 유지하면서도, 그 이후의 심사/협업/면접 운영을 백엔드 도메인으로 통합합니다.

## 핵심 기능

- 프로젝트 생성, 수정, 삭제
- OWNER / ADMIN 권한 분리 및 관리자 관리
- Google OAuth 및 Google Sheets 연동
- 지원자 동기화, 조회, 상태 변경, 북마크
- 댓글 협업, 알림 조회/읽음 처리
- 면접 가능 시간 수집, 스마트 스케줄, 수동 배정
- 결과 안내용 SMS 템플릿 및 미리보기

## 시스템 아키텍처

도메인 중심 패키지 구조를 사용하며, 각 컨텍스트 내부에서 `presentation -> application -> domain -> infrastructure` 레이어로 책임을 나눕니다.

```text
src/main/java/com/campusform/server
├─ identity
├─ project
├─ recruiting
├─ notification
└─ global
```

- `identity`: 로그인, 사용자 정보, 프로필, 알림 수신 설정
- `project`: 프로젝트 관리, 관리자 관리, Google OAuth, Google Sheets 연동
- `recruiting`: 지원자 심사, 댓글, 면접 설정, 스케줄링, 결과 안내
- `notification`: 알림 저장/조회 및 이벤트 기반 알림 생성
- `global`: 보안, 공통 예외 처리, 설정

이벤트 기반 후처리도 일부 적용되어 있습니다.

- `AdminAddedEvent` -> 관리자 추가 알림
- `CommentCreatedEvent` -> 댓글 알림
- `SheetSyncCompletedEvent` -> 동기화 결과 알림
- `ApplicantUpdated` -> SMS 발송

## 기술 스택

| Category | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.5.9 |
| Security | Spring Security, OAuth2 Client |
| Persistence | Spring Data JPA |
| Database | MySQL 8.0, H2 |
| API Docs | Swagger UI, OpenAPI |
| External Integration | Google OAuth, Google Sheets API, AWS S3 |
| Infra | Docker, Nginx, AWS EC2, GHCR |
| CI/CD | GitHub Actions |

## 주요 도메인 흐름

1. 운영진이 프로젝트를 생성하고 Google Sheets 권한을 연결합니다.
2. 서버가 Sheet 응답을 읽어 지원자를 프로젝트에 동기화합니다.
3. 운영진이 지원자를 심사하고 댓글로 협업합니다.
4. 면접 단계에서는 가능 시간 수집, 스마트 스케줄 생성, 수동 조정을 진행합니다.
5. 결과 안내와 운영 이벤트는 알림/SMS 후처리로 이어집니다.

프로젝트 상태 흐름:

```text
DOCUMENT -> INTERVIEW -> INTERVIEW_COMPLETE
DOCUMENT -> DOCUMENT_COMPLETE
```

## 환경 변수 / 외부 연동

이 프로젝트는 아래 연동에 의존합니다.

- DB: MySQL 운영 DB, H2 로컬 개발 DB
- Google OAuth: 로그인 및 Sheets 권한 위임
- Google Sheets API: 지원자 데이터 동기화
- AWS S3: 프로필 이미지 저장
- CORS / Cookie / 배포 변수: 프론트엔드 연동 및 운영 환경 설정

대표 환경 변수 범주는 다음과 같습니다.

- DB: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- OAuth: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FRONTEND_URL`
- Sheets Redirect: `APP_OAUTH2_LOGIN_REDIRECT_URI`, `APP_OAUTH2_SHEETS_REDIRECT_URI`
- CORS / Cookie: `CORS_ALLOWED_ORIGINS`, `COOKIE_DOMAIN`, `COOKIE_SECURE`
- AWS S3: `AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`

## API 문서 

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI Docs: `/v3/api-docs`
- 인증 방식: `JSESSIONID` 쿠키 기반

## 배포 방식

배포는 GitHub Actions 기반으로 자동화되어 있습니다.

1. `main` 브랜치에 push
2. 테스트 실행
3. Docker 이미지 빌드 및 GHCR push
4. EC2로 배포 자산 복사 후 `docker compose` 재기동

운영 구성은 `app`, `db`, `nginx` 서비스 기준이며, 자세한 파이프라인은 [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml), 컨테이너 구성은 [`docker-compose.prod.yml`](docker-compose.prod.yml)에서 확인할 수 있습니다.

## ERD / 다이어그램

### ERD

<img width="3438" height="2416" alt="CampusForm 최종 ERD" src="https://github.com/user-attachments/assets/233b237b-68ac-4bd2-bce9-6609c29b47a9" />

### 아키텍처 다이어그램

<img width="697" height="398" alt="CampusForm Architecture" src="https://github.com/user-attachments/assets/7f35a691-9e30-4164-b31a-19bdbfc5edd7" />

## 협업 방식

- 기준 브랜치는 `main`이며, 운영 배포와 연결됩니다.
- 기능 개발은 `feat/기능명` 브랜치에서 진행하고 PR로 병합합니다.
- 리뷰 후 `main`에 병합되면 GitHub Actions를 통해 자동 배포됩니다.
- 도메인 책임 분리와 이벤트 기반 후처리 원칙을 유지하는 것을 우선합니다.
