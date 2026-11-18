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

### 3. Spring Boot 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

로그에 `Started NutripickApplication`이 표시되면 서버가 실행된 것입니다. 서버를 종료할 때는 터미널에서 `Ctrl + C`를 누릅니다.

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
