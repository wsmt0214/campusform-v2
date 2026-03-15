# CampusForm AWS EC2 CI/CD 구축 가이드

## 1. 문서 목적

이 문서는 현재 저장소 기준으로 CampusForm를 아래 구조로 운영 배포하는 방법을 정리한 문서다.

- AWS EC2 1대
- `app`, `db`, `nginx` 3개 컨테이너 분리
- MySQL은 Amazon RDS가 아니라 Docker 컨테이너로 직접 운영
- Nginx는 별도 Docker 컨테이너로 운영
- HTTPS는 Let's Encrypt + Certbot으로 적용
- CI/CD는 GitHub Actions 사용
- 이미지 레지스트리는 GHCR 사용
- GitHub 저장소가 새로 만들어진 상태도 포함

이 문서는 실제 작업 순서대로 따라갈 수 있게 작성했다.
각 단계는 아래 기준으로 설명한다.

- 목적: 이 단계에서 끝내야 하는 것
- 이유: 왜 이 구성이 필요한가
- 실행: 실제로 무엇을 입력하고 무엇을 눌러야 하는가
- 검증: 정상인지 어떻게 확인하는가
- 실패 시 확인: 막히면 어디를 먼저 봐야 하는가

## 2. 최종 권장 구조

### 구조 요약

- 브라우저 요청은 `api.campusform.kro.kr`로 들어온다.
- EC2의 `80`, `443` 포트는 Nginx 컨테이너만 받는다.
- Nginx가 HTTPS를 종료하고 `app:8080`으로 프록시한다.
- 앱은 `db:3306`으로 MySQL에 연결한다.
- GitHub Actions가 앱 이미지를 빌드해 GHCR에 push한다.
- EC2는 새 이미지를 pull해서 `docker compose`로 앱을 갱신한다.

### 실제 요청 흐름

```text
사용자 브라우저
  -> https://api.campusform.kro.kr
  -> EC2 443
  -> nginx 컨테이너
  -> app 컨테이너:8080
  -> db 컨테이너:3306
```

### 실제 배포 흐름

```text
main 브랜치 push
  -> GitHub Actions 실행
  -> 테스트
  -> Docker 이미지 빌드
  -> GHCR push
  -> deploy 자산을 EC2로 복사
  -> EC2 SSH 접속
  -> IMAGE_URI 최신값 반영
  -> docker compose pull app
  -> docker compose up -d --remove-orphans
```

## 3. 왜 이 구조를 선택하는가

### 3-1. 서버와 DB를 분리하는 이유

- 앱 재배포와 DB 데이터를 분리할 수 있다.
- 앱 컨테이너는 계속 교체해도 DB 데이터는 volume에 남는다.
- 장애 원인을 앱 계층과 DB 계층으로 나눠서 보기 쉬워진다.

### 3-2. Nginx를 별도 컨테이너로 두는 이유

- HTTPS 처리와 앱 런타임을 분리할 수 있다.
- TLS, 리다이렉트, 리버스 프록시를 Nginx에 집중시킬 수 있다.
- Spring Boot 앱은 API 처리만 맡고, 웹 게이트웨이는 Nginx가 맡는다.

### 3-3. GHCR를 쓰는 이유

- GitHub 저장소와 연동이 쉽다.
- GitHub Actions에서 바로 push할 수 있다.
- 무료로 시작하기 쉽다.

### 3-4. EC2 한 대로 가는 이유

- 현재 단계에서 비용이 가장 낮다.
- 구조가 단순해서 운영하기 쉽다.
- 문제가 생겨도 추적할 범위가 좁다.

### 3-5. 이 구조의 한계

- EC2 한 대가 장애 나면 앱과 DB가 같이 영향받는다.
- DB 백업과 복구를 직접 관리해야 한다.
- 인증서 갱신 상태를 직접 봐야 한다.

## 4. 현재 저장소에서 이 구조를 담당하는 파일

- `docker-compose.prod.yml`
  `app`, `db`, `nginx` 3개 컨테이너 정의

- `.github/workflows/deploy.yml`
  GitHub Actions 배포 파이프라인

- `deploy/.env.prod.example`
  운영 `.env` 예시

- `deploy/nginx/bootstrap.conf.template`
  인증서가 없을 때 쓰는 임시 HTTP 설정

- `deploy/nginx/https.conf.template`
  인증서 발급 후 쓰는 HTTPS 설정

- `deploy/nginx/default.conf.template`
  Nginx 컨테이너가 실제로 읽는 설정 파일

- `deploy/scripts/renew-certificate.sh`
  인증서 자동 갱신 스크립트

## 5. 이 문서에서 고정해서 쓰는 값

| 항목 | 값 |
| --- | --- |
| 루트 도메인 | `campusform.kro.kr` |
| API 도메인 | `api.campusform.kro.kr` |
| 프론트 도메인 | `web.campusform.kro.kr` |
| 작업 디렉터리 | `~/campus-project` |
| 앱 내부 포트 | `8080` |
| DB 내부 포트 | `3306` |
| 기본 브랜치 | `main` |

중요:

- 현재 워크플로는 `~/campus-project`를 기준으로 작성되어 있다.
- 다른 경로를 쓰려면 `.github/workflows/deploy.yml`도 같이 수정해야 한다.
- GitHub 저장소가 새로 만들어졌다면 `.github/workflows/deploy.yml`이 원격 `main` 브랜치에 올라가기 전까지 Actions 실행 이력은 비어 있는 것이 정상이다.

## 6. 전체 실행 순서

실제 작업은 아래 순서로 진행한다.

1. 도메인 구조 확정
2. EC2 생성
3. 보안 그룹 설정
4. EC2 기본 설정
5. Docker / Compose 설치
6. 운영 디렉터리 준비
7. 운영용 `.env` 작성
8. DNS 연결
9. 새 GitHub 저장소 생성
10. 로컬 프로젝트를 새 저장소에 push
11. GitHub Actions 활성화와 표시 조건 확인
12. GHCR 준비
13. GitHub Secrets 등록
14. GitHub Actions 1회 실행으로 앱 이미지 생성
15. EC2에 배포 자산이 들어왔는지 확인
16. Nginx bootstrap 설정 적용
17. `app + db + nginx` 1차 기동
18. Certbot으로 인증서 최초 발급
19. Nginx를 HTTPS 설정으로 전환
20. 인증서 자동 갱신 설정
21. Google OAuth 재설정
22. 정식 자동 배포 흐름 확인
23. 운영 검증
24. 백업/복구/장애 대응 준비

### 왜 이 순서인가

이 순서가 중요한 이유는 초기 한 번만 특수한 과정이 있기 때문이다.

1. GitHub 저장소가 새로 만들어진 상태에서는 Actions 이력이 비어 있다.
2. `.github/workflows/deploy.yml`이 원격 기본 브랜치에 올라가야 GitHub가 워크플로를 인식한다.
3. 첫 이미지가 GHCR에 만들어져야 `app` 컨테이너가 pull할 수 있다.
4. 첫 배포 시점에는 인증서가 없기 때문에 Nginx를 HTTPS 설정으로 바로 띄우면 실패할 수 있다.
5. 그래서 `새 GitHub repo 준비 -> 첫 Actions 실행 -> EC2 bootstrap 전환 -> Certbot 발급 -> HTTPS 복귀` 순서가 필요하다.

## 7. Step 1. 도메인 구조 확정

### 목적

쿠키, OAuth, CORS, HTTPS에 사용할 도메인을 확정한다.

### 이유

이 프로젝트는 로그인 세션, Google OAuth redirect URI, CORS 허용 origin, 쿠키 도메인이 모두 도메인 값에 묶여 있다.

### 실행

- API: `https://api.campusform.kro.kr`
- 프론트: `https://web.campusform.kro.kr`
- 쿠키 루트 도메인: `campusform.kro.kr`

### 검증

문서, `.env`, Google OAuth, 프론트 설정이 모두 위 값을 기준으로 맞는지 확인한다.

### 실패 시 확인

- 프론트가 실제로 `web.campusform.kro.kr`에서 서비스되는지 확인
- API를 다른 도메인으로 쓸 계획이 남아 있지 않은지 확인

## 8. Step 2. EC2 인스턴스 생성

### 목적

운영 서버를 준비한다.

### 이유

이 구조는 모든 운영 자산이 EC2 한 대에 올라간다.

### 실행

AWS Console에서 EC2 인스턴스를 생성한다.

권장값:

- OS: Ubuntu 22.04 LTS 또는 24.04 LTS
- 인스턴스 타입: `t3.small` 이상
- 스토리지: `gp3 30GB` 이상
- Key Pair: 새로 생성

### 왜 이 값을 추천하는가

- `t3.micro`는 `app + mysql + nginx`를 함께 돌리기에 메모리가 빠듯할 수 있다.
- MySQL 데이터, Docker 이미지, 로그가 쌓이므로 디스크가 너무 작으면 곧 막힌다.

### 검증

- EC2 Public IP
- SSH 접속용 key pair
- EC2 보안 그룹 ID

### 실패 시 확인

- Ubuntu AMI로 생성했는지 확인
- key pair를 실제로 내려받았는지 확인

## 9. Step 3. 보안 그룹 설정

### 목적

외부에 열 포트와 내부에만 둘 포트를 분리한다.

### 이유

외부에 보여야 하는 것은 Nginx의 `80`, `443`뿐이다.
MySQL `3306`과 앱 `8080`은 외부에 열면 안 된다.

### 실행

보안 그룹 Inbound 규칙:

- `22`: 본인 고정 IP만 허용
- `80`: `0.0.0.0/0`
- `443`: `0.0.0.0/0`

절대 열지 말 것:

- `3306`
- `8080`

### 검증

보안 그룹 화면에서 위 규칙만 열려 있는지 확인한다.

### 실패 시 확인

- `22`를 너무 좁게 잡아 SSH가 막히지 않았는지 확인
- `3306`, `8080`이 실수로 열리지 않았는지 확인

## 10. Step 4. EC2 기본 설정

### 목적

서버 패키지와 시간대를 정리한다.

### 이유

초기 업데이트와 시간대 설정을 하지 않으면 로그 시간과 cron 시간이 꼬이기 쉽다.

### 실행

로컬에서 EC2 접속:

```bash
ssh -i /path/to/key.pem ubuntu@<EC2_PUBLIC_IP>
```

EC2에서 실행:

```bash
sudo apt update
sudo apt upgrade -y
sudo timedatectl set-timezone Asia/Seoul
```

### 검증

```bash
timedatectl
```

### 실패 시 확인

- SSH 접속이 안 되면 보안 그룹 22번 규칙 확인
- 기본 사용자가 `ubuntu`인지 확인

## 11. Step 5. Docker / Compose 설치

### 목적

컨테이너 운영 환경을 준비한다.

### 이유

현재 운영 환경 전체는 `docker compose`로 돌아간다.

### 실행

```bash
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
```

선택:

```bash
sudo usermod -aG docker $USER
exit
```

### 검증

```bash
docker --version
docker compose version
sudo systemctl status docker
```

### 실패 시 확인

- Docker 저장소가 정상 등록되었는지 확인
- Ubuntu 버전이 너무 오래되지 않았는지 확인

## 12. Step 6. 운영 디렉터리 준비

### 목적

EC2 안에서 배포 자산을 둘 기준 경로를 만든다.

### 이유

현재 워크플로는 `~/campus-project`에 배포 자산을 복사하고 그 안에서 `docker compose`를 실행한다.

### 실행

```bash
mkdir -p ~/campus-project
mkdir -p ~/campus-project/certbot/conf
mkdir -p ~/campus-project/certbot/www
mkdir -p ~/campus-project/deploy/nginx
cd ~/campus-project
```

### 검증

```bash
ls -al ~/campus-project
ls -al ~/campus-project/certbot
```

### 실패 시 확인

- `~/campusform-v2` 같은 다른 경로를 쓰고 있지 않은지 확인
- 다른 경로를 꼭 써야 하면 workflow도 같이 수정해야 한다

## 13. Step 7. 운영용 `.env` 작성

### 목적

앱, DB, OAuth, 쿠키, CORS, S3 값을 운영 환경 변수 파일에 모은다.

### 이유

현재 `docker-compose.prod.yml`은 대부분의 설정을 `.env`에서 읽는다.
특히 새 저장소에서 첫 GitHub Actions 배포를 할 때도 EC2에 `.env`가 미리 있어야 한다.
`.env`가 없거나 `MYSQL_ROOT_PASS`가 비어 있으면 MySQL 컨테이너는 바로 종료된다.

### 실행

EC2에서 `.env` 파일 생성:

```bash
cd ~/campus-project
nano .env
```

예시:

```dotenv
API_DOMAIN=api.campusform.kro.kr
IMAGE_URI=ghcr.io/YOUR_GITHUB_OWNER/campusform-server:latest

MYSQL_ROOT_PASS=CHANGE_THIS_ROOT_PASSWORD
DB_NAME=campusform
MYSQL_USER_NAME=campusform
MYSQL_USER_PASS=CHANGE_THIS_APP_PASSWORD

GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET

FRONTEND_URL=https://web.campusform.kro.kr
APP_OAUTH2_LOGIN_REDIRECT_URI=https://web.campusform.kro.kr/auth/callback
APP_OAUTH2_SHEETS_REDIRECT_URI=https://web.campusform.kro.kr/oauth/google/callback
APP_OAUTH2_ALLOWED_REDIRECT_URIS=https://web.campusform.kro.kr

CORS_ALLOWED_ORIGINS=https://web.campusform.kro.kr

COOKIE_DOMAIN=campusform.kro.kr
COOKIE_SECURE=true
COOKIE_SAME_SITE=none

SERVER_SERVLET_SESSION_COOKIE_DOMAIN=campusform.kro.kr
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=none

AWS_S3_BUCKET=YOUR_S3_BUCKET
AWS_S3_REGION=ap-northeast-2
AWS_ACCESS_KEY=YOUR_S3_ACCESS_KEY
AWS_SECRET_KEY=YOUR_S3_SECRET_KEY
```

반드시 바꿔야 하는 값:

- `YOUR_GITHUB_OWNER`
- MySQL 비밀번호 2개
- Google Client ID / Secret
- S3 관련 값

MySQL 관련 키 이름은 아래처럼 정확히 맞아야 한다.

- `MYSQL_ROOT_PASS`
- `DB_NAME`
- `MYSQL_USER_NAME`
- `MYSQL_USER_PASS`

### 검증

```bash
grep -E "^(API_DOMAIN|IMAGE_URI|DB_NAME|MYSQL_USER_NAME|FRONTEND_URL|COOKIE_DOMAIN)=" .env
```

### 실패 시 확인

- `=` 앞뒤 공백 확인
- `https://` 누락 여부 확인
- `MYSQL_ROOT_PASSWORD`가 아니라 `MYSQL_ROOT_PASS`를 썼는지 확인
- Windows에서 만든 파일이면 줄바꿈이 `CRLF`거나 BOM이 들어가지 않았는지 확인

## 14. Step 8. DNS 연결

### 목적

도메인이 EC2를 가리키게 만든다.

### 이유

Let's Encrypt 인증서 발급은 도메인 검증 기반이다.

### 실행

- `api.campusform.kro.kr` -> EC2 Public IP
- `web.campusform.kro.kr` -> 프론트 서비스 위치

### 검증

```bash
nslookup api.campusform.kro.kr
ping api.campusform.kro.kr
```

### 실패 시 확인

- A 레코드 IP가 EC2 공인 IP와 같은지 확인
- DNS 전파 시간이 충분했는지 확인

## 15. Step 9. 새 GitHub 저장소 생성

### 목적

이 프로젝트의 원격 저장소를 새로 만든다.

### 이유

GitHub Actions, GHCR, Secrets, Packages는 모두 GitHub 저장소 단위로 연결된다.
즉, 새 저장소를 먼저 만들어야 이후 단계가 의미가 있다.

### 실행

GitHub에서 새 repository 생성:

- Repository name: 원하는 이름
- Visibility: private 권장
- Default branch: `main` 기준으로 운영

중요:

- 로컬 프로젝트에 이미 Git 이력이 있다면 GitHub에서 `README`, `.gitignore`, `license`를 넣고 시작하지 않는 편이 단순하다.
- 빈 저장소로 만드는 편이 최초 push 때 충돌이 없다.

### 검증

아래 형태의 빈 저장소 URL이 준비되어 있어야 한다.

```text
https://github.com/<OWNER>/<REPO>.git
```

### 실패 시 확인

- GitHub에서 저장소를 다른 계정으로 만들지 않았는지 확인
- 실수로 템플릿 저장소나 README 초기화를 켜지 않았는지 확인

## 16. Step 10. 로컬 프로젝트를 새 저장소에 push

### 목적

현재 로컬 프로젝트를 새 GitHub 저장소에 연결하고 기본 브랜치에 올린다.

### 이유

GitHub Actions가 보이려면 `.github/workflows/deploy.yml`이 원격 저장소의 기본 브랜치에 실제로 올라가 있어야 한다.

### 실행

현재 원격 상태 확인:

```bash
git remote -v
git branch --show-current
```

기존 `origin`이 없다면:

```bash
git remote add origin https://github.com/<OWNER>/<REPO>.git
git branch -M main
git push -u origin main
```

기존 `origin`이 다른 저장소를 가리키면:

```bash
git remote set-url origin https://github.com/<OWNER>/<REPO>.git
git branch -M main
git push -u origin main
```

만약 아직 로컬 커밋이 없다면 먼저 커밋:

```bash
git add .
git commit -m "chore: initialize repository"
git push -u origin main
```

### 이 단계에서 중요한 점

- `.github/workflows/deploy.yml`이 같이 push되어야 한다.
- 새 저장소라서 Actions 탭이 비어 있는 것은 push 전까지는 정상이다.

### 검증

GitHub 저장소 파일 목록에서 아래가 보이는지 확인한다.

- `.github/workflows/deploy.yml`
- `docker-compose.prod.yml`
- `deploy/`

### 실패 시 확인

- 현재 브랜치가 `main`인지 확인
- push한 원격 저장소가 새 저장소가 맞는지 확인
- `.github/workflows/` 폴더가 `.gitignore`에 걸리지 않는지 확인

## 17. Step 11. GitHub Actions 활성화와 표시 조건 확인

### 목적

새 GitHub 저장소에서 Actions가 왜 안 보이는지 이해하고, 실제로 동작 가능한 상태로 만든다.

### 이유

새 저장소를 만들면 Actions 실행 이력이 비어 있는 것이 정상이다.
또한 조직이나 저장소 설정에 따라 Actions가 비활성화되어 있을 수도 있다.

### 먼저 알아둘 핵심

- Actions 탭이 비어 있는 것과 Actions가 고장 난 것은 다르다.
- `.github/workflows/deploy.yml`이 기본 브랜치에 올라가기 전에는 실행할 워크플로 자체가 없다.
- workflow 파일이 올라간 뒤에도 저장소 설정에서 Actions가 막혀 있으면 실행되지 않는다.

### 실행

GitHub 저장소에서 아래를 확인한다.

1. `Actions` 탭으로 이동
2. 워크플로 목록이 비어 있더라도 먼저 `main` 브랜치에 workflow 파일이 올라갔는지 확인
3. `Settings -> Actions -> General` 이동
4. `Actions permissions`에서 Actions 사용이 허용되어 있는지 확인
5. 외부 action을 쓰므로 `Allow all actions and reusable workflows`를 권장
6. `Workflow permissions`는 `Read and write permissions`로 두는 편이 단순하다
7. 저장 후 다시 `Actions` 탭 확인

### 왜 `Read and write permissions`를 권장하는가

현재 workflow는 GHCR push에 `packages: write` 권한을 사용한다.
workflow 파일 안에도 권한을 명시해 두었지만, 새 저장소 세팅이 너무 보수적이면 처음부터 막힐 수 있다.

### 검증

아래 둘 중 하나가 보여야 한다.

- `Deploy To EC2` workflow 항목
- 혹은 `Run workflow` 버튼

### 실패 시 확인

- `.github/workflows/deploy.yml`이 원격 `main`에 실제로 올라갔는지 확인
- 저장소나 조직에서 Actions가 차단되어 있지 않은지 확인
- workflow 파일 경로가 정확히 `.github/workflows/deploy.yml`인지 확인

## 18. Step 12. GHCR 준비

### 목적

앱 이미지를 저장할 레지스트리를 준비한다.

### 이유

현재 구조는 EC2에서 소스를 직접 빌드하지 않는다.
CI가 이미지를 만들고 EC2는 그 이미지를 pull한다.

### 실행

현재 workflow가 push하는 이미지 형식:

```text
ghcr.io/<GITHUB_OWNER>/campusform-server:latest
ghcr.io/<GITHUB_OWNER>/campusform-server:<git-sha>
```

해야 할 일:

1. GitHub 저장소의 패키지 사용 가능 여부 확인
2. GHCR pull용 GitHub 계정 결정
3. `read:packages` 권한이 있는 PAT 생성

권장:

- 배포 전용 PAT 1개 생성
- 최소 권한만 부여

### 새 저장소에서 자주 헷갈리는 점

- 아직 첫 이미지 push 전이라면 Packages 탭이 비어 있어도 정상이다.
- 첫 workflow가 성공해서 이미지가 push된 뒤에 GHCR 패키지가 보이는 경우가 많다.

### 검증

GitHub의 Packages 화면을 열 수 있는지 확인한다.

### 실패 시 확인

- private 패키지 접근 권한 확인
- PAT 권한에 `read:packages` 포함 여부 확인

## 19. Step 13. GitHub Secrets 등록

### 목적

GitHub Actions가 EC2와 GHCR에 접근할 수 있게 한다.

### 이유

현재 `.github/workflows/deploy.yml`은 아래 secret이 없으면 동작하지 않는다.

- `EC2_HOST`
- `EC2_USERNAME`
- `EC2_SSH_KEY`
- `GHCR_USERNAME`
- `GHCR_PAT`

### 실행

GitHub 저장소에서:

`Settings -> Secrets and variables -> Actions -> New repository secret`

등록할 값:

- `EC2_HOST`
  EC2 Public IP 또는 Public DNS

- `EC2_USERNAME`
  보통 `ubuntu`

- `EC2_SSH_KEY`
  PEM 파일 전체 내용

- `GHCR_USERNAME`
  GHCR pull 가능한 GitHub 사용자명

- `GHCR_PAT`
  `read:packages` 권한 포함 PAT

### 검증

Secrets 목록에 위 5개가 모두 보이는지 확인한다.

### 실패 시 확인

- PEM 시작/끝 줄 포함 여부 확인
- SSH key 내용 전체를 넣었는지 확인

## 20. Step 14. GitHub Actions 1회 실행으로 앱 이미지 생성

### 목적

최초 앱 이미지를 GHCR에 만들고, 배포 자산을 EC2에 넣는다.

### 이유

`app` 서비스는 `IMAGE_URI` 이미지를 pull해서 뜬다.
즉, 최초 한 번은 GitHub Actions가 먼저 돌아서 이미지를 만들어야 한다.

### 실행

방법 A. `main`에 push:

```bash
git add .
git commit -m "chore: prepare production deploy"
git push origin main
```

방법 B. GitHub에서 수동 실행:

`Actions -> Deploy To EC2 -> Run workflow`

### 이 단계에서 실제로 일어나는 일

1. 테스트 실행
2. 앱 이미지 빌드
3. GHCR push
4. `docker-compose.prod.yml`, `deploy/`를 EC2로 복사
5. EC2에서 `docker compose pull app`
6. EC2에서 `docker compose up -d --remove-orphans`

### 새 저장소 기준으로 꼭 알아둘 점

- 이 시점까지 Actions 탭에 아무 것도 없었다가, 첫 push 후에 워크플로가 나타나는 것이 정상이다.
- 첫 Actions 실행 전에 EC2 `~/campus-project/.env`를 반드시 먼저 만들어 둬야 한다.
- 첫 배포에서는 `campus-nginx`가 인증서가 없어서 제대로 안 뜰 수 있다.
- 이건 다음 단계에서 bootstrap 설정으로 바로 잡으면 된다.

### 검증

Actions 로그에서 아래 순서가 보이는지 확인한다.

- `Run tests`
- `Build application`
- `Build and push Docker image`
- `Copy deploy assets to EC2`
- `Deploy to EC2`

### 실패 시 확인

- `GHCR_PAT` 권한 확인
- `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY` 확인
- Actions 설정이 막혀 있지 않은지 다시 확인

## 21. Step 15. EC2에 배포 자산이 들어왔는지 확인

### 목적

EC2 안에 실제 배포 파일이 들어왔는지 확인한다.

### 이유

bootstrap 전환, Certbot 발급, 자동 갱신은 모두 EC2 안의 배포 자산을 기준으로 진행한다.

### 실행

```bash
cd ~/campus-project
ls -al
ls -al deploy
ls -al deploy/nginx
```

최소 확인 파일:

- `docker-compose.prod.yml`
- `deploy/nginx/bootstrap.conf.template`
- `deploy/nginx/https.conf.template`
- `deploy/nginx/default.conf.template`
- `deploy/scripts/renew-certificate.sh`

파일이 없으면 수동 복사:

```bash
scp -i /path/to/key.pem docker-compose.prod.yml ubuntu@<EC2_PUBLIC_IP>:~/campus-project/
scp -i /path/to/key.pem -r deploy ubuntu@<EC2_PUBLIC_IP>:~/campus-project/
```

### 검증

위 파일들이 보이면 다음 단계로 넘어간다.

### 실패 시 확인

- workflow가 `Copy deploy assets to EC2`까지 성공했는지 확인
- EC2 경로가 `~/campus-project`가 맞는지 확인

## 22. Step 16. Nginx bootstrap 설정 적용

### 목적

인증서가 없는 상태에서도 Nginx를 먼저 띄울 수 있게 한다.

### 이유

Let's Encrypt는 `/.well-known/acme-challenge/` 경로를 HTTP로 검증한다.
인증서가 없는 첫 배포에서는 bootstrap 설정이 먼저 필요하다.

### 실행

```bash
cd ~/campus-project
cp deploy/nginx/bootstrap.conf.template deploy/nginx/default.conf.template
```

### 이 단계에서 바뀌는 것

- Nginx는 `80` 포트 기준으로 뜬다
- `/.well-known/acme-challenge/` 경로를 열 수 있다
- 인증서가 없어도 시작 가능한 상태가 된다

### 검증

```bash
cat ~/campus-project/deploy/nginx/default.conf.template
```

### 실패 시 확인

- 실제로 바꿔야 하는 파일은 `default.conf.template`이다
- `bootstrap.conf.template`만 보고 끝내지 않았는지 확인

## 23. Step 17. `app + db + nginx` 1차 기동

### 목적

부트스트랩 상태로 운영 컨테이너가 뜨는지 확인한다.

### 이유

인증서 발급 전에 Nginx와 앱과 DB가 기본적으로 살아 있어야 한다.

### 실행

```bash
cd ~/campus-project
sudo docker compose -f docker-compose.prod.yml up -d
```

상태 확인:

```bash
sudo docker compose -f docker-compose.prod.yml ps
```

로그 확인:

```bash
sudo docker logs campus-mysql --tail 100
sudo docker logs campus-server --tail 100
sudo docker logs campus-nginx --tail 100
```

### 검증

- `campus-mysql`가 `healthy` 상태인지 확인
- `campus-server`가 종료되지 않는지 확인
- `campus-nginx`가 종료되지 않는지 확인

### 실패 시 확인

- `.env` 값 누락 여부 확인
- `IMAGE_URI`가 실제 GHCR 이미지인지 확인
- `default.conf.template`이 bootstrap 상태인지 확인

## 24. Step 18. Certbot으로 인증서 최초 발급

### 목적

`api.campusform.kro.kr`용 HTTPS 인증서를 발급한다.

### 이유

운영 환경은 HTTPS가 기본이다.

### 실행

```bash
cd ~/campus-project
sudo docker run --rm \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d api.campusform.kro.kr \
  --email YOUR_EMAIL \
  --agree-tos \
  --no-eff-email
```

### 검증

```bash
ls -al ~/campus-project/certbot/conf/live/api.campusform.kro.kr
```

보여야 하는 파일:

- `fullchain.pem`
- `privkey.pem`

### 실패 시 확인

- DNS가 EC2 IP를 가리키는지 확인
- 보안 그룹에서 `80` 포트가 열려 있는지 확인
- `campus-nginx`가 떠 있는지 확인

## 25. Step 19. Nginx를 HTTPS 설정으로 전환

### 목적

발급한 인증서를 실제 서비스에 적용한다.

### 이유

인증서가 있어도 Nginx 설정이 HTTPS로 바뀌지 않으면 서비스에는 반영되지 않는다.

### 실행

```bash
cd ~/campus-project
cp deploy/nginx/https.conf.template deploy/nginx/default.conf.template
sudo docker compose -f docker-compose.prod.yml up -d nginx
sudo docker exec campus-nginx nginx -s reload
```

### 검증

```bash
curl -I https://api.campusform.kro.kr
```

### 실패 시 확인

- 인증서 파일 존재 여부 확인
- `default.conf.template`이 HTTPS 내용인지 확인
- Nginx 로그 확인

## 26. Step 20. 인증서 자동 갱신 설정

### 목적

인증서 만료 전에 자동 갱신되게 만든다.

### 이유

Let's Encrypt 인증서는 자동 갱신이 없으면 운영 중 만료될 수 있다.

### 실행

권한 부여:

```bash
cd ~/campus-project
chmod +x deploy/scripts/renew-certificate.sh
```

수동 테스트:

```bash
./deploy/scripts/renew-certificate.sh ~/campus-project
```

cron 등록:

```bash
crontab -e
```

추가할 내용:

```cron
0 3 * * * /bin/sh /home/ubuntu/campus-project/deploy/scripts/renew-certificate.sh /home/ubuntu/campus-project >> /home/ubuntu/campus-project/certbot-renew.log 2>&1
```

### 검증

```bash
crontab -l
tail -n 50 ~/campus-project/certbot-renew.log
```

### 실패 시 확인

- EC2 사용자가 `ubuntu`인지 확인
- 경로가 `/home/ubuntu/campus-project`인지 확인

## 27. Step 21. Google OAuth 재설정

### 목적

운영 도메인 기준으로 Google 로그인과 Google Sheets 연동을 다시 맞춘다.

### 이유

이 프로젝트는 Google OAuth를 2가지 용도로 사용한다.

- 사용자 로그인
- Google Sheets 연동 토큰 발급

### 27-1. 로그인 OAuth 흐름

```text
브라우저
  -> https://api.campusform.kro.kr/oauth2/authorization/google
  -> Google 로그인
  -> https://api.campusform.kro.kr/login/oauth2/code/google
  -> 백엔드가 세션 처리
  -> https://web.campusform.kro.kr/auth/callback
```

핵심:

- Google이 직접 호출하는 로그인 콜백은 백엔드 주소다
- 프론트의 `/auth/callback`은 Google이 직접 부르는 주소가 아니다

### 27-2. Google Sheets OAuth 흐름

```text
프론트
  -> 백엔드에 authorize URL 요청
  -> Google 승인 페이지 이동
  -> https://web.campusform.kro.kr/oauth/google/callback
  -> 프론트가 code를 백엔드로 전달
  -> 백엔드가 token 교환
```

### 27-3. Google Console에서 할 일

1. `APIs & Services -> OAuth consent screen`
2. `APIs & Services -> Library`
3. `Google Sheets API` 활성화
4. `APIs & Services -> Credentials`
5. `Create Credentials -> OAuth client ID`

### 27-4. OAuth client 실제 입력값

Authorized JavaScript origins:

```text
https://web.campusform.kro.kr
```

Authorized redirect URIs:

```text
https://api.campusform.kro.kr/login/oauth2/code/google
https://web.campusform.kro.kr/oauth/google/callback
```

주의:

- origin에는 path를 넣지 않는다
- `/auth/callback`은 Google redirect URI가 아니다

### 27-5. EC2 `.env` 반영값

```dotenv
GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET

FRONTEND_URL=https://web.campusform.kro.kr
APP_OAUTH2_LOGIN_REDIRECT_URI=https://web.campusform.kro.kr/auth/callback
APP_OAUTH2_SHEETS_REDIRECT_URI=https://web.campusform.kro.kr/oauth/google/callback
APP_OAUTH2_ALLOWED_REDIRECT_URIS=https://web.campusform.kro.kr

CORS_ALLOWED_ORIGINS=https://web.campusform.kro.kr

COOKIE_DOMAIN=campusform.kro.kr
COOKIE_SECURE=true
COOKIE_SAME_SITE=none

SERVER_SERVLET_SESSION_COOKIE_DOMAIN=campusform.kro.kr
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SERVER_SERVLET_SESSION_COOKIE_SAME_SITE=none
```

반영 후 재기동:

```bash
cd ~/campus-project
nano .env
sudo docker compose -f docker-compose.prod.yml up -d
```

### 27-6. 로그인 테스트

```text
https://api.campusform.kro.kr/oauth2/authorization/google?redirect_uri=https://web.campusform.kro.kr/auth/callback
```

### 27-7. Google Sheets 연동 테스트

1. 백엔드가 authorize URL을 만든다
2. 사용자가 Google 승인한다
3. Google이 `https://web.campusform.kro.kr/oauth/google/callback`으로 보낸다
4. 프론트가 받은 `code`를 백엔드에 전달한다
5. 백엔드가 token 교환에 성공한다

### 27-8. 실패 시 가장 먼저 볼 것

- `http`와 `https`를 섞지 않았는지 확인
- `api`와 `web` 도메인을 뒤집지 않았는지 확인
- `.env` 수정 후 재기동했는지 확인

## 28. Step 22. 정식 자동 배포 흐름 확인

### 목적

bootstrap이 끝난 뒤 자동 배포가 정상적으로 도는지 확인한다.

### 이유

최초 1회와 이후 배포는 성격이 다르다.

### 현재 워크플로의 실제 동작 순서

1. `main` 브랜치에 push
2. GitHub Actions가 checkout
3. JDK 17 세팅
4. `./gradlew test`
5. `./gradlew bootJar`
6. Docker 이미지 빌드
7. GHCR에 `latest`, `git sha` 태그로 push
8. `docker-compose.prod.yml`, `deploy/`를 EC2로 복사
9. EC2에 SSH 접속
10. `.env`의 `IMAGE_URI` 최신화
11. `docker compose pull app`
12. `docker compose up -d --remove-orphans`
13. 사용하지 않는 이미지 정리

### 검증

최초 bootstrap 이후에는 `main` push만으로 배포가 끝나는지 확인한다.

### 실패 시 확인

- GHCR 로그인 실패 여부
- EC2 SSH 접속 실패 여부
- `.env` 누락 여부

## 29. Step 23. 운영 검증

### 목적

서비스가 실제 운영 가능한 상태인지 최종 점검한다.

### 이유

컨테이너가 떠 있는 것과 서비스가 정상인 것은 다르다.

### 점검 목록

1. `https://api.campusform.kro.kr` 접속 가능
2. 인증서 경고 없음
3. `campus-nginx`가 재시작 반복 없이 유지됨
4. `campus-server`가 재시작 반복 없이 유지됨
5. `campus-mysql`가 `healthy` 상태
6. Google 로그인 동작
7. Google Sheets 연동 동작

### 검증 명령

```bash
cd ~/campus-project
sudo docker compose -f docker-compose.prod.yml ps
curl -I https://api.campusform.kro.kr
sudo docker logs campus-server --tail 200
sudo docker logs campus-nginx --tail 200
sudo docker logs campus-mysql --tail 200
```

## 30. Step 24. 백업/복구/장애 대응 준비

### 목적

DB 유실과 운영 장애에 대비한다.

### 이유

지금 구조는 MySQL을 직접 Docker로 운영하므로 백업과 복구도 직접 챙겨야 한다.

### 최소 백업 전략

```bash
mkdir -p ~/campus-project/backup
sudo docker exec campus-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" campusform' > ~/campus-project/backup/campusform-$(date +%F).sql
```

권장:

- 최소 7일치 보관
- 배포 전 수동 백업 추가
- 가능하면 다른 스토리지에 1부 더 보관

### 자주 쓰는 운영 명령

상태 확인:

```bash
cd ~/campus-project
sudo docker compose -f docker-compose.prod.yml ps
```

전체 재기동:

```bash
cd ~/campus-project
sudo docker compose -f docker-compose.prod.yml up -d
```

앱 로그:

```bash
sudo docker logs campus-server --tail 200
```

DB 로그:

```bash
sudo docker logs campus-mysql --tail 200
```

Nginx 로그:

```bash
sudo docker logs campus-nginx --tail 200
```

앱만 새 이미지로 갱신:

```bash
cd ~/campus-project
sudo docker compose -f docker-compose.prod.yml pull app
sudo docker compose -f docker-compose.prod.yml up -d app
```

### 장애 대응 포인트

#### HTTPS가 안 붙는다

- 인증서 파일 존재 여부 확인
- Nginx 설정이 bootstrap인지 HTTPS인지 확인
- `80`, `443` 보안 그룹 확인
- DNS A 레코드 확인

#### 브라우저에서 502 또는 504

- `campus-server` 로그 확인
- DB 연결 성공 여부 확인
- 앱 컨테이너 재시작 반복 여부 확인

#### DB 연결 실패

- `campus-mysql` 상태 확인
- `.env`의 DB 계정과 비밀번호 확인
- `DB_URL`이 `db:3306`을 가리키는지 확인

#### GHCR pull 실패

- `GHCR_USERNAME` 확인
- `GHCR_PAT` 확인
- 패키지 접근 권한 확인
- 이미지 경로 오타 확인

## 31. 구조 요약

### 장점

- 비용이 낮다
- 구조가 단순하다
- 앱, DB, 프록시가 역할별로 분리된다
- HTTPS와 배포 흐름이 명확하다

### 단점

- 운영 책임을 직접 져야 한다
- EC2 단일 장애 지점이 있다
- DB 백업을 직접 챙겨야 한다

## 32. 공식 문서 참고

- AWS EC2 Key Pairs  
  https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/create-key-pairs.html

- AWS Security Groups  
  https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/creating-security-group.html

- Docker Engine on Ubuntu  
  https://docs.docker.com/engine/install/ubuntu/

- GitHub Actions workflow syntax  
  https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax

- GitHub Actions secrets  
  https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets

- Google OAuth Web Server Apps  
  https://developers.google.com/identity/protocols/oauth2/web-server

## 33. 최종 결론

현재 이 프로젝트의 가장 현실적인 구조는 아래와 같다.

- EC2 1대
- `app`, `db`, `nginx` 3컨테이너 분리
- MySQL 직접 Docker 운영
- Nginx 별도 Docker 운영
- GHCR를 통한 이미지 저장
- GitHub Actions 자동 배포

새 GitHub 저장소에서 시작하더라도, `workflow 파일을 main에 올리고 -> Actions를 허용하고 -> Secrets를 넣고 -> 첫 이미지를 만든 뒤 -> bootstrap/HTTPS 전환` 순서로 가면 운영 가능한 상태까지 연결할 수 있다.
