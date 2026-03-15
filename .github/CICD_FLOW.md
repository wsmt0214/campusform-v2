
## 3. 파일끼리 어떻게 협력하는가

### 3.1 배포 파이프라인 기준 협력 관계

1. `.github/workflows/deploy.yml`
   - 시작점이다.
   - `Dockerfile`을 사용해 이미지를 빌드한다.
   - `docker-compose.prod.yml`과 `deploy/` 디렉터리를 EC2로 복사한다.
   - EC2에 SSH로 접속해 `docker compose` 명령을 실행한다.

2. `Dockerfile`
   - Spring Boot 애플리케이션을 컨테이너 이미지로 패키징한다.
   - 최종 결과물은 GHCR에 올라가고, EC2는 소스코드 대신 이 이미지를 내려받아 실행한다.

3. `docker-compose.prod.yml`
   - EC2에서 실제로 어떤 컨테이너를 어떤 순서로 띄울지 결정한다.
   - `.env` 값을 읽어 `db`, `app`, `nginx`에 환경변수를 주입한다.

4. `deploy/nginx/*.template`
   - `nginx` 컨테이너가 시작될 때 설정 파일 원본으로 사용된다.
   - `API_DOMAIN` 값이 템플릿에 주입되어 실제 Nginx 설정이 생성된다.

5. `deploy/scripts/renew-certificate.sh`
   - `certbot` 컨테이너와 `nginx` 컨테이너를 연결하는 운영 스크립트다.
   - 같은 인증서 디렉터리를 공유하므로, 갱신 후 `nginx reload`만 하면 새 인증서가 반영된다.

6. `src/main/resources/application.yml`
   - `docker-compose.prod.yml`이 주입한 환경변수를 Spring Boot 설정으로 연결한다.
   - 즉 `.env -> docker-compose.prod.yml -> application.yml -> 런타임 동작` 흐름의 중간 지점이다.

### 3.2 환경변수 기준 협력 관계

```text
EC2의 ~/.env
  -> docker-compose.prod.yml
     -> db 컨테이너 초기화용 MYSQL_* 변수
     -> app 컨테이너용 DB/OAuth/CORS/Cookie/S3 변수
     -> nginx 컨테이너용 API_DOMAIN 변수
        -> application.yml / SecurityConfig / CookieOAuth2AuthorizationRequestRepository
        -> deploy/nginx/default.conf.template
```

대표적인 연결은 아래와 같다.

| 환경변수 | 어디서 사용되는가 | 실제 영향 |
| --- | --- | --- |
| `IMAGE_URI` | `docker-compose.prod.yml` | app 컨테이너가 어떤 GHCR 이미지를 띄울지 결정 |
| `MYSQL_ROOT_PASS`, `DB_NAME`, `MYSQL_USER_NAME`, `MYSQL_USER_PASS` | `db`, `app` | MySQL 초기화와 Spring datasource 연결에 동시에 사용 |
| `API_DOMAIN` | `nginx` 템플릿 | Nginx `server_name`, 인증서 경로, HTTPS 라우팅 기준 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `application.yml` | prod 프로필 DB 연결 |
| `CORS_ALLOWED_ORIGINS` | `SecurityConfig.java` | 프론트 도메인만 CORS 허용 |
| `COOKIE_DOMAIN`, `COOKIE_SECURE`, `COOKIE_SAME_SITE` | OAuth 쿠키 저장소 | 서브도메인 간 OAuth 쿠키 공유 정책 |
| `SERVER_SERVLET_SESSION_COOKIE_*` | `application.yml` | 세션 쿠키 도메인, secure, same-site 설정 |

## 4. EC2 디렉터리 구조

사용자가 알려준 현재 경로는 아래와 같다.

```bash
ubuntu@ip-172-31-6-186:~/campusform-v2$ ls
certbot  deploy  docker-compose.prod.yml
```

실제로 운영에 필요한 구조를 포함하면 아래처럼 보는 것이 정확하다.

```text
~/campusform-v2
├─ .env
│  -> 숨김 파일이라 ls 결과에 안 보일 수 있음
│  -> 운영 비밀값, DB 계정, OAuth 값, S3 값, IMAGE_URI 저장
├─ docker-compose.prod.yml
│  -> 운영 컨테이너 정의
├─ deploy/
│  ├─ nginx/
│  │  ├─ default.conf.template
│  │  └─ bootstrap.conf.template
│  └─ scripts/
│     └─ renew-certificate.sh
├─ certbot/
│  ├─ conf/
│  │  -> Let's Encrypt 인증서 실파일 보관
│  │  -> live/, archive/, renewal/ 등이 들어감
│  └─ www/
│     -> ACME challenge 파일을 내려주는 webroot
└─ certbot-renew.log
   -> cron으로 갱신 스크립트를 돌리면 생길 수 있는 로그 파일
```

중요한 점은 아래 두 가지다.

1. `deploy/`와 `docker-compose.prod.yml`은 GitHub Actions가 EC2로 복사한다.
2. `.env`와 `certbot/` 내부 데이터는 EC2에서 유지된다. 즉 민감정보와 인증서는 GitHub에서 내려오지 않는다.

## 5. Docker 컨테이너들은 어떻게 협력하는가

### 5.1 운영 시점의 역할 분담

| 컨테이너 | 이미지 | 역할 | 누구와 협력하는가 |
| --- | --- | --- | --- |
| `db` | `mysql:8.0` | 영속 데이터 저장 | `app`이 `db:3306`으로 접속 |
| `app` | `ghcr.io/.../campusform-server` | Spring Boot API 실행 | `nginx`가 `app:8080`으로 프록시, `db`에 쿼리 |
| `nginx` | `nginx:1.27-alpine` | 80/443 공개, TLS 종료, 리버스 프록시 | `app`, `certbot`과 설정/인증서 공유 |
| `certbot` | `certbot/certbot` | 인증서 발급/갱신 전용 일회성 컨테이너 | `nginx`와 `certbot/conf`, `certbot/www` 공유 |

### 5.2 네트워크와 볼륨 기준 협력

```text
브라우저
  -> EC2 80/443
  -> nginx 컨테이너
  -> app:8080
  -> db:3306

certbot 컨테이너
  -> certbot/www 에 challenge 파일 생성
  -> certbot/conf 에 인증서 저장
  -> nginx 컨테이너가 같은 경로를 마운트해서 즉시 사용
```

추가로 중요한 점은 아래와 같다.

1. `app`은 `ports`가 아니라 `expose: 8080`만 사용한다.
   - 즉 호스트 외부에는 직접 열리지 않고, 같은 Compose 네트워크 안의 `nginx`만 접근한다.

2. `db`는 `db_data` named volume을 사용한다.
   - app 컨테이너가 새 이미지로 갈아끼워져도 DB 데이터는 살아남는다.

3. `nginx`는 템플릿 파일과 인증서 디렉터리를 bind mount 한다.
   - 설정 변경이나 인증서 갱신이 이미지 재빌드 없이 반영된다.

## 6. 시간 흐름으로 보는 상세 동작

여기서는 "최초 1회 세팅"과 "이후 반복되는 자동 배포"를 나눠서 본다. 두 흐름은 완전히 같지 않다.

### 6.1 최초 1회: 운영 서버를 받을 준비를 한다

#### 시점 A. EC2에 운영 작업 디렉터리를 만든다

무슨 일이 일어나는가:

- EC2에 `~/campusform-v2` 디렉터리가 준비된다.
- 이 안에 `docker-compose.prod.yml`, `deploy/`, `certbot/`, `.env`가 모인다.

관련 파일:

- `docker-compose.prod.yml`
- `deploy/nginx/default.conf.template`
- `deploy/nginx/bootstrap.conf.template`
- `deploy/scripts/renew-certificate.sh`
- EC2의 `~/campusform-v2/.env`

왜 필요한가:

- 이후 GitHub Actions는 이 경로를 기준으로 파일을 덮어쓰고 `docker compose`를 실행한다.

#### 시점 B. `.env`를 EC2에 직접 만든다

무슨 일이 일어나는가:

- 운영 비밀값을 EC2에만 저장한다.
- GitHub Actions는 `.env`를 복사하지 않는다.

중요 포인트:

- 워크플로우는 `.env`가 없으면 바로 실패한다.
- 특히 `API_DOMAIN`, `MYSQL_ROOT_PASS`, `DB_NAME`, `MYSQL_USER_NAME`, `MYSQL_USER_PASS`는 비어 있으면 안 된다.

#### 시점 C. 최초 인증서 발급 전이라면 bootstrap Nginx를 쓴다

무슨 일이 일어나는가:

- 아직 `certbot/conf/live/<도메인>/fullchain.pem` 같은 인증서 파일이 없다.
- 그런데 운영용 `default.conf.template`은 443과 인증서 파일을 바로 참조한다.
- 그래서 첫 배포 시점에는 `bootstrap.conf.template`처럼 HTTP-only 설정이 필요하다.

왜 필요한가:

- 인증서가 없는 상태에서 443 설정으로 Nginx를 띄우면 Nginx 기동이 실패할 수 있다.

실무적으로 보면:

1. 처음에는 `bootstrap.conf.template` 내용으로 Nginx를 띄운다.
2. Certbot으로 인증서를 발급한다.
3. 그 다음 다시 `default.conf.template` 기반의 HTTPS 설정으로 돌아온다.

### 6.2 이후 반복: 개발자가 `main`에 push 한다

#### 시점 1. GitHub Actions 워크플로우가 시작된다

트리거:

- `main` 브랜치 push
- 또는 `workflow_dispatch`

무슨 일이 일어나는가:

- `.github/workflows/deploy.yml`이 실행된다.
- `concurrency` 설정 때문에 같은 시점에 여러 배포가 겹치면 이전 실행은 취소되고 최신 배포만 남는다.

#### 시점 2. CI 검증이 먼저 돈다

무슨 일이 일어나는가:

1. 저장소 checkout
2. JDK 17 설치
3. `chmod +x gradlew`
4. `./gradlew test`

왜 중요한가:

- 테스트가 실패하면 이미지도 만들지 않고 배포도 하지 않는다.

#### 시점 3. Docker 이미지가 만들어지고 GHCR에 올라간다

무슨 일이 일어나는가:

1. GitHub Actions가 GHCR 로그인
2. Buildx 준비
3. `Dockerfile` 기반 이미지 빌드
4. 아래 태그로 GHCR push
   - `ghcr.io/<owner>/campusform-server:latest`
   - `ghcr.io/<owner>/campusform-server:<commit-sha>`

이때 `Dockerfile` 안에서 실제로 하는 일:

1. builder stage가 JDK 17 기반으로 Gradle 빌드를 수행한다.
2. `src`, `gradle`, `gradlew`, `build.gradle`, `settings.gradle`을 복사한다.
3. builder stage에서 다시 `./gradlew bootJar --no-daemon`를 실행한다.
4. runtime stage는 JRE 17 기반의 가벼운 이미지로 바뀐다.
5. builder stage에서 만든 `app.jar`만 최종 이미지에 넣고 `8080`으로 실행한다.

중요 포인트:

- 현재 JAR 빌드는 `Dockerfile` 내부에서 한 번만 수행된다.
- `.dockerignore`가 `.git`, `build`, `*.md` 등을 제외해 빌드 컨텍스트를 줄여준다.

### 6.3 이미지가 준비되면 EC2에 배포 자산을 보낸다

#### 시점 4. EC2로 복사되는 것은 소스코드가 아니라 운영 자산이다

무슨 일이 일어나는가:

- `appleboy/scp-action`이 아래만 EC2 `~/campusform-v2`로 복사한다.
  - `docker-compose.prod.yml`
  - `deploy/`

중요 포인트:

- EC2에는 `src/`나 `Dockerfile` 전체가 올라가지 않는다.
- EC2는 소스코드를 빌드하지 않고, GHCR에서 미리 빌드된 이미지 하나를 받아 실행한다.

### 6.4 EC2에서 실제 컨테이너를 교체한다

#### 시점 5. SSH로 접속해서 `.env`를 검사한다

무슨 일이 일어나는가:

1. `~/campusform-v2`로 이동
2. `.env` 존재 여부 확인
3. Windows에서 만들어진 `.env`일 수 있으므로 BOM/CRLF 제거
4. 필수 변수 누락 검사

이 단계가 중요한 이유:

- 운영 서버가 비밀값 없이 뜨는 것을 막는다.
- 줄바꿈 문제 때문에 환경변수 파싱이 깨지는 것을 방지한다.

#### 시점 6. `IMAGE_URI`를 최신 이미지로 맞춘다

무슨 일이 일어나는가:

- 워크플로우가 `.env` 안의 `IMAGE_URI` 값을 `ghcr.io/<owner>/campusform-server:latest`로 덮어쓴다.
- 기존 `IMAGE_URI`가 없으면 새로 추가한다.

의미:

- 다음 `docker compose up -d`는 항상 방금 GHCR에 올라간 최신 앱 이미지를 바라보게 된다.

참고:

- GHCR에는 SHA 태그도 같이 올라가지만, 현재 자동 배포는 `latest`를 사용한다.

#### 시점 7. EC2가 GHCR에서 앱 이미지를 pull 한다

무슨 일이 일어나는가:

1. EC2가 `GHCR_USERNAME`, `GHCR_PAT`로 GHCR 로그인
2. `docker compose -f docker-compose.prod.yml pull app`

중요 포인트:

- 여기서는 `app`만 명시적으로 pull 한다.
- `db`와 `nginx`는 compose 파일의 고정 이미지(`mysql:8.0`, `nginx:1.27-alpine`)를 사용한다.

#### 시점 8. `docker compose up -d --remove-orphans`가 실행된다

무슨 일이 일어나는가:

- Compose가 `.env`와 `docker-compose.prod.yml`을 읽는다.
- 필요한 컨테이너를 생성하거나 재생성한다.
- compose 정의에서 사라진 orphan 컨테이너는 제거한다.

컨테이너별 실제 동작:

1. `db`
   - MySQL 8.0 컨테이너가 뜬다.
   - `MYSQL_*` 값으로 초기화된다.
   - `db_data` volume을 사용하므로 데이터는 컨테이너 교체와 분리된다.
   - healthcheck가 성공할 때까지 대기한다.

2. `app`
   - `IMAGE_URI`가 가리키는 GHCR 이미지를 사용한다.
   - `SPRING_PROFILES_ACTIVE=prod`로 부팅한다.
   - DB 주소는 `jdbc:mysql://db:3306/...` 형태라 Compose 내부 DNS로 `db`를 찾는다.
   - `depends_on`의 health condition 때문에 DB가 healthy가 된 뒤 시작한다.

3. `nginx`
   - 80, 443 포트를 EC2 호스트에 공개한다.
   - `/etc/nginx/templates/default.conf.template`에 템플릿을 마운트한다.
   - `API_DOMAIN`이 템플릿에 주입되어 실제 설정 파일이 생성된다.
   - `app`이 시작된 뒤 `app:8080`으로 프록시한다.
   - 인증서는 `./certbot/conf`를 마운트해서 읽는다.

#### 시점 9. 불필요한 이미지 정리를 한다

무슨 일이 일어나는가:

- `docker image prune -f`가 dangling image를 정리한다.

효과:

- EC2 디스크가 계속 쌓이는 것을 완화한다.

## 7. 런타임 요청 흐름

배포가 끝난 뒤 실제 사용자 요청은 아래 순서로 흐른다.

```text
브라우저
  -> https://API_DOMAIN
  -> EC2 443
  -> nginx 컨테이너
  -> app:8080
  -> db:3306
```

### 7.1 nginx 단계에서 일어나는 일

- 80 포트 요청은 `default.conf.template` 기준으로 HTTPS로 리다이렉트된다.
- 443 포트 요청은 TLS 종료 후 `app:8080`으로 전달된다.
- `X-Forwarded-For`, `X-Forwarded-Proto` 헤더가 함께 전달된다.

### 7.2 app 단계에서 일어나는 일

- `application.yml`의 `prod` 프로필이 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 읽는다.
- `server.forward-headers-strategy`와 Tomcat 헤더 설정이 프록시 뒤 환경을 인지한다.
- `SecurityConfig.java`가 `CORS_ALLOWED_ORIGINS`를 읽어 프론트엔드만 허용한다.
- OAuth 쿠키 저장소가 `COOKIE_DOMAIN`, `COOKIE_SECURE`, `COOKIE_SAME_SITE`를 읽어 서브도메인 쿠키를 맞춘다.
- 세션 쿠키도 `SERVER_SERVLET_SESSION_COOKIE_*` 값으로 맞춰진다.

### 7.3 db 단계에서 일어나는 일

- `app`은 `db` 서비스 이름으로 MySQL에 연결한다.
- Compose 네트워크 안에서는 컨테이너 이름이 내부 DNS 이름처럼 동작한다.

## 8. 인증서 발급과 갱신 흐름

이 부분은 자동 배포와 별도의 흐름이다. 특히 Certbot은 상시 실행되는 서비스가 아니라 필요할 때 잠깐 실행되는 컨테이너다.

### 8.1 최초 발급

흐름:

1. `nginx`가 HTTP-only bootstrap 설정으로 먼저 뜬다.
2. `certbot` 컨테이너가 `certbot/www`에 challenge 파일을 둔다.
3. Let's Encrypt가 `http://API_DOMAIN/.well-known/acme-challenge/...`로 검증한다.
4. 인증서가 `certbot/conf`에 저장된다.
5. 이후 Nginx를 운영용 HTTPS 설정으로 바꾼다.

핵심 협력 포인트:

- `nginx`와 `certbot`은 `certbot/www`, `certbot/conf`를 공유한다.
- 그래서 challenge 응답과 인증서 파일이 두 컨테이너 사이에서 자연스럽게 연결된다.

### 8.2 갱신

`deploy/scripts/renew-certificate.sh`가 하는 일:

1. `certbot/certbot` 컨테이너를 일회성으로 띄워 `renew` 실행
2. 성공하면 `docker compose exec -T nginx nginx -s reload`

의미:

- Nginx 컨테이너를 통째로 다시 만들지 않고 reload만으로 새 인증서를 반영한다.

## 9. 최초 1회와 이후 배포의 차이

### 최초 1회

- `.env`를 사람이 직접 만들어야 한다.
- 인증서가 없으므로 bootstrap Nginx가 먼저 필요할 수 있다.
- Certbot 최초 발급 절차가 따로 들어간다.

### 이후 반복 배포

- `main` push만으로 자동 배포된다.
- 새 앱 이미지만 pull 해서 `app`이 교체된다.
- DB 데이터는 volume에 남는다.
- Nginx는 설정이 바뀌지 않았다면 같은 역할을 유지한다.

## 10. 이 저장소 기준으로 꼭 알아야 할 포인트

1. EC2는 소스코드를 빌드하지 않는다.
   - 빌드는 GitHub Actions와 Dockerfile에서 끝난다.
   - EC2는 GHCR 이미지를 pull 해서 실행만 한다.

2. 앱 배포와 데이터 보존이 분리되어 있다.
   - 앱은 이미지 교체 방식
   - DB는 volume 유지 방식

3. `app`은 외부에 직접 열리지 않는다.
   - 외부 공개 진입점은 `nginx` 하나다.

4. 인증서 관리도 Docker 협력 구조 안에 들어와 있다.
   - `nginx`는 challenge 응답과 인증서 사용
   - `certbot`은 발급/갱신만 담당

5. 워크플로우 메시지와 저장소 상태 사이에 작은 차이가 있다.
   - `deploy.yml`은 `.env`가 없으면 `deploy/.env.prod.example`에서 만들라고 안내한다.
   - 하지만 현재 저장소에는 그 예시 파일이 보이지 않는다.
   - 실제 운영에서는 EC2의 `~/campusform-v2/.env`를 직접 준비해야 한다.

## 11. 가장 짧게 요약하면

현재 CI/CD는 아래 한 줄로 요약할 수 있다.

```text
main push -> GitHub Actions가 테스트/이미지 빌드/GHCR push -> EC2가 최신 app 이미지를 pull -> docker compose가 db/app/nginx를 묶어 운영 -> nginx가 외부 트래픽을 app으로 전달 -> app이 db와 통신
```

그리고 인증서는 아래처럼 별도 흐름으로 붙는다.

```text
certbot 컨테이너 -> certbot/conf, certbot/www 갱신 -> nginx reload -> HTTPS 유지
```
