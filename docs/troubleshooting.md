# 트러블슈팅 기록

운영 중 겪은 문제와 원인·해결을 누적 기록한다. 새 항목은 위에 추가.

---

## 1. 메뉴 크롤러가 200을 반환하지만 DB에 아무것도 저장 안 됨 (2026-06)

### 증상
- `POST /api/admin/menus/crawl?selDate=...` 호출 시 `{ "success": true }` (HTTP 200)
- 그러나 `menu_schedule` 테이블이 비어 있음 (`SELECT date, COUNT(*) FROM menu_schedule ...` → empty set)
- `GET /api/menus?date=...` 도 전부 빈 응답(count 0)
- 서버 로그에 명확한 에러가 잘 안 보임

### 결론(원인)
**EC2가 미국 리전(us-east-1)에 있어, 한국에서만 정상 접근되는 경북대 생협 사이트(`coop.knu.ac.kr`)에서 메뉴 HTML을 받아오지 못함.** 빈/비정상 응답 → 파서가 `div.week_table`를 못 찾음 → 빈 리스트 반환 → 저장 0건. 크롤러가 식당별 예외를 `try/catch`로 삼키고(로그만 남김) 항상 200을 반환해 실패가 드러나지 않았다.

### 원인이 아니었던 것 (조사로 배제)
- ❌ **파서 버그**: 한국 IP로 받은 동일 HTML을 분석하니 `td[0~4]`에 메뉴 22개씩·`￦가격` 구조가 파서 기대와 일치(파싱 시 100개+ 추출). 파서 정상.
- ❌ **selDate 미동작**: GET 파라미터로 주차 변경이 응답에 반영됨(응답 크기·내용 다름).
- ❌ **User-Agent 차단**: 브라우저/Java/빈 UA 모두 동일 HTML 수신.
- ❌ **식당 이름 불일치**: `RestaurantService` 시딩 이름(정보센터/복지관/첨성/글로벌플라자/공식당 학생식당/공식당 교직원식당)과 `saveMenus` 조회 이름 일치.
- ❌ **Swagger 타임아웃**: 200을 받았다는 건 동기 `crawl()`이 끝까지 돌았다는 뜻 → 중간에 끊긴 게 아님.

### 진단 방법
1. **한국 IP(로컬/브라우저)** 에서 크롤 대상 URL을 직접 열기 → `week_table` 2개 + 메뉴 데이터 확인(소스엔 데이터 있음).
2. **서버(EC2)** 에서 같은 URL을 curl → 데이터 없음/접속 불가 확인.

```bash
# 반드시 "한 줄"로 실행 (아래 함정 참고)
curl -sS -o /dev/null -w "HTTP=%{http_code} size=%{size_download}\n" \
"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=86&selDate=2026-06-15"
```

> ⚠️ **함정 — `\` 줄바꿈 복붙 사고**: 명령을 백슬래시(`\`)로 여러 줄에 걸쳐 붙여넣으면, 백슬래시 뒤에 공백이 섞여 줄 연속이 깨진다. 그러면 `curl`이 **URL 없이 실행되어 `HTTP=000`(가짜 실패)** 가 뜨고, URL 줄은 `-bash: https://...: No such file or directory` 로 따로 실행된다. **000을 "연결 실패"로 오해하기 쉬움.** 반드시 한 줄로 실행해 진짜 결과를 봐야 한다.

### 해결 — EC2를 서울 리전(ap-northeast-2)으로 이전
인스턴스는 리전 간 이동이 불가하므로 **AMI로 복제 후 서울에서 재기동**한다.
1. us-east-1 인스턴스 → `작업 → 이미지 및 템플릿 → 이미지 생성`(AMI)
2. AMI → `작업 → AMI 복사` → 대상 리전 `아시아 태평양(서울) ap-northeast-2`
3. 서울에서 복사된 AMI로 인스턴스 시작 — **t4g.small(ARM 유지)**, 보안그룹 인바운드 22·8080, 새 키페어
4. 탄력적 IP 할당·연결 → 새 퍼블릭 IP 확보
5. GitHub Secrets `EC2_HOST`(새 IP)·`EC2_SSH_KEY`(새 .pem) 갱신 → CD 재배포
6. 프론트엔드 API base URL을 새 IP로 교체
7. 서울에서 크롤 정상 확인 후 us-east-1 인스턴스 **종료** + 옛 탄력적 IP **해제**(미사용 IP 요금 방지)

검증: 서울 EC2에서 위 curl이 `HTTP=200 size=24000+` 이면 성공.

### 데이터 안전성 메모
- 크롤 시 **연결 예외**가 나면 `Jsoup.connect().get()`에서 던져져 `saveMenus`가 호출되지 않음 → 기존 편성이 wipe되지 않아 데이터 보존.
- 단 **빈 200 응답**이 오면 빈 리스트로 `saveMenus` 호출 → `deleteByMenu_Restaurant`로 wipe 후 미삽입 → **데이터 유실 위험**. (해외 리전에서 빈 페이지를 받는 경우 주의)

### 부수적으로 겪은 혼선
- **저장 날짜 ≠ selDate**: 크롤은 `selDate` 주의 "내용"을 가져오되, `saveMenus`가 항상 **"이번 주 월요일 + dayIndex"** 로 저장한다. 따라서 조회는 `selDate`가 아니라 **이번 주 날짜**로 해야 메뉴가 보인다.

### 향후 개선 (보류)
- 크롤 응답에 **저장 건수 / 실패 식당 목록**을 반환해 "조용한 실패"를 드러내기. (크롤러 구조 변경 필요 — CodeRabbit PR #44 지적과 동일. 채윤님 도메인이라 협의 후 반영)
- 근본적으로는 **서울 리전 유지**로 매주 월요일 스케줄 크롤이 정상 동작.

---

## 2. prod 앱이 부팅에 실패해 컨테이너 무한 재시작 — SMTP 미설정 (PR #40, 2026-06)

### 증상
- `docker ps` 에서 `haksamo-app`이 "Up N seconds"를 반복(RestartCount 153), 8080 미응답
- `docker logs haksamo-app` → `APPLICATION FAILED TO START`

### 원인
prod 전용 `SmtpMailSender`가 `JavaMailSender` 빈을 주입받는데, `application-prod.yml`에 `spring.mail.*` 설정이 없어 해당 빈이 자동 구성되지 않음 → 컨텍스트 초기화 실패. (dev는 `LoggingMailSender`라 무관)

### 해결
- `application-prod.yml`에 `spring.mail.*` 추가(`MAIL_HOST` 기본값 `smtp.gmail.com` → 자격증명이 비어도 부팅은 보장)
- `docker-compose.yml` app 서비스에 `MAIL_*`·`ADMIN_*` 환경변수 전달(.env에만 있고 컨테이너로 안 넘어가던 문제도 동시 수정)
- `cd.yml`의 `.env` 생성에 `MAIL_*` 추가, GitHub Secrets에 `MAIL_USERNAME`/`MAIL_PASSWORD`(Gmail 앱 비밀번호) 등록
