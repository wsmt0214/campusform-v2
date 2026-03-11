## Campus Form Server

**Campus Form** 프로젝트의 백엔드 서버 레포지토리입니다.

---

### 🐳 Tech Stack 

| Category | Technology | Version |
| --- | --- | --- |
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.5.9 |
| **Build Tool** | Gradle | 8.x |
| **Database** | MySQL | 8.0 |
| **Infra** | AWS EC2, Docker | - |
| **CI/CD** | GitHub Actions | - |

---

### 🧳 사전 준비 사항 (Prerequisites) 

이 프로젝트를 실행하기 위해 아래 도구들이 설치되어 있어야 합니다.

* **JDK 17** 이상
* **Docker Desktop** (Docker Compose 포함)
* **IntelliJ IDEA** (권장)

---

### 📋 로컬 실행 가이드 (Getting Started)

아래 절차에 따라 로컬 개발 환경을 세팅해 주세요.

#### 1. 프로젝트 클론

```bash
git clone https://github.com/Konkuk-KUIT/CAMPUSFORM-Server.git
cd CAMPUSFORM-Server

```

#### 2. 환경 변수 설정 (.env)

프로젝트 **최상위 루트(`root`)** 경로에 `.env` 파일을 생성하고, 팀 노션을 참고하여 값을 채워주세요.

#### 3. DB 컨테이너 실행

로컬 DB는 Docker를 통해 실행합니다. (로컬 3306 포트 충돌 방지를 위해 **3307** 포트를 사용합니다.)

```bash
# 백그라운드 모드로 DB만 실행
docker-compose up -d db
```

* **확인:** `docker ps` 명령어로 `campus-mysql` 컨테이너가 떠 있는지 확인하세요.
* **접속 정보:**
* Host: `localhost`
* Port: `3307`
* User/PW: `.env`에 설정한 값

#### 4. Spring Boot 실행

IntelliJ에서 `CampusFormServerApplication`을 Run 하거나, 터미널에서 아래 명령어로 실행합니다.

```bash
./gradlew bootRun

```

---

### 🤝 협업 및 배포 전략 (GitHub Flow & CI/CD) 

#### Branch Strategy

* **`main`**: **유일한 기준 브랜치**입니다. 항상 배포 가능한 상태를 유지하며, 이곳에 Merge되면 **AWS EC2로 자동 배포**됩니다.
* **`feat/기능명`**: 개별 기능 개발 브랜치입니다. `main`에서 분기하여 작업 완료 후 `main`으로 PR을 보냅니다.

---

#### Work Flow 

1. 이슈 생성
2. `main` 브랜치에서 새로운 브랜치 생성 (`feat/기능명`)
3. 작업 완료 후 `main`으로 Pull Request (PR)
4. 코드 리뷰 및 Merge (1개 이상의 Approve 후 Merge → 자동 배포 트리거)

---

### ERD
<img width="3438" height="2416" alt="CampusForm 최종 ERD" src="https://github.com/user-attachments/assets/233b237b-68ac-4bd2-bce9-6609c29b47a9" />

---

### 아키텍처 다이어그램
<img width="697" height="398" alt="KakaoTalk_20260219_113319858" src="https://github.com/user-attachments/assets/7f35a691-9e30-4164-b31a-19bdbfc5edd7" />


