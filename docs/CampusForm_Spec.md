# CampusForm All-in-One Spec (Cursor 학습용)

버전: v3 (통합 버전)  
기준일: 2026-01-12 (KST)  
문서 목적: **CampusForm 백엔드/도메인/DB/API/정책/UI를 한 파일에 통합**하여 Cursor AI 등 도구에 학습 입력으로 사용하기 위함.

---

## 0. 한 줄 소개

CampusForm은 **구글 폼(구글 시트) 응답을 기반으로 지원자를 동기화**하고,  
관리자가 **서류/면접 단계에서 지원자를 관리(보류/합격/불합격, 댓글, 즐겨찾기, 결과 페이지/문자 템플릿)** 하며,  
면접 단계에서는 **스마트 시간표(면접관 30분 후보 → 면접 슬롯 생성 → 지원자 선택 → 자동 배정)** 를 제공하는 서비스다.

---

## 1. 범위(Scope)

### 포함
- 인증/인가(세션 기반) + 프로젝트 권한(OWNER/ADMIN)
- 프로젝트 생성/관리자 초대/알림
- 구글 시트 연동 및 동기화(지원자/추가 질문)
- 지원자(서류/면접) 상태 관리(보류/합격/불합격), 검색/정렬, 즐겨찾기, 댓글(수정/삭제 포함)
- **결과 탭(서류 결과/면접 결과)**: 통계, 명단 전체보기, 문자 템플릿(@이름, @포지션), 개인별 문자 복사
- **스마트 시간표**: 면접 정보 설정, 면접관 30분 후보 등록, 지원자 시간 링크 모집, 응답 결과 확인, 자동 배정 결과/확정

### 제외(현 단계)
- 감사로그(Audit Log)
- 동시성 고도화(낙관/비관락 등) – 1차에서는 단순 처리
- 알림 "발송(SMS)" 자체 (복사 기능만 제공, 실제 발송은 외부에서)

---

## 2. 용어(Glossary)

- **프로젝트(Project)**: 하나의 모집 공고(채용 프로세스 단위). 모집 기간/시트 연동/관리자/단계(state)를 포함.
- **OWNER / ADMIN**: 프로젝트 관리자 역할.
- **지원자(Applicant)**: 시트로부터 동기화되는 모집 참여자.
- **서류/면접 상태(Document/Interview Status)**: `HOLD / PASS / FAIL`
- **단계(State)**: 프로젝트 진행 단계(`DOCUMENT_*`, `INTERVIEW_*`, `*_COMPLETE`)
- **잠금(LOCKED)**: 해당 단계에서 지원자 상태 변경을 막는 상태.
- **스마트 시간표**
  - **면접관 후보 시간(30분 블록)**: 면접관이 가능한 시간(입력 단위)
  - **지원자 선택 슬롯(슬롯 단위)**: 관리자가 설정한 면접 소요시간(예: 20분) 기준 슬롯(배포 단위)
  - **최종 시간표(확정)**: 알고리즘 결과로 확정된 면접 스케줄

---

## 3. Bounded Context(컨텍스트) 구성

CampusForm은 역할/변경이유(Change Reason)가 다른 기능들을 분리하기 위해 4개 컨텍스트로 구성한다.

### 3.1 Identity Context
> **사용자 식별/인증** 컨텍스트

- 로그인(OAuth2: 구글)
- 세션/쿠키 기반 인증
- 프로필(닉네임, 프로필 이미지)
- 개인 알림 수신 설정(ON/OFF)

**설계 원칙**
- Recruiting/Project와 완전 분리
- 다른 컨텍스트는 `userId`만 참조(유저 소유권은 Identity)

### 3.2 Project Context (**프로젝트 설정만 포함**)
> **모집 공고(프로젝트)와 운영 설정** 컨텍스트

- 프로젝트 생성/수정/삭제(hard delete)
- 모집 기간(시작/종료)
- 관리자 관리(OWNER/ADMIN)
- 구글 시트 연동 및 동기화 설정(OAuth 토큰 포함)
- 면접 시간표 "규칙/정책" 설정(슬롯 길이/휴식/인원 제한 등)

**주의**
- 지원자 평가/댓글/결과/시간표 "배정"은 포함하지 않음(Recruiting 소관)

### 3.3 Recruiting Context (**프로젝트 생성 이후 과정**)
> **실제 채용 프로세스(핵심 Core Domain)**

- 지원자 목록/필터/정렬/상태 변경
- 서류/면접 평가(PASS/FAIL/HOLD)
- 댓글(수정/삭제 포함), 즐겨찾기
- 서류 결과/면접 결과 페이지(통계, 명단 복사, 문자 템플릿/개인별 복사)
- 스마트 시간표 생성/배정/확정

### 3.4 Notification Context
> **인앱 알림** 컨텍스트

- 알림 생성/조회/읽음 처리
- 알림 정책 확장 여지를 위해 독립 유지(추후 이메일/푸시/SMS 가능)

---

## 4. 운영/정책 요약(확정)

### 4.1 인증/보안
- 로그인 유지: **세션/쿠키**
- 세션 만료: **24시간**, 활동 시 갱신(sliding)
- CSRF: 세션 기반 방어 정책 적용  
  - 공개 링크 API는 **토큰(token) 검증**으로 보호
- OAuth2: 구글만 제공
- 시트 접근 주체: **OWNER OAuth 토큰**

### 4.2 권한/역할

#### 역할 정의
- **OWNER**: 프로젝트 내 모든 권한
- **ADMIN**: 아래 제외 거의 ALL
  - 다른 관리자 추가/초대 불가
  - 프로젝트 삭제 불가
  - 다음 단계(state) 이동 불가(마감하기/종료하기 포함)
  - 지원자 상태(보류/합격/불합격) 변경 가능
  - 결과/스마트시간표 조회 및 편집 가능

#### 권한 핵심 규칙
- **마감하기 버튼은 OWNER에게만 노출/동작**
  - 서류 결과: `서류 마감하기` OWNER only
  - 면접 결과: `면접 마감하기` OWNER only
- 서류/면접 합/불(상태 변경): **OWNER/ADMIN 모두 가능**

### 4.3 권한 기반 라우팅/기능 차단(서버도 강제)
- ADMIN 접근 불가
  - 관리자 관리 화면(초대/삭제)
  - 프로젝트 삭제
  - 프로젝트 단계 변경(다음 단계로 이동) 관련 API

### 4.4 단계(state) / 마감(lock)
- 단계는 단방향 진행(되돌리기 불가)
- 다음 단계 이동 조건(권장/기본): **모든 지원자가 HOLD 없이 PASS/FAIL 확정**
- 잠금 의미
  - `*_LOCKED` 이후 해당 단계에서의 상태 변경을 막는다.
  - 잠금 이후 허용 범위(확정)
    - 댓글: 작성/수정/삭제 가능(면접 코멘트 운영)
    - 즐겨찾기: 가능
    - 동기화: 가능(단, 기존 상태값 PASS/FAIL은 유지)

> 참고: `DOCUMENT_DONE` 같은 중간 상태는 "완료 페이지 1회 노출" 등 UI 정책에 맞춰 내부적으로 LOCKED와 동일 취급하거나, 별도 플래그로 대체 가능.

### 4.5 시트 동기화
- 트리거: 프론트 새로고침 모션(또는 동등 사용자 액션)에서 동기화 API 호출
- 중복 판단: 프로젝트 내 `(name + email)` 유니크
- 시트에서 삭제는 불가하므로 DB 삭제 동기화는 고려하지 않음
- 동기화로 지원서 수정 감지 시 알림: "OOO 님이 지원서를 수정했어요."

---

## 5. 프로젝트 상태 모델

### 5.1 Enum: project_state
- `DOCUMENT_OPEN`: 서류 평가 가능
- `DOCUMENT_LOCKED`: 서류 마감(상태 변경 불가)
- `DOCUMENT_DONE`: (선택) 서류 결과 완료 페이지 1회 노출 이후 상태
- `DOCUMENT_COMPLETE`: 면접 없이 서류로 종료
- `INTERVIEW_OPEN`: 면접 진행/시간표/평가 가능
- `INTERVIEW_LOCKED`: 면접 마감(상태 변경 불가)
- `ALL_COMPLETE`: 면접까지 종료

### 5.2 전환 규칙(상세)

#### 서류 마감 플로우
- (OWNER) **서류 결과 페이지 → `서류 마감하기`**
  - 최초 1회: "서류 결과 완료(축하)" 페이지 노출
  - 이후 재진입 시: 바로 면접 단계(또는 면접 지원자 관리)로 이동

#### 서류 결과 완료 페이지(최초 1회)
- 축하 일러스트/메시지
- 버튼 2개
  - `다음 단계: 면접 설정하기`
    - 확인 모달: "면접 단계로 이동하면 서류 단계는 종료되며 수정할 수 없습니다."
    - 확인 시: 상태 전환 `INTERVIEW_OPEN`
  - `면접 없이 모집 종료하기`
    - 확인 모달 후 종료
    - 상태 전환 `DOCUMENT_COMPLETE`

#### 면접 마감 플로우
- (OWNER) **면접 결과 페이지 → `면접 마감하기`**
  - 최초 1회: "면접 결과 완료(축하)" 페이지 노출
  - `종료하기` 확인 모달 → `ALL_COMPLETE`
  - 이후: 바로 "면접 지원자 관리" 또는 홈 이동(정책대로)

---

## 6. 기능 요구사항(상세)

## 6.1 Identity

### 6.1.1 로그인(구글 OAuth)
- 구글 OAuth 로그인
- 로그인 성공 시 세션 발급
- 신규 유저면 프로필 초기 설정(닉네임/프로필 이미지)

### 6.1.2 세션
- 세션 유효성 검증 API
- 슬라이딩 만료(활동 시 연장)

### 6.1.3 프로필/설정
- 프로필 조회/수정(닉네임/이미지)
- 알림 수신 설정 토글

---

## 6.2 Project (설정만)

### 6.2.1 프로젝트 생성/조회/삭제
- 프로젝트 생성(제목, 모집 기간)
- 프로젝트 목록/상세 조회
- 프로젝트 삭제: OWNER만, hard delete

### 6.2.2 관리자 관리
- 프로젝트 관리자 목록 조회
- 관리자 초대(이메일 기반)
  - 초대 대상이 가입자가 아니면 에러
- 권한: OWNER/ADMIN

### 6.2.3 구글 시트 연동
- 시트 URL 저장(파싱 필요)
- OAuth 토큰 저장(OWNER 1개)
- 시트 연동 실패 시에도 UI 버튼은 살아 있어야 함(재시도 가능)

### 6.2.4 면접 설정(규칙)
- 면접 가능 날짜(복수일)
- 일일 시작/종료 시간
- 슬롯 길이(예: 20분) + 휴식 시간(예: 0~10분)
- 슬롯당 최대 지원자 수
- 슬롯당 최소/최대 면접관 수

---

## 6.3 Recruiting (프로젝트 생성 이후)

### 6.3.1 지원자 동기화 데이터 모델
- 기본 컬럼 매핑(이름/이메일/전화/성별/학교/학과/포지션 등)
- 고정 컬럼 외 질문은 "추가 질문" 테이블로 저장

### 6.3.2 지원자 목록(서류/면접 공통)
- 검색(이름/이메일/전화 등)
- 필터(상태 HOLD/PASS/FAIL 등)
- 정렬(최신순, 이름순 등)
- 즐겨찾기(프로젝트 공용)

### 6.3.3 상태 변경(서류/면접)
- OWNER/ADMIN 모두 지원자 상태 변경 가능
- `*_LOCKED` 상태에서는 상태 변경 불가(서버에서 차단)

### 6.3.4 댓글(면접 코멘트) — 케밥 메뉴 수정/삭제
- 지원자별 댓글 작성
- 대댓글(부모 댓글) 지원
- 케밥 메뉴: 수정/삭제
- 권한(권장)
  - 작성자 본인: 수정/삭제 가능
  - OWNER: 모든 댓글 삭제 가능(선택)
  - ADMIN: 본인 댓글만 수정/삭제(권장)

### 6.3.5 결과 페이지(서류/면접 공통 구조)

#### 결과 네비게이션
- 결과 탭 진입 시 하단에 2개 버튼(또는 카드)
  - `서류 결과` 페이지로 이동
  - `면접 결과` 페이지로 이동

#### 서류 결과 페이지

##### 화면 구성(공통)
- 상단: 뒤로가기, 알림(벨)
- 공고명(프로젝트 title)
- 통계 요약 영역
  - 총 지원자 수
  - 서류 합격자 수
  - 서류 경쟁률(표기 규칙 정의: 예. "지원자:합격자" 또는 "지원자/합격자")
  - 성비 막대(남/여 비율) *(성별 데이터 없으면 0 또는 미표기 정책)*
- 탭: `합격자` / `불합격자`

##### 합격/불합격 탭 공통 기능

**A) 명단 카드 + 전체보기**
- `서류 (합격/불합격)자 명단 (N명)` 카드
- `전체보기` 클릭 시 **명단 상세(바텀시트/모달)** 노출
  - 상단 버튼:
    - `명단 복사하기` : "이름(학교/학과/포지션)" 형태로 전체 복사
    - `전화번호 복사하기` : `010-1234-5678, 010-XXXX-XXXX ...` 형태로 전체 복사(쉼표 구분)
  - 리스트: 지원자 요약(이름 + 부가정보)
    - 부가정보 표준: `연령대 / 학과 / 포지션`
    - 값이 없으면 `-` 처리

**B) 문자 템플릿 입력(자동저장)**
- 템플릿 입력 영역(텍스트)
- 안내 문구:
  - "@이름", "@포지션" 변수를 사용 가능
- 버튼:
  - `변수 적용하기` : 아래 개인별 메시지 생성/갱신 트리거
  - `템플릿 복사하기` : 입력 템플릿 원문을 복사(일괄 전송용)

**C) 템플릿 사용법 안내 팝업**
- (i) 또는 안내 버튼 클릭 시 팝업
- 내용(최신 UI 반영):
  - **동일 문장 일괄 전송**
    1. (전체보기) `전화번호 복사하기`
    2. `템플릿 복사하기`
    3. 외부 문자 앱에서 전체에게 동일 문장 전송
  - **자동 치환 개별 전송**
    1. 템플릿에 `@이름`, `@포지션` 입력
    2. `변수 적용하기`로 개인별 메시지 생성
    3. 개인별 `문자 복사하기/전화번호 복사하기`로 개별 전송

**D) 개인별 메시지 리스트(자동 치환 결과)**
- 영역 제목: `(합격/불합격)자 개인별 문자`
- 각 지원자 행:
  - 좌측: 이름 + 부가정보
  - 우측 아이콘 2개:
    - `문자 복사` 아이콘
    - `전화번호 복사(#)` 아이콘
- `문자 복사` 클릭 시 **개인별 메시지 상세 바텀시트**
  - 지원자 정보 표시(이름/학교/학과/포지션)
  - 버튼:
    - `문자 복사하기` : 치환된 최종 메시지 복사
    - `전화번호 복사하기` : 해당 지원자 번호 복사
  - 본문: 치환된 메시지(예: "김민준 님 … 포지션 …")

##### Empty State
- 합격자 0명 / 불합격자 0명인 경우
  - 리스트/전체보기는 비활성 또는 "0명" 표기 유지
  - 개인별 메시지 리스트는 비표시 또는 빈 상태 표시

##### 서류 마감하기(OWNER only)
- 버튼: `서류 마감하기`
  - **ADMIN에게는 버튼 미노출**
- 동작:
  - 최초 1회: "서류 결과 완료(축하)" 페이지로 이동
  - 이후: 바로 면접 관련 초기 페이지로 이동(정책대로)

#### 면접 결과 페이지
> 서류 결과와 구조/기능 동일 (단, "면접" 기준의 값으로 치환)

##### 공통
- 통계: 총 지원자 수, 면접 합격자 수, 면접 경쟁률, 성비
- 탭: 합격자/불합격자
- 명단 전체보기 + 복사 기능 동일
- 문자 템플릿 동일
  - 변수: `@이름`, `@포지션`
- 개인별 메시지 바텀시트 동일

##### 면접 마감하기(OWNER only)
- 버튼: `면접 마감하기`
  - **ADMIN에게는 버튼 미노출**
- 최초 1회: "면접 결과 완료" 페이지로 이동
- 이후: 바로 "면접 지원자 관리" 또는 홈 이동(정책대로)

##### 면접 결과 Empty 화면
- 면접이 아직 없을 때(예: 서류 단계인데 면접 결과 탭 진입)
  - "진행 중인 면접이 없습니다…"
  - 상단: 이전/알림/공고명/안내문구

---

## 6.4 스마트 시간표(Recruiting 핵심)

### 6.4.1 개요(핵심 로직)

**입력**
1) 면접 설정(프로젝트 규칙)  
2) 면접관 가능 시간 후보(30분 단위)  
3) 지원자 가능 시간(슬롯 단위 선택 제출)  

**출력**
- 최종 확정 면접 시간표(슬롯별 지원자/면접관 배정)
- 배정 불가 지원자 목록 + 사유

**개념/정책**
- 면접관 시간 후보 입력 단위: **30분**
- 지원자 노출 슬롯 단위: **slot_duration_min(+break 정책)**
- 예: 면접 소요 20분, 휴식 0분 → 20분 단위 슬롯 생성
- 목표: 제약조건을 만족하면서 가능한 많은 지원자를 배정
  - 슬롯당 지원자 최대치
  - 슬롯당 면접관 최소/최대
  - 필수 면접관이 있다면 포함 여부(운영 정책)

### 6.4.2 관리자 화면 요구(상세)

#### 스마트 시간표 메인(관리자)
- 섹션 구조(아코디언/스텝)
  1. **면접 정보 설정**(미설정이면 다른 기능 비활성)
  2. **면접관 시간 등록**
  3. **지원자 면접 가능 시간 모집**

#### 1) 면접 정보 설정 페이지
- 입력
  - 면접 날짜(캘린더)
  - 시작/종료 시간
  - 타임당 지원자 수(최대)
  - 타임당 면접관 수(최소/최대)
  - 예상 소요 시간(분/한 타임 기준)
  - 휴식 시간(분)
  - 포지션별 면접 토글(같은 타임에 같은 포지션끼리 배정 우선)
- 버튼
  - `설정하기`
- 설정 완료 후
  - 2) 면접관 시간 등록, 3) 지원자 시간 모집 기능 활성화

#### 2) 면접관 시간 등록
- 드롭다운: `전체` / 특정 면접관 선택
- 면접관 아코디언(면접관별)
  - 프로필/닉네임/이메일
  - **필수 면접관 스위치**
  - 면접관별 담당 포지션(복수 선택)
- 전체 시간표(그리드)
  - 설정된 날짜/시간 범위를 표시
  - 선택된(겹치는) 시간이 많을수록 진하게 표시(표현 정책)
  - 특정 시간 클릭 시: 해당 시간 참여 면접관 리스트 팝업
  - 프로필 클릭 시: 참여/미참여 리스트 페이지(카톡 투표 느낌)
  - `시간으로 돌아가기` 버튼 제공

#### 3) 지원자 면접 가능 시간 모집
- 공개 링크 관리
  - 링크 URL 표시 + 복사 아이콘
  - `지원자 시간 페이지 편집` 버튼
  - `응답 결과 확인` 버튼
  - `지원자 전화번호 복사` 버튼(문자 전송을 위한 전체 복사용)
- 하단 CTA
  - `스마트 시간표 생성` 버튼

#### 지원자 시간 모집 페이지(관리자 편집)
- 스위치: `지원자 응답 받기`
  - ON: 링크 공개(지원자 제출 가능)
  - OFF: 비공개(제출 불가) + 안내 문구 노출
- 안내 문구 입력(자동저장)
- 면접 가능 시간(칩/버튼)
  - 면접 정보 설정값에 따라 자동 생성
  - 슬롯은 "면접 소요 시간" 단위(예: 20분)로 노출
- 버튼
  - `저장하기`
- UI 보조
  - 화면 최상단 이동 버튼

#### 지원자 시간 모집 페이지(지원자용)
- 표시
  - 관리자 안내 문구
- 입력
  - 이름
  - 전화번호
- 시간 선택(드롭다운)
  - 날짜별 펼치기
  - 슬롯 칩 다중 선택
- 제출
  - `제출하기`
- 제출 완료 페이지
  - 체크/축하 메시지
  - `종료하기`(홈으로)

#### 응답 결과 확인(관리자)
- 검색창(지원자 이름)
- 날짜/시간별로 "가능한 모든 지원자(이름/학교/학과/포지션)" 표시
- 검색된 지원자는 강조 표시

#### 스마트 시간표 결과(자동배정 결과)
- 상단
  - `스마트 시간표 유의사항 안내(i)` 팝업
    - 알고리즘 설명
    - "겹치는 일정이 없도록 효율 조합 우선"
    - "확정 후 수동 변경 가능(정책 선택)"
- 배정 불가 인원 섹션
  - 인원 수 + 지원자 정보 + 사유(예: "면접관과 시간이 중복되지 않습니다.")
- 날짜별 배정 카드
  - 시간 범위
  - 지원자 리스트(이름/학교/학과/포지션)
  - 면접관 리스트(닉네임)
- CTA
  - `면접 시간 확정`
    - 확정 시 "확정된 시간표"로 저장 + 이후 결과/면접 단계에서 사용

#### 확정 이후 정책(권장)
- 확정된 시간표는 "면접 운영 데이터"로 고정
- 재생성 시:
  - 기존 확정본을 "이전 버전"으로 두거나(추천),
  - 강제 덮어쓰기(OWNER 확인) 중 택1

### 6.4.3 알고리즘(구현 가능 수준의 윤곽)
- 단계 1: 면접 설정으로 슬롯 후보(날짜×시간) 생성
- 단계 2: 면접관 30분 후보 블록을 슬롯 단위로 변환 가능한지 계산
  - 슬롯이 특정 면접관 블록에 완전히 포함되는지(또는 겹침 정책)로 "가능 여부" 도출
- 단계 3: 지원자 제출 슬롯과 교집합 생성(지원자별 가능한 슬롯 목록)
- 단계 4: 배정(그리디/휴리스틱 권장)
  - 우선순위 예시:
    1) 가능한 슬롯 수가 적은 지원자부터 배정(제약이 큰 지원자 우선)
    2) 슬롯별 남은 자리/면접관 수 제약 체크
    3) 필수 면접관 제약이 있으면 우선 반영
- 단계 5: 배정 실패 인원은 사유 기록
  - "선택한 슬롯에 가능한 면접관이 부족"
  - "선택한 슬롯이 존재하지 않음(설정 변경 등)"
  - "슬롯 정원 초과"

---

## 6.5 Notification

### 6.5.1 인앱 알림
- 알림 리스트 조회(최신순)
- 읽음 처리(read_at)
- 알림 수신 설정 OFF 시(사용자 설정) 신규 생성 제한 또는 표시만 제한(정책 선택)

### 6.5.2 알림 트리거(예)
- 시트 동기화 결과(SUCCESS/ERROR, 수정 감지 포함)
- 새 지원자 유입
- 댓글 생성
- 관리자 추가

---

## 7. DB 설계(DBML) — 전체

Enum project_state { // 프로젝트 단계(상태)
  DOCUMENT_OPEN // 프로젝트 생성 완, 서류 단계
  DOCUMENT_LOCKED // 서류 합불 결정 -> 발송
  DOCUMENT_DONE // 서류 완료 및 프로젝트 종료
  INTERVIEW_OPEN // 면접, 스마트시간표
  INTERVIEW_LOCKED  // 면접 합불 결정 -> 발송
  ALL_COMPLETE // 면접 종료 및 모집 공고 종료
}

Enum applicant_status { // 지원자 심사 결과(단계별)
  HOLD
  PASS
  FAIL
}

Enum project_role { // 프로젝트 관리자 역할
  OWNER
  ADMIN
}

Enum sync_status { // 동기화 상태
  OK
  ERROR
}

Enum notification_type { // 알림 타입 -> 그에 내용 작성
  SHEET_SYNC_RESULT
  NEW_APPLICANT
  COMMENT_CREATED
  ADMIN_ADDED
}

Enum interview_schedule_run_status {
  DRAFT
  CONFIRMED
  SUPERSEDED
}

// =====================================================

Table users {
  id int [primary key, increment]
  email varchar [not null, unique]
  nickname varchar [not null]
  profile_image_url varchar
  created_at timestamp [not null, default: `now()`]

  Note: '관리자 계정 관리'
}

Table user_notification_settings {
  id int [primary key, increment]

  user_id int [not null]

  notification_enabled boolean [not null, default: true]

  updated_at timestamp [not null, default: `now()`]

  indexes {
    (user_id) [unique, Note: '사용자당 알림 설정 1개']
  }

  Note: '개인별 알림 설정. 설정은 부가정보로 별도 테이블로 관리'
}


Table projects {
  id int [primary key, increment]
  title varchar [not null]

  owner_id int [not null]

  state project_state [not null, default: 'DOCUMENT_OPEN']

  sheet_url varchar [not null, Note: 'API 호출 위해 파싱 필요']

  last_sync_status sync_status [not null, default: 'OK'] 
  last_synced_at timestamp [Note: '동기화 과도 호출 제한 필요']

  start_at date [not null]
  end_at date [not null]

  created_at timestamp [not null, default: `now()`]
  updated_at timestamp [not null, default: `now()`]

  indexes {
    (owner_id) [Note: 'FOR owner_id -> 프로젝트 조회']
  }
}


Table project_admins {
  id int [primary key, increment]
  project_id int [not null]
  admin_id int [not null]

  role project_role [not null, default: 'ADMIN', Note: '확장 대비']
  created_at timestamp [not null, default: `now()`]

  indexes {
    (project_id, admin_id) [unique, Note: 'Unique 제약']
    (project_id) [Note: 'FOR project_id -> 관리자 목록 조회']
  }

  Note: '프로젝트 별 ADMIN 관리'
}


Table project_message_templates {
  project_id int [primary key] // 값 타입 -> FK(PK)

  template_document_pass text [Note: '서류 합격 -> 메시지 템플릿']
  template_document_fail text [Note: '서류 불합격 -> 메시지 템플릿']
  template_interview_pass text [Note: '면접 합격 -> 메시지 템플릿']
  template_interview_fail text [Note: '면접 불합격 -> 메시지 템플릿']

  updated_at timestamp [not null, default: `now()`]

  Note: '프로젝트 템플릿은 부가정보로 별도 테이블로 관리'
}


Table google_oauth_tokens {
  id int [primary key, increment]
  owner_id int [not null, Note: '시트를 읽는 OWNER 관리자']

  access_token text [not null, Note: 'API 호출용']
  refresh_token text [Note: '갱신용']
  expiry_at timestamp

  created_at timestamp [not null, default: `now()`]
  updated_at timestamp [not null, default: `now()`]

  indexes {
    (owner_id) [unique, Note: 'OWNER 당 토큰 1개 존재']
  }

  Note: '시트 접근용 OAuth 토큰. 민감 정보 -> 분리 저장'
}


Table applicants {
  id int [primary key, increment]
  project_id int [not null]

  name varchar [not null]
  school varchar
  major varchar
  gender varchar
  phone varchar
  email varchar [not null]
  position varchar
  

  document_status applicant_status [not null, default: 'HOLD']
  interview_status applicant_status [default: 'HOLD']

  bookmarked boolean [not null, default: false, Note: '프로젝트 공용 즐겨찾기']

  created_at timestamp [not null, default: `now()`]
  updated_at timestamp [not null, default: `now()`]

  indexes {
    (project_id, name, email) [unique, Note: '프로젝트 내에서는 이름+이메일로 지원자를 구분']
  }

  Note: '지원자 필수 매핑 항목 + 단계별 심사 상태'
}


Table applicant_extra_answers {
  id int [primary key, increment]
  applicant_id int [not null]

  question_text varchar [not null, Note: '시트 헤더 외부 UI용']
  answer_text text [Note: '답변이 없음을 고려하여 Null 가']

  created_at timestamp [not null, default: `now()`]

  indexes {
    (applicant_id) [Note: 'FOR applicant_id -> 시트 결과 조회']
  }

  Note: '시트에서 고정 컬럼으로 매핑되지 않은 질문을 별도 테이블로 관리'
}


Table comments {
  id int [primary key, increment]

  applicant_id int [not null]
  author_id int [not null]

  parent_comment_id int [Note: '최초 댓글 ID. null -> 이 댓글이 최초 댓글']

  content text [not null]

  created_at timestamp [not null, default: `now()`]
  updated_at timestamp [Note: '댓글 수정 시 갱신']

  indexes {
    (applicant_id) [Note: 'FOR 특정 지원자에 대한 댓글 목록 조회']
    (parent_comment_id) [note: 'FOR 특정 댓글에 대한 답글 조회']
  }
}


Table notifications {
  id int [primary key, increment]
  receiver_id int [not null]
  project_id int [not null]

  type notification_type [not null, Note: '알람 원인']
  payload json [not null, Note: '알람 문구 작성을 위한 추가 데이터 관리'] 

  created_at timestamp [not null, default: `now()`, Note: 'FOR 시간 별로 정렬']
  read_at timestamp [Note: 'null -> 안 읽음']

  indexes {
    (receiver_id, created_at) [Note: 'FOR 항상 사용하는 조회를 빠르게']
  }

  Note: '개인 알림 관리'
}


// =====================================================
// 스마트 시간표
// =====================================================

Table interview_settings {
  id int [primary key, increment]
  project_id int [not null]

  start_time time [not null]
  end_time time [not null]

  // 슬롯 = duration + break
  slot_duration_min int [not null]
  slot_break_min int [not null, default: 0]

  max_applicants_per_slot int [not null]
  min_interviewers_per_slot int [not null]
  max_interviewers_per_slot int [not null]

  updated_at timestamp [not null, default: `now()`]

  indexes {
    (project_id) [unique]
  }

  Note: '면접 관련 설정'
}


Table interview_days {
  id int [primary key, increment]
  project_id int [not null]
  interview_date date [not null]

  indexes {
    (project_id, interview_date) [unique]
    (project_id)
  }

  Note: '면접 일자 복수 관리'
}


Table interview_required_interviewers {
  id int [primary key, increment]
  project_id int [not null]
  admin_id int [not null]

  required boolean [not null, default: false, Note: '필수 면접관']

  created_at timestamp [not null, default: `now()`]
  updated_at timestamp [not null, default: `now()`]

  indexes {
    (project_id, admin_id) [unique]
    (project_id)
  }

  Note: '필수 면접관 관리'
}


Table interviewer_availability_blocks {
  id int [primary key, increment]
  admin_id int [not null]
  interview_day_id int [not null]

  start_time time [not null, Note: 'end_time: +30m']

  created_at timestamp [not null, default: `now()`]

  indexes {
    (admin_id, interview_day_id, start_time) [unique]
    (interview_day_id, start_time)
  }

  Note: '면접관 30분 단위 가능 시간 후보'
}


Table interview_availability_investigation_links {
  id int [primary key, increment]
  project_id int [not null]

  token varchar [not null, unique]
  enabled boolean [not null, default: true]
  guidance_text text

  created_at timestamp [not null, default: `now()`]
  updated_at timestamp [not null, default: `now()`]

  indexes {
    (project_id) [unique]
  }

  Note: '지원자 면접 가능 시간 조사 링크'
}


Table interviewee_availability_slots {
  id int [primary key, increment]
  applicant_id int  [not null]
  interview_day_id int [not null]

  start_time time [not null, Note: 'end_time: +(slot_time)m']

  indexes {
    (applicant_id, interview_day_id) [unique]
    (interview_day_id, start_time) [unique]
  }

  Note: '제출된 지원자 면접 가능 슬롯'
}


Table interview_scheduled_slots {
  id int [primary key, increment]
  project_id int [not null]
  interview_day_id int [not null]

  start_time time [not null, Note: 'end_time: +(slot_time)m']

  indexes {
    (project_id, interview_day_id, start_time) [unique]
    (interview_day_id, start_time)
  }

  Note: '배정된 슬롯'
}


Table interview_scheduled_slot_applicants {
  id int [primary key, increment]
  schedule_slot_id int [not null]
  applicant_id int [not null]

  indexes {
    (schedule_slot_id, applicant_id) [unique]
    (applicant_id)
  }

  Note: '슬롯에 배정된 지원자'
}


Table interview_scheduled_slot_interviewers {
  id int [primary key, increment]
  schedule_slot_id int [not null]
  admin_id int [not null]

  indexes {
    (schedule_slot_id, admin_id) [unique]
  }

  Note: '슬롯에 배정된 면접관'
}


Table interview_schedule_unassigned_applicants {
  id int [primary key, increment]
  project_id int [not null]
  applicant_id int [not null]

  reason_text text [not null]

  indexes {
    (project_id, applicant_id) [unique]
  }

  Note: '자동 배정 실패한 지원자 및 사유'
}



// =====================================================
// REFERENCES
// =====================================================
Ref: projects.owner_id > users.id

Ref: project_admins.project_id > projects.id
Ref: project_admins.admin_id > users.id

Ref: project_message_templates.project_id > projects.id

Ref: google_oauth_tokens.owner_id > users.id

Ref: applicants.project_id > projects.id
Ref: applicant_extra_answers.applicant_id > applicants.id

Ref: comments.applicant_id > applicants.id
Ref: comments.author_id > users.id
Ref: comments.parent_comment_id > comments.id

Ref: notifications.receiver_id > users.id
Ref: notifications.project_id > projects.id

Ref: user_notification_settings.user_id > users.id

Ref: interview_settings.project_id > projects.id

Ref: interview_days.project_id > projects.id

Ref: interview_required_interviewers.project_id > projects.id
Ref: interview_required_interviewers.admin_id > users.id

Ref: interviewer_availability_blocks.admin_id > users.id
Ref: interviewer_availability_blocks.interview_day_id > interview_days.id

Ref: interview_availability_investigation_links.project_id > projects.id

Ref: interviewee_availability_slots.interview_day_id > interview_days.id
Ref: interviewee_availability_slots.applicant_id > applicants.id

Ref: interview_scheduled_slots.project_id > projects.id
Ref: interview_scheduled_slots.interview_day_id > interview_days.id

Ref: interview_scheduled_slot_applicants.schedule_slot_id > interview_scheduled_slots.id
Ref: interview_scheduled_slot_applicants.applicant_id > applicants.id

Ref: interview_scheduled_slot_interviewers.schedule_slot_id > interview_scheduled_slots.id
Ref: interview_scheduled_slot_interviewers.admin_id > users.id

Ref: interview_schedule_unassigned_applicants.project_id > projects.id
Ref: interview_schedule_unassigned_applicants.applicant_id > applicants.id
