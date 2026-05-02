# CampusForm Server

> Google Form/Sheet 기반 모집 운영을 하나의 플랫폼으로 통합하는 백엔드 API 서버

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?logo=springboot) ![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql) ![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

---

## 목차

- [서비스 소개](#서비스-소개)
- [해결하려는 문제](#해결하려는-문제)
- [핵심 기능](#핵심-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [기술 스택](#기술-스택)
- [도메인 흐름](#도메인-흐름)
- [로컬 개발 환경 설정](#로컬-개발-환경-설정)
- [환경 변수](#환경-변수)
- [API 문서](#api-문서)
- [배포 방식](#배포-방식)
- [ERD / 다이어그램](#erd--다이어그램)

---

## 서비스 소개

CampusForm은 동아리·학생단체의 신규 모집 운영을 위한 플랫폼입니다.  
이 서버는 인증, 프로젝트 관리, 지원자 심사, 면접 운영, 알림/SMS 처리까지 모집 전 과정의 백엔드를 담당합니다.

---

## 해결하려는 문제

기존 모집 운영은 Google Form, 스프레드시트, 메신저, 별도 문서가 분산되어 있어 지원자 상태 관리와 팀 협업 기록이 끊기기 쉽습니다.

CampusForm Server는 **Google Sheet를 입력 소스로 유지하면서** 그 이후의 심사·협업·면접 운영을 하나의 백엔드 도메인으로 통합합니다.

---

## 핵심 기능

| 카테고리 | 기능 |
|---|---|
| 프로젝트 | 생성·수정·삭제, OWNER/ADMIN 권한 관리 |
| 지원자 | Google Sheets 동기화, 조회·필터링, 상태 변경, 북마크 |
| 심사 | 서류·면접 심사 결과(PASS/FAIL/HOLD), 댓글 협업 |
| 면접 | 가능 시간 수집, 스마트 자동 스케줄링, 수동 배정 |
| 결과 안내 | SMS 템플릿 생성·미리보기 |
| 알림 | 이벤트 기반 알림 생성·조회·읽음 처리 |

---

## 시스템 아키텍처

도메인 중심 패키지 구조를 사용하며, 각 컨텍스트 내부에서 `presentation → application → domain → infrastructure` 레이어로 책임을 분리합니다.

```text
src/main/java/com/campusform/server
├─ identity      # 로그인, 사용자 정보, 프로필, 알림 수신 설정
├─ project       # 프로젝트 관리, 관리자 관리, Google OAuth, Google Sheets 연동
├─ recruiting    # 지원자 심사, 댓글, 면접 설정·스케줄링, 결과 안내
├─ notification  # 알림 저장·조회 및 이벤트 기반 알림 생성
└─ global        # 보안, 공통 예외 처리, 공통 설정
```

**이벤트 기반 후처리**

| 이벤트 | 후처리 |
|---|---|
| `AdminAddedEvent` | 관리자 추가 알림 생성 |
| `CommentCreatedEvent` | 댓글 알림 생성 |
| `SheetSyncCompletedEvent` | 동기화 결과 알림 생성 |
| `ApplicantUpdated` | SMS 결과 안내 발송 |

---

## 기술 스택

| 카테고리 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.9 |
| Security | Spring Security, OAuth2 Client |
| Persistence | Spring Data JPA (Hibernate) |
| Database | MySQL 8.0 (운영), H2 (로컬) |
| API Docs | Swagger UI / OpenAPI 3 |
| External | Google OAuth2, Google Sheets API v4, AWS S3 SDK v2 |
| Infra | Docker, Docker Compose, Nginx, AWS EC2, GHCR |
| CI/CD | GitHub Actions |

---

## 도메인 흐름

```
1. 프로젝트 생성 → Google Sheets 권한 연결
2. Sheet 데이터 → 지원자 자동 동기화
3. 서류 심사 (PASS / FAIL / HOLD) + 댓글 협업
4. 면접 가능 시간 수집 → 스마트 스케줄 생성 → 수동 조정
5. 결과 SMS 안내 + 이벤트 알림
```

**프로젝트 상태 전이**

```
DOCUMENT ──────────────► DOCUMENT_COMPLETE
    │
    └──► INTERVIEW ──────► INTERVIEW_COMPLETE
```

---

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 17
- Docker & Docker Compose

### 실행 방법

**1. MySQL 컨테이너 실행**

```bash
docker compose -f docker-compose.local.yml up -d
```

로컬 MySQL은 포트 `3307`에서 실행됩니다.

| 항목 | 값 |
|---|---|
| Host | `localhost:3307` |
| Database | `campusform_test` |
| Username | `campusform` |
| Password | `campusform` |

**2. 환경 변수 설정**

`src/main/resources/application-local.yml` 또는 IDE Run Configuration에서 아래 [환경 변수](#환경-변수)를 설정합니다.

**3. 애플리케이션 실행**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 환경 변수

| 변수 | 설명 |
|---|---|
| `DB_URL` | JDBC URL (e.g. `jdbc:mysql://localhost:3307/campusform_test`) |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 시크릿 |
| `FRONTEND_URL` | 프론트엔드 URL (OAuth 리다이렉트, CORS 허용) |
| `APP_OAUTH2_LOGIN_REDIRECT_URI` | OAuth2 로그인 완료 후 리다이렉트 URI |
| `APP_OAUTH2_SHEETS_REDIRECT_URI` | Sheets 권한 위임 완료 후 리다이렉트 URI |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 오리진 목록 |
| `COOKIE_DOMAIN` | 세션 쿠키 도메인 |
| `COOKIE_SECURE` | 세션 쿠키 Secure 속성 (`true` / `false`) |
| `AWS_S3_BUCKET` | S3 버킷 이름 |
| `AWS_S3_REGION` | S3 리전 (e.g. `ap-northeast-2`) |
| `AWS_ACCESS_KEY` | AWS Access Key |
| `AWS_SECRET_KEY` | AWS Secret Key |

---

## API 문서

| 항목 | URL |
|---|---|
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI Spec | `/v3/api-docs` |
| 인증 방식 | `JSESSIONID` 쿠키 기반 세션 |

---

## 배포 방식

`main` 브랜치 push 시 GitHub Actions 파이프라인이 자동 실행됩니다.

```
main push
  → 테스트 실행
  → Docker 멀티스테이지 빌드 (JDK 17 builder → JRE slim)
  → GHCR 이미지 push (latest + commit SHA 태그)
  → EC2로 배포 자산 복사
  → docker compose 재기동 (app / db / nginx)
```

- 파이프라인 상세: [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)
- 운영 컨테이너 구성: [`docker-compose.prod.yml`](docker-compose.prod.yml)

---

## ERD / 다이어그램

### ERD

<img width="3438" height="2416" alt="CampusForm 최종 ERD" src="https://github.com/user-attachments/assets/233b237b-68ac-4bd2-bce9-6609c29b47a9" />

### 아키텍처 다이어그램

<img width="697" height="398" alt="CampusForm Architecture" src="https://github.com/user-attachments/assets/7f35a691-9e30-4164-b31a-19bdbfc5edd7" />
