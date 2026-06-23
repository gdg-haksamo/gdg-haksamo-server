# 관리자 RBAC — 통합 & 후속 작업 노트

> 작성: 김동우 / 2026-06-24 (PR #28, feat/26)
>
> 관리자 RBAC·계정 관리(동우, Auth/공통)를 구현하면서, **채윤님 도메인(Menu/Restaurant/Review) 위에 얹혀야 완성되는 부분**과
> **임의 판단으로 먼저 만든 부분**, **출시 후로 미룬 하드닝**을 한곳에 정리한다.
> 새 작업 세션·채윤님이 이 문서만 보면 "무엇이 남았고 무엇을 건드려야 하는지" 알 수 있게 한다.
> (설계 근거는 erd-decisions #27, API는 docs/api-spec.md "관리자" 절)

---

## 1. 채윤님 도메인 통합 시 추가/수정해야 할 것

### 1-1. 메뉴/리뷰 관리 엔드포인트에 `RestaurantAdminGuard` 호출 추가 (필수)
식당 운영자(RESTAURANT_ADMIN)가 **자기 식당만** 조작하도록, 조작 직전에 가드를 한 줄 호출한다.
SUPER_ADMIN은 전체 통과, RESTAURANT_ADMIN은 `managed_restaurant_id` 일치 시만 통과, 아니면 403 `A008`.

- 가드: `com.gdg.haksamo.global.security.RestaurantAdminGuard#requirePermission(Long userId, Long restaurantId)`
- 호출 예 (품절 토글):
  ```java
  @Service
  @RequiredArgsConstructor
  public class MenuAdminService {
      private final RestaurantAdminGuard restaurantAdminGuard;
      private final MenuScheduleRepository menuScheduleRepository;

      @Transactional
      public void toggleSoldOut(Long userId, Long scheduleId) {
          MenuSchedule schedule = menuScheduleRepository.findById(scheduleId)
                  .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND)); // 에러코드 신설 필요
          Long restaurantId = schedule.getMenu().getRestaurant().getRestaurantId();
          restaurantAdminGuard.requirePermission(userId, restaurantId); // ← 이 한 줄
          schedule.setSoldOut(!schedule.isSoldOut());
      }
  }
  ```
- 컨트롤러는 `@AuthenticationPrincipal Long userId`를 받아 서비스로 전달.
- 대상 엔드포인트(예정, api-spec.md 참고): 품절 토글 / 신메뉴 등록 / 이름·가격 수정 / 리뷰 삭제.

### 1-2. `Review` 엔티티 신설 후 리뷰 관리 API (대기 중)
- 현재 `Review` 엔티티가 코드에 **없음**(ERD에는 정의됨). 생기기 전까지 리뷰 삭제 관리 API는 스펙(예정)으로만 둠.
- 리뷰 삭제: SUPER_ADMIN 전체 / RESTAURANT_ADMIN은 `review.menu.restaurant`가 자기 식당일 때만 → 위 가드 동일 패턴.

### 1-3. 관리 작업용 에러코드 신설
- 품절/메뉴/리뷰 not-found 등은 `ErrorCode`에 메뉴/리뷰 코드가 필요(예: `MENU_NOT_FOUND`, `SCHEDULE_NOT_FOUND`, `REVIEW_NOT_FOUND`). 현재 미정의.

---

## 2. 규약 불일치 — 팀 조율 필요 (우리가 임의 판단한 부분과 충돌)

| 항목 | 현재 상태 | 영향 / 조치 |
|---|---|---|
| **URL prefix** | 동우: `/api/auth`, `/api/users`, `/api/admin`<br>채윤: `MenuController` = `/menus` (`/api` 없음) | api-spec.md는 Base URL `/api` 기준. **컨벤션 통일 필요**(`/api/menus` 권장 or context-path `/api` 설정). 아래 SecurityConfig 공개 규칙·관리 API 경로도 여기 의존 |
| **응답 래퍼** | 동우: 전부 `ApiResponse<T>`<br>채윤: `MenuController`는 raw `List<MenuResponse>` 반환 | FE 파싱 일관성 위해 메뉴도 `ApiResponse`로 감싸는 게 좋음(협의) |
| **비로그인 공개 메뉴** | `SecurityConfig`는 `anyRequest().authenticated()` — 오늘 메뉴도 현재 **로그인 필요** | 정책상 "오늘 메뉴 리스트"는 공개 예정(erd-decisions #26). 경로 확정 후 `SecurityConfig`에 `GET` 공개 1줄 추가 필요. **메뉴 상세는 공개 금지** |

> SecurityConfig 안에 위 TODO를 주석으로 남겨둠(`global/security/SecurityConfig.java`). 경로/컨벤션 확정 시 1줄 추가.

---

## 3. 이미 채윤님 코드에 결합된 지점 (참고)

- `AdminUserService`는 식당 존재 검증을 위해 `RestaurantRepository#existsById`를 사용한다(이미 dev에 머지됨). 식당 운영자 발급/권한변경 시 무효 `restaurantId`면 404 `U010`.
- `User.managed_restaurant_id`는 **FK 없는 `Long` 컬럼**으로 둠(조기 결합 회피). Restaurant 스키마가 안정화되면 FK 전환 검토 가능(필수 아님 — 앱 레벨 검증으로 정합성 확보 중).

---

## 4. 출시 후로 미룬 하드닝 (CodeRabbit PR #28 리뷰, MVP 범위로 보류)

지금 고치지 않은 근거가 있는 항목. 트래픽/멀티 인스턴스 단계에서 재검토.

- **이메일 인증번호 해시 저장** (`email_verification.code` 평문) — 6자리·3분·5회 제한이라 단기 위험 낮음. 인증 흐름 변경 리스크로 보류.
- **`refresh_token` / `email_verification` FK** — userId/email 키만으로 느슨하게 결합(앱이 정리). ddl-auto가 FK 생성 안 함.
- **동시성**: 이메일 인증 시도횟수 race, 토큰 upsert 원자성, 시더 동시 기동 유니크 충돌 — 단일 EC2·단일 디바이스 정책이라 보류(시더는 DataIntegrityViolation→409 핸들러로 부분 완화).
- **`role`↔`managed_restaurant_id` DB CHECK 불변식** — 앱 레벨(`User.changeRole`)에서 보장.

---

## 5. 새 세션이 알아야 할 현재 상태 (요약)

- 브랜치 `feat/26` = **PR #28**. 공통기반·인증(JWT)·관리자 RBAC·계정관리 + **dev 머지분(채윤님 Menu/Restaurant/Event 크롤러)** 포함. CI green.
- dev에 있던 임시 `global/config/SecurityConfig.java`(permitAll 플레이스홀더)는 인증 SecurityConfig와 빈 충돌이라 **삭제함**. 앞으로 SecurityConfig는 `global/security/SecurityConfig.java` 하나.
- 관리자 권한: `USER` / `RESTAURANT_ADMIN`(자기 식당) / `SUPER_ADMIN`(전체). 최초 SUPER_ADMIN은 `AdminAccountInitializer`가 env(`ADMIN_EMAIL`/`PASSWORD`/`NICKNAME`)로 1회 시딩 — **PR #28이 dev 머지·배포돼야** prod `.env`에 주입됨.
- Swagger: `/swagger-ui.html`. Auth/User/Admin 컨트롤러 `@Tag`/`@Operation` 적용. 공개 엔드포인트도 전역 Bearer 표기됨(기능 무관, MVP 허용).