# gdg-haksamo-server

경북대 학식 추천 PWA **학사모**의 백엔드 레포지토리.

자세한 기획·ERD·업무 분담은 [`docs/`](./docs) 참고.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.x |
| DB | MySQL 8.0 (docker-compose) |
| Auth | Spring Security + JWT (JJWT 0.12.6) |
| AI | Gemini 1.5 Flash (Vertex AI) — 예정 |
| Push | FCM — 예정 |

---

## 빠른 시작 (로컬 개발)

### 1. 환경변수 준비
```bash
cp .env.example .env
# 에디터로 .env 열어서 비밀번호 / JWT_SECRET 등을 채워주세요
```

### 2. DB 실행 (docker-compose)
```bash
docker compose up -d        # MySQL 컨테이너 백그라운드 실행
docker compose ps           # 상태 확인
docker compose logs -f db   # 로그 보기
```

종료:
```bash
docker compose down         # 컨테이너 중지·삭제 (데이터는 volume에 보존)
docker compose down -v      # 데이터까지 완전 삭제 (주의)
```

### 3. Spring Boot 애플리케이션 실행
IntelliJ에서 `HaksamoApplication`을 Run 하거나,
```bash
cd haksamo
./gradlew bootRun
```

기본 포트는 `8080`. `http://localhost:8080`에서 확인.

---

## 프로젝트 구조

```
gdg-haksamo-server/
├── .github/                  GitHub 이슈/PR 템플릿, 워크플로
├── docs/                     기획·ERD·업무분담·학습 가이드
├── haksamo/                  Spring Boot 모듈
│   └── src/main/java/com/gdg/haksamo/
│       ├── HaksamoApplication.java
│       ├── domain/           도메인별 패키지
│       │   ├── user/         회원/인증
│       │   ├── restaurant/
│       │   ├── menu/
│       │   ├── review/
│       │   ├── preference/
│       │   ├── recommendation/  AI 추천
│       │   ├── notification/    푸시 알림
│       │   └── event/
│       └── global/           공통 인프라
│           ├── config/
│           ├── security/
│           ├── exception/
│           ├── response/     ApiResponse 등
│           └── common/       BaseEntity 등
├── docker-compose.yml        로컬 MySQL
└── .env.example              환경변수 예시
```

---

## 협업 워크플로

### 1) 이슈 생성
- GitHub → Issues → New Issue → 작업 유형(feat / fix / docs / refactor / chore) 템플릿 선택
- 제목 prefix는 템플릿이 자동으로 채워줍니다 (`[FEAT] ...`)

### 2) 브랜치 생성
이슈 번호를 반드시 포함합니다.

| 유형 | 브랜치 | 예시 |
|------|--------|------|
| 기능 | `feat/{이슈번호}` | `feat/5` |
| 버그 | `fix/{이슈번호}` | `fix/12` |
| 리팩터링 | `refactor/{이슈번호}` | `refactor/8` |
| 기타 작업 | `chore/{이슈번호}` | `chore/1` |
| 문서 | `docs/{이슈번호}` | `docs/3` |

```bash
git checkout dev
git pull origin dev
git checkout -b feat/5
```

### 3) 커밋
[.github/commit_convention.md](./.github/commit_convention.md) 규칙을 따릅니다.

```
feat: 사용자 로그인 API 추가
fix: 비밀번호 검증 로직 오류 수정
chore: docker-compose에 MySQL 추가
```

### 4) Push & PR
```bash
git push -u origin feat/5
```
- 브랜치를 push하면 `dev`로 가는 PR이 **자동 생성**됩니다 (`.github/workflows/auto-pr.yml`)
- PR 본문은 `pull_request_template.md`가 자동 적용됩니다 — 체크리스트를 채워주세요
- 리뷰어 1명 이상 승인 후 머지

### 5) 머지 후
```bash
git checkout dev
git pull origin dev
git branch -d feat/5     # 로컬 브랜치 정리
```

---

## 문서 인덱스

- [docs/project-context.md](./docs/project-context.md) — 프로젝트 개요·팀·일정·MVP 기능
- [docs/app-design.md](./docs/app-design.md) — 화면·기능 설계 (기획 이미지 텍스트화)
- [docs/erd.sql](./docs/erd.sql) — DDL
- [docs/erd-decisions.md](./docs/erd-decisions.md) — ERD 설계 결정 사유
- [docs/work-division.md](./docs/work-division.md) — BE 업무 분담
- [docs/learning-guide.md](./docs/learning-guide.md) — Spring 학습 가이드
- [docs/학사모.png](./docs/학사모.png) — 기획 완성본 (전체 화면 흐름)
- [docs/학사모 앱 설계.webp](./docs/학사모%20앱%20설계.webp) — 홈 화면 상세 설계
