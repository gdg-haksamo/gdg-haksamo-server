# prod 앱이 부팅에 실패해 컨테이너 무한 재시작 — SMTP 미설정

- 발생: 2026-06 (PR #40)
- 영역: 배포 / 설정(Spring Mail)

## 증상
- `docker ps` 에서 `haksamo-app`이 "Up N seconds"를 반복(RestartCount 153), 8080 미응답
- `docker logs haksamo-app` → `APPLICATION FAILED TO START`

## 원인
prod 전용 `SmtpMailSender`가 `JavaMailSender` 빈을 주입받는데, `application-prod.yml`에 `spring.mail.*` 설정이 없어 해당 빈이 자동 구성되지 않음 → 컨텍스트 초기화 실패. (dev는 `LoggingMailSender`라 무관)

## 해결
- `application-prod.yml`에 `spring.mail.*` 추가 — `MAIL_HOST` 기본값 `smtp.gmail.com`으로 **host 미설정으로 인한 `JavaMailSender` 미구성(부팅 실패) 문제를 방지**.
- 단, **`MAIL_USERNAME`/`MAIL_PASSWORD`는 별도 필수**로 주입해야 한다. 미주입 시 부팅은 되더라도 실제 인증번호 발송 단계에서 SMTP 인증 실패가 발생한다.
- `docker-compose.yml` app 서비스에 `MAIL_*`·`ADMIN_*` 환경변수 전달(.env에만 있고 컨테이너로 안 넘어가던 문제도 동시 수정).
- `cd.yml`의 `.env` 생성에 `MAIL_*` 추가, GitHub Secrets에 `MAIL_USERNAME`/`MAIL_PASSWORD`(Gmail 앱 비밀번호) 등록.

## 교훈
- prod 부팅이 외부 의존(메일 빈)에 묶이지 않도록, 빈 자동구성 조건(`spring.mail.host`)을 기본값으로 충족시켜 **부팅 실패와 런타임 발송 실패를 분리**했다.
