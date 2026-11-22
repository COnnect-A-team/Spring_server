# NutriPick Server

NutriPick 서버 프로젝트입니다. Spring Boot 애플리케이션은 로컬에서 실행하고, PostgreSQL과 Redis는 Docker로 실행합니다.

## 준비물

- Java 21
- Docker Desktop

## 로컬 서버 실행

프로젝트 루트 폴더에서 아래 순서로 실행합니다.

### 1. PostgreSQL과 Redis 실행

Docker Desktop을 실행한 뒤 다음 명령어를 입력합니다.

```bash
docker compose up -d
docker compose ps
```

PostgreSQL과 Redis 컨테이너가 실행 중인지 확인합니다.

### 2. 로컬 설정 파일 만들기

최초 한 번만 예시 파일을 복사합니다.

```bash
cp src/main/resources/application-local.example.yaml src/main/resources/application-local.yaml
```

`src/main/resources/application-local.yaml`을 열어 다음 값이 Docker 설정과 맞는지 확인합니다.

| 설정 | 값 |
| --- | --- |
| PostgreSQL URL | `jdbc:postgresql://localhost:5433/nutripick` |
| PostgreSQL 사용자 | `nutripick` |
| PostgreSQL 비밀번호 | `nutripick-local` |
| Redis 호스트 | `localhost` |
| Redis 포트 | `6330` |

이 로컬 설정 파일은 개인용이며 Git에 커밋하지 않습니다. 예시 파일은 팀 공통 설정이므로 커밋합니다.

### 3. 인증과 메일 설정

JWT 서명 키는 Base64로 인코딩한 32바이트 이상의 난수를 사용합니다. 로컬에서 처음 실행할 때 다음 명령으로 설정할 수 있습니다. 배포 환경에서는 모든 서버 인스턴스와 재시작 사이에 같은 키를 사용하도록 시크릿으로 관리합니다.

```bash
export NUTRIPICK_JWT_SECRET="$(openssl rand -base64 32)"
```

이메일 인증을 사용하려면 SMTP 서버와 발신 주소를 설정합니다. 다음 예시는 로컬에서 실행 중인 테스트 메일함의 주소입니다.

```bash
export SPRING_MAIL_HOST=127.0.0.1
export SPRING_MAIL_PORT=1025
export NUTRIPICK_MAIL_FROM=no-reply@nutripick.test
```

실제 메일 서버는 제공자의 SMTP 포트와 인증 및 TLS 설정을 따릅니다. 인증이 필요하면 `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`와 해당 제공자의 `spring.mail.properties`를 설정합니다.

### 4. Spring Boot 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

로그에 `Started NutripickApplication`이 표시되면 서버가 실행된 것입니다. 서버를 종료할 때는 터미널에서 `Ctrl + C`를 누릅니다.

## 인증 API

| 순서 | 메서드와 경로 | 요청 및 결과 |
| --- | --- | --- |
| 1 | `POST /api/v1/auth/signup` | `email`, `password`, `name`으로 가입합니다. 201 응답의 `requiresEmailVerification`은 `true`입니다. |
| 2 | `POST /api/v1/auth/email-verification/send` | `email`로 6자리 인증번호를 요청합니다. 유효기간은 10분이고, 같은 이메일로 재발송하려면 60초를 기다립니다. |
| 3 | `POST /api/v1/auth/email-verification/confirm` | `email`과 메일로 받은 문자열 `token`을 보냅니다. 인증이 완료되면 `emailVerified`가 `true`입니다. |
| 4 | `POST /api/v1/auth/login` | `email`, `password`로 로그인합니다. `accessToken`, `refreshToken`, `expiresIn`(초), `user`를 반환합니다. |
| 5 | `GET /api/v1/users/me` | Access Token에 담긴 사용자 ID로 본인의 계정 정보를 조회합니다. |

POST 요청의 본문은 JSON입니다. 이메일의 `@` 앞부분은 대소문자를 구분하고, 도메인은 대소문자를 구분하지 않습니다.

인증번호는 Redis에 사용자 ID별로 보관합니다. 두 계정에 같은 숫자가 발송되어도 재발송, 만료, 오입력 횟수는 계정별로 처리합니다. 한 번호를 5회 잘못 입력하면 재발송이 필요합니다.

Access Token은 1시간 동안 유효한 JWT이고, Refresh Token은 Redis에 14일 동안 보관합니다. 로그인 응답의 `accessToken` 값으로 보호 API를 호출합니다.

```bash
curl -H "Authorization: Bearer ${ACCESS_TOKEN}" http://localhost:8080/api/v1/users/me
```

보안 설정은 위의 공개 POST 네 개와 명세상 공개인 `POST /api/v1/auth/google`, `POST /api/v1/auth/password-reset/request`, `POST /api/v1/auth/password-reset/confirm`에 익명 접근을 허용합니다. 그 밖의 경로는 기본적으로 인증을 요구합니다. Google 로그인·비밀번호 재설정·로그아웃 컨트롤러는 별도 작업입니다.

보호 API에 토큰이 없거나 만료·검증 실패·Refresh Token 사용인 경우 `401 UNAUTHORIZED`를 반환합니다. 인증된 사용자가 필요한 권한을 갖지 못하면 `403 FORBIDDEN`을 반환합니다. 자체 로그인에서는 자격 증명이 일치하지만 이메일이 미인증인 경우에도 403을 반환합니다.

## 사용자 API

네 API 모두 로그인 응답의 Access Token을 요구합니다. 서버는 토큰으로 조회·수정할 계정을 결정합니다.

| 메서드와 경로 | 요청과 응답 |
| --- | --- |
| `GET /api/v1/users/me` | 계정의 `id`, `email`, `name`, `emailVerified`, `tutorialCompleted`를 반환합니다. |
| `PATCH /api/v1/users/me/name` | `{"name":"새 이름"}`을 보내면 저장된 이름을 같은 형태로 반환합니다. |
| `GET /api/v1/users/me/onboarding` | 현재 이름, 튜토리얼 완료 여부, 선택 순서대로 정렬한 관심 영양소를 반환합니다. 첫 저장 전 목록은 `[]`입니다. |
| `PUT /api/v1/users/me/onboarding` | 아래 본문으로 이름, 튜토리얼 상태, 관심 영양소를 한 번에 저장하고 저장된 값을 반환합니다. |

```json
{
  "name": "뉴트리",
  "tutorialCompleted": true,
  "nutrientPreferences": [
    {"nutrientCode": "protein", "priority": "HIGH", "direction": "HIGHER", "selectionOrder": 1},
    {"nutrientCode": "sugar", "priority": "NORMAL", "direction": "LOWER", "selectionOrder": 2}
  ]
}
```

이름은 공백만으로 구성할 수 없고 최대 100자입니다. 영양소 코드는 앞뒤 공백을 제거하고 소문자로 저장하며 최대 50자입니다. 우선순위는 `LOW`, `NORMAL`, `HIGH` 중 하나이며 생략하면 `NORMAL`입니다. 방향은 `HIGHER`와 `LOWER`를 지원합니다.

잘못된 요청 필드에는 `400 INVALID_REQUEST`를 반환합니다. 온보딩에는 서로 다른 영양소 두 개를 순서 1과 2로 저장해야 합니다. 개수, 중복 또는 순서가 이 조건에 맞지 않으면 `422 UNPROCESSABLE_ENTITY`를 반환하며 기존 값이 유지됩니다. 유효한 토큰에 해당하는 계정이 없으면 `404 NOT_FOUND`를 반환합니다.

## 테스트

테스트는 개인 로컬 설정 파일이나 Docker DB 없이 H2 메모리 데이터베이스를 사용합니다.

```bash
./gradlew test
```

## Docker 종료

컨테이너만 종료하려면 다음을 실행합니다.

```bash
docker compose down
```

PostgreSQL 데이터는 Docker 볼륨에 유지됩니다. `docker compose down -v`는 저장된 DB 데이터도 삭제하므로 필요한 데이터가 있을 때는 사용하지 마세요.
