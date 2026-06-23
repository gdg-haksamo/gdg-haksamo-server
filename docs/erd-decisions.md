# ERD 설계 결정사항

각 설계 선택의 이유와 트레이드오프를 기록합니다.

---

## 1. `Preference` — 1행 1키워드 구조 (1:N)

**결정:** User당 여러 행으로 키워드를 저장 (`user_id` + `keyword` + `type`)

**이유:**
- 키워드를 개별 행으로 저장하면 특정 키워드 추가/삭제가 단순한 INSERT/DELETE로 처리됨
- `type ENUM('LIKED', 'DISLIKED')`으로 선호/기피를 한 테이블에서 관리

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 키워드 단건 삭제가 간단 | 유저 조회 시 JOIN 또는 서브쿼리 필요 |
| 키워드 수 유연하게 확장 가능 | 키워드가 많아지면 행이 많아짐 (MVP 수준엔 무관) |

**대안:** JSON 컬럼(`liked_keywords JSON`)에 배열로 저장
- 조회는 간단하지만 개별 키워드 추가/삭제에 애플리케이션 레벨 로직 필요, MySQL JSON 인덱스 제한

---

## 2. `MenuSchedule.time` — VARCHAR 대신 ENUM

**결정:** `ENUM('BREAKFAST', 'LUNCH', 'DINNER')`

**이유:**
- 학식은 조/중/석 3가지만 존재하는 고정 도메인
- ENUM은 잘못된 값 입력을 DB 레벨에서 차단
- 애플리케이션에서 Java `enum`과 1:1 매핑되어 타입 안전성 확보

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 잘못된 값 DB 레벨 차단 | 새 끼니 추가 시 ALTER TABLE 필요 (현실적으로 발생 안 함) |
| Java enum 연동 자연스러움 | - |

---

## 3. `MenuSchedule.date` — DATETIME 대신 DATE

**결정:** `DATE` 타입

**이유:**
- 학식 메뉴는 날짜 단위로 관리되므로 시간 정보가 의미 없음
- `DATETIME`을 쓰면 `2026-05-19 00:00:00` vs `2026-05-19 12:00:00` 같은 값 불일치 위험
- DATE 기준 인덱스 조회가 더 효율적

---

## 4. `MenuSchedule.is_sold_out` — 날짜별 품절 상태

**결정:** `MenuSchedule`에 `is_sold_out BOOLEAN` 컬럼 추가

**이유:**
- 품절 상태는 메뉴의 속성이므로 별도 테이블 분리 불필요
- 관리자가 PATCH 한 번으로 품절 처리 가능

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 조회 쿼리 간단 (JOIN 불필요) | 품절 이력(언제 품절됐는지)을 추적할 수 없음 |

**결론:** 이력 추적은 MVP 범위 밖이므로 직접 컬럼으로 결정

---

## 5. `User.fcm_token` — User 테이블 직접 컬럼

**결정:** 별도 테이블 없이 `User`에 `fcm_token VARCHAR(255)` 컬럼

**이유:**
- 디바이스가 1개인 PWA 환경에서 토큰은 사실상 유저당 1개
- 별도 `DeviceToken` 테이블은 멀티디바이스 지원 시 필요

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 구조 단순, JOIN 불필요 | 멀티디바이스 지원 불가 |

**결론:** PWA는 단일 브라우저 환경이므로 허용 가능한 트레이드오프

---

## 6. `Recommendation` + `RecommendationMenu` — 테이블 분리

**결정:** 헤더(`Recommendation`)와 추천 메뉴 목록(`RecommendationMenu`)을 1:N으로 분리

**이유:**
- Gemini가 복수의 메뉴를 추천하는 구조를 표현하려면 1:N이 필요
- 추천 이유(`reason`)는 1회 생성되는 메타데이터이므로 헤더에 보관
- `(user_id, date)` UNIQUE KEY로 하루 1행만 유지
- 추천 후보는 MenuSchedule에서 조회
- 추천 결과는 Menu 기준으로 저장

**새로고침 정책 (UI 새로고침 버튼 대응):**
- 자동 호출: 하루 첫 요청 시 새 row 생성 (기존 캐시 hit이면 그대로 반환)
- 사용자 새로고침: 같은 `(user_id, date)` row를 **UPDATE** (RecommendationMenu도 삭제 후 재삽입), 하루 최대 **3회** 제한
- `refresh_count`로 당일 횟수 추적, 3회 초과 시 429 응답 반환
- `excluded_menu_ids`(JSON): 새로고침 시 현재 추천 menu_id를 누적, 다음 Gemini 호출 프롬프트에 제외 목록으로 전달 → 중복 추천 방지 (당일만 유지, 다음날 새 row 생성 시 초기화)
- `updated_at`으로 마지막 갱신 시각 추적, 히스토리는 보존하지 않음

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 복수 추천 메뉴 표현 가능 | 조회 시 JOIN 1회 추가 |
| 하루 1행 강제로 캐시 정책 단순 | `excluded_menu_ids` JSON이 커질 수 있으나 하루 최대 3회 × 추천 메뉴 수라 미미 |
| `refresh_count`로 남용 방지를 DB 레벨에서 관리 | - |

**대안:** Redis 캐시 — 조회 성능은 더 높지만 인프라 복잡도 증가, 4주 프로젝트 범위 초과

---

## 7. `Restaurant.map_x/map_y` — INT 대신 DECIMAL(10,7)

**결정:** `DECIMAL(10, 7)`

**이유:**
- 위도/경도는 소수점 7자리가 필요 (ex. 위도 35.8897623, 경도 128.6102453)
- INT는 소수점 없이 저장되어 좌표 정밀도가 완전히 손실됨

---

## 8. `Review` — 중복 리뷰 허용

**결정:** 동일 유저가 같은 메뉴에 여러 번 리뷰 작성 가능 (UNIQUE 제약 없음)

**이유:**
- 4주 MVP에서 중복 방지 로직 구현 비용 대비 효과 낮음
- 서비스 초기엔 리뷰 수를 늘리는 것이 우선

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 구현 단순 | 어뷰징 가능성 존재 |

**후속 조치:** 서비스 안정화 후 `UNIQUE KEY (user_id, menu_id)` 추가 고려

---

## 9. `Notification` — 알림 히스토리 테이블

**결정:** 발송된 알림을 `Notification` 테이블에 기록

**이유:**
- 앱 내 알림 내역 조회 기능(후순위)을 위한 기반
- FCM 발송 성공 여부와 무관하게 발송 기록 보존

**결론:** MVP에서 알림 내역 UI는 후순위이지만 테이블은 미리 준비

---

## 10. `User` — 알림 설정 세분화 (5종 토글)

**결정:** `User`에 알림 관련 BOOLEAN 컬럼 5개를 추가
- `push_notification_enabled` — 마스터 토글
- `notification_breakfast_enabled` — 아침 추천 알림
- `notification_lunch_enabled` — 점심 추천 알림
- `notification_dinner_enabled` — 저녁 추천 알림
- `notification_event_enabled` — 이벤트/공지 알림

**이유:**
- 기획서(학사모.png) 마이페이지 알림 설정에 토글 5종이 명시되어 있음
- 발송 시 마스터 토글이 OFF면 끼니별 토글과 무관하게 발송 중단
- 마스터 ON + 끼니별 OFF면 해당 끼니 알림만 차단
- 별도 `UserNotificationSetting` 테이블은 항목 수가 적어 과도함

**트레이드오프:**
| 장점 | 단점 |
|------|------|
| 구조 단순, JOIN 불필요 | 설정이 더 늘어나면 컬럼이 늘어남 (10+ 시 분리 고려) |
| 발송 측 쿼리에서 한 번에 필터 가능 | - |

**대안:** Preference 테이블에 type을 확장하는 방안 — 의미가 다른 데이터(알림 vs 키워드)를 같은 테이블에 두는 게 부적절해서 기각.

---

## 11. 좋아요/하트 — 도입하지 않음

**결정:** 메뉴에 좋아요/하트 기능을 추가하지 않고, 대신 **리뷰 개수**를 카드에 노출

**이유:**
- 기획서 캡션에 명시: "찜 목록 및 하트는 구현 안 할 것임. 대신 해당 메뉴 리뷰 갯수 구현"
- 좋아요 테이블을 만들면 사용자 행동 데이터가 분산됨 — 리뷰 1개로 통합하는 것이 단순
- 메뉴 카드 UI에는 ★ 이미지 + 리뷰 개수만 노출

**결론:** `Like`/`Favorite` 류 테이블 없음. 메뉴 조회 API에서 `Review` 개수를 집계해 반환.

---

## 12. `FavoriteRestaurant` — 자주 가는 학식당 (N개)

**결정:** `(user_id, restaurant_id)` 복합 PK의 별도 테이블

**이유:**
- 기획서(학사모.png) 마이페이지에 "자주 가는 학식당"이 복수로 노출 (예: 봉식당, 고측한식당, 정보화시단 등)
- User에 단일 FK로 두면 1개만 등록 가능 → 부적합
- Preference 테이블에 keyword로 식당명을 넣는 방식은 `restaurant_id` FK가 빠져 정합성 떨어짐

**활용 흐름:**
- 홈 화면 default 노출 식당을 사용자별로 결정 (자주 가는 식당 우선)
- 푸시 알림에서 추천 메뉴 선정 시 가중치로 사용 가능

---

## 13. `ReviewHelpful` — 리뷰 "도움됐어요"

**결정:** `(review_id, user_id)` 복합 PK 테이블. 1 사용자 = 1 리뷰에 1번 표시 가능.

**이유:**
- 기획서(학사모.png) 메뉴 상세 페이지의 리뷰 카드에 "도움됐어요" 액션이 존재
- 마이페이지에 본인 리뷰가 받은 도움됐어요 누계 노출
- UNIQUE는 복합 PK로 자연 강제됨

**조회:**
- 리뷰별 카운트: `SELECT review_id, COUNT(*) FROM ReviewHelpful GROUP BY review_id`
- 본인 리뷰 누계: `SELECT COUNT(*) FROM ReviewHelpful rh JOIN Review r ON rh.review_id = r.review_id WHERE r.user_id = ?`

**미도입 (기획서에서 X 표시):**
- 리뷰 이벤트 / 도움됐어요 순 당첨자 뽑기 — 4주 MVP 범위 초과
- 이벤트 기능을 도입하게 되면 `ReviewEvent (id, start_date, end_date)` + `ReviewEventWinner` 별도 설계

---

## 14. 별점 그래프 (1~5점 분포)

**결정:** 별도 테이블 없음. `Review.rating GROUP BY` 집계로 처리.

**이유:**
- 메뉴 상세 페이지에 5/4/3/2/1점 각각의 리뷰 개수 막대그래프 표시
- `Review` 테이블이 이미 존재하므로 추가 테이블 불필요
- 쿼리 예: `SELECT rating, COUNT(*) FROM Review WHERE menu_id = ? GROUP BY rating`

---

## 15. 회원가입 시 키워드 입력 — **도입함** (Figma 디자인 반영, v1.4에서 결정 변경)

**결정:** 회원가입을 3단계로 구성하고, 마지막 단계에서 선호 키워드를 선택받는다. (가입 후 마이페이지에서 수정 가능)

> **변경 이력:** 초기 기획(학사모.png)에서는 회원가입 키워드 단계에 X 표시였으나, 최종 Figma 디자인(`signin-1/2/3`)에서 3단계 가입으로 확정되어 **도입하는 것으로 변경**.

**회원가입 3단계 (Figma 기준):**
1. **기본 정보** — 이메일(@knu.ac.kr), 비밀번호, 닉네임(2~8자)
2. **식당 선택** — 자주 가는 식당 복수 선택(`FavoriteRestaurant`) + 학과(`User.department`) 입력
3. **키워드 선택** — 선호 음식 키워드 복수 선택(`Preference`, type=LIKED)

**이유:**
- 가입 직후부터 개인화 추천이 동작하려면 키워드·자주 가는 식당이 필요
- 키워드 미설정(스킵) 사용자도 추천이 동작하도록, Gemini 프롬프트는 키워드가 비어 있을 때 일반 추천을 반환하게 분기

**키워드 입력 방식 (Figma 기준):**
- 자유 입력이 아니라 **고정 칩 선택**: 맛 취향(매운/순한/단/신), 음식 종류(면류/밥류/육류/해산물/채식/국·찌개/샐러드/빵·샌드위치), 요리 종류(한/중/일/양식), 건강 목표(저칼로리/고단백)
- 디자인상 **선호(LIKED)만** 수집 — 기피(DISLIKED) 입력 UI는 없음. 단, `Preference.type` ENUM은 유지하여 추후 기피 입력 확장 가능
- Gemini 프롬프트에는 LIKED 키워드만 반영 (DISLIKED는 데이터가 없으면 생략)

---

## 16. 인스타 이벤트 / 외부 SNS 연동 — 도입하지 않음 (X 표시)

**결정:** 공지/이벤트 페이지의 인스타 이미지 노출 기능은 미구현.

**이유:**
- 기획서(학사모.png) 공지·이벤트 페이지의 인스타 카드에 X 표시
- 외부 SNS API 연동은 4주 MVP 범위 초과
- 운영자가 직접 `Event` 테이블에 등록한 콘텐츠만 노출

---

## 17. Menu + MenuSchedule 분리

**결정:** "메뉴 정체성"과 "이번 주 편성"을 두 테이블로 분리한다.
- `Menu` = 영구 메뉴 카탈로그 (리뷰가 매달리는 곳, 안 지움)
- `MenuSchedule` = 이번 주 편성표 (월요일마다 전체 갈아엎음)

> Meal 헤더 + MealMenu 2개 테이블 대신 **단일 `MenuSchedule` 1개**만 추가. (Meal 헤더는 '운영 안 함(빈 슬롯)'과 '아예 크롤 안 됨'을 구분해 보여줄 때만 필요한데, UI가 그 구분을 안 하므로 과설계)

**핵심: 리뷰가 안 쌓이던 진짜 원인이 "메뉴 정체성 + 편성"이 한 테이블에 섞여서였음.** 분리로 해결되는 케이스:
- 고정/재등장 메뉴 → 같은 `(식당, 이름)`이라 upsert로 같은 `menu_id` 재사용 → 지난 리뷰 그대로 유지
- 한 주에 다른 요일 같은 메뉴 → 스케줄만 2행(`menu_id` 동일, `date` 다름), 리뷰는 `Menu` 1곳에 합산
- 월요일 초기화 → `MenuSchedule`만 wipe & 재삽입, `Menu`/`Review`는 영구 보존

**컬럼 배치 기준 — "이번 주 편성에 없어도 화면에 떠야 하는 값인가?"**
(검색→상세는 이번 주 편성에 없는 메뉴도 도달 가능한데, 스케줄은 매주 지워지므로)
- `name`/`price`/`image_url`/`description`/영양 → **Menu** (미편성 메뉴도 검색·상세에 떠야 함)
- `is_sold_out` → **MenuSchedule** (날짜별 상태, 매주 wipe로 자연 초기화)

**`Review.menu_id` → Menu** (변경 없음). `menu_id`가 안정적이라 리뷰가 정상 누적됨.

---

## 18. Menu 식별자 (restaurant_id, name)

**결정:** UNIQUE KEY (restaurant_id, name) 식별자 지정

**이유:**
- 같은 식당의 같은 메뉴는 동일 메뉴로 취급
- 식당, 메뉴 외 가격 등 달라지는 경우는 upsert 시켜 덮어씌움

---

## 19. Menu 영양·이미지·한줄설명 — 신규 메뉴 첫 크롤링 시 Gemini로 생성

**결정:** `image_url`, `description`(AI 한줄설명), 영양정보(`calories`/`protein`/`carb`/`fat`)는 크롤링하지 않고 **Gemini로 생성**한다. 생성 시점은 **새벽 크롤 스케줄러가 메뉴를 처음 적재할 때(미리)**.

**이유:**
- 메뉴 상세조회 시점에 Gemini를 호출하면 응답이 느려 **첫 조회자가 빈 화면**을 보게 됨 → 미리 생성해 둠
- 값이 비어있는(`NULL`) **신규 메뉴만** Gemini 호출, 고정/재등장 메뉴는 이미 채워져 있어 **스킵** → Gemini **비용 절감**
- 크롤 본체(편성 저장)와 **분리/비동기**로 처리 → Gemini가 느려도 `MenuSchedule` 저장은 즉시 완료

**흐름:**
```
크롤 스케줄러 → MenuSchedule 저장(즉시) 
            → (비동기) description/영양/image_url == NULL 인 Menu만 Gemini 호출하여 채움
```

---

## 20. 메뉴 크롤러 구조 — 식당별 파서 분리 + 공용 저장 1곳

**결정:** 파싱은 식당별 파서로 분리하되, 저장(upsert)은 공용 1곳으로 통일한다.
```
[식당별 파서 N개]  parse(Document) → List<ParsedMenu>     // 식당별 구조·시간 차이는 여기서 흡수
   ParsedMenu = { name(정제), time, price, dayIndex }
        ▼
[공용 저장 1곳]  // upsert·정제 중복 금지 (여기서 리뷰 정합성 버그 방지)
   menu = findByRestaurantAndName(r, name) ?? save(new Menu)   // (식당,이름) upsert
   menu.price = price                                          // 최신값 갱신
   scheduleRepo.save(new MenuSchedule(menu, 이번주월요일 + dayIndex, time))
```
- 크롤 시작 시 `MenuSchedule` 전체(또는 지난주분) DELETE 후 재삽입. `Review`·`Menu`는 건드리지 않아 안전.

**구현 시 주의 (정합성 critical):**
- **이름 정제가 정합성 핵심으로 격상됨**: "제육볶음" vs "제육볶음 "(공백)이 갈리면 카탈로그가 쪼개지고 리뷰가 분산됨 → 정제 regex를 **결정론적으로** + DB `UNIQUE(restaurant_id, name)`를 마지막 방어선으로. 이름은 정제본 1컬럼으로 표시·식별 겸용.
- `week`(요일 문자열) → 실제 `DATE`: 월요일 크롤 기준 `date = 이번주 월요일 + dayIndex`, 요일은 date에서 파생.
- enum 언어 통일: 조식/중식/석식 → `BREAKFAST`/`LUNCH`/`DINNER`.
- 가격 없는 메뉴에서 `Integer.parseInt` 예외 → `price` null 허용/방어.
- `restaurant` String → `restaurant_id` FK: Restaurant 행 시딩이 선행돼야 함.

---

## 21. `User.department` / `User.grade` — 학과·학년 컬럼

**결정:** `User`에 `department VARCHAR(255) NULL`(학과), `grade INT NULL`(학년) 추가.

**이유:**
- Figma 마이페이지 프로필에 "컴퓨터학부 / 2학년" 노출
- 회원가입 2단계에서 학과를 입력받음

**비고:** 학년(`grade`)의 입력 위치(가입 단계/마이페이지)는 디자인상 미확정. 컬럼은 미리 준비.

---

## 22. 식당 — 5곳 (공식당 학생/교직원 분리)

**결정:** 식당은 **5곳**: 공식당(학생), 공식당(교직원), 복지관, 정보센터, 카페테리아 첨성.

**이유:**
- Figma 홈/회원가입의 식당 탭이 공식당·복지관·정보센터·카페테리아 첨성 기준이고, 공식당은 **학생/교직원 식당이 분리** 운영됨
- 초기 문서의 "첨성관·공과대식당·정보전산원(3곳)"은 구버전 → 본 결정으로 대체

**비고:** 크롤러 파서는 현재 정보센터(정보전산원)만 구현됨 → 나머지 식당 파서 추가 필요. `Restaurant` 행 시딩 선행.

---

## 23. 관리자 페이지 — 공식당 운영자용 (공식당 미팅 결과)

**결정:** 관리자 페이지를 제공한다. 핵심은 **클릭 한 번**으로 끝나는 단순 조작. (권한 모델은 #27 참고 — `ADMIN` 단일에서 `RESTAURANT_ADMIN`/`SUPER_ADMIN` 2단계로 확장됨)
- **품절 처리** — 메뉴 옆 버튼 클릭으로 `MenuSchedule.is_sold_out` 토글 (다음 끼니·다음 날 자동 초기화)
- **신메뉴 등록** — `Menu` + 당일 `MenuSchedule` 추가
- **이름·가격 수정** — `Menu.name` / `Menu.price` 수정
- 위 메뉴 조작은 **본인 담당 식당에 한해서만** 가능 — 각 도메인 서비스에서 `RestaurantAdminGuard.requirePermission(userId, restaurantId)` 호출로 강제.

**이유:**
- 공식당 사장님 미팅 결과 관리자 페이지 제공 합의 ([[meeting_gongsikdang]])
- 영양사님이 이미 매일 하시는 품절 표시에 "버튼 클릭 한 번"만 얹는 프레임 → 관리자 부담 최소화

**비고:** 로드맵상 **2026-07-05 공식당 관리자 페이지 선제공**, 2026-07-10 베타 런칭. Figma에는 관리자 화면이 아직 없음(별도 설계 필요).

---

## 24. 인증 — Access(메모리) + Refresh(httpOnly 쿠키) + 회전

**결정:** JWT 기반. Access Token은 응답 본문(FE 메모리 보관), Refresh Token은 **httpOnly 쿠키**로 내려 브라우저·탭 종료 후에도 로그인 유지.

**이유 / 트레이드오프:**
- Access를 JS 접근 불가한 곳에 둘 수 없어 메모리 보관(XSS 노출 최소화), Refresh는 httpOnly 쿠키로 JS 접근 차단.
- **회전(rotation) + 재사용 감지**: `refresh_token`에 해시를 유저당 1행 저장. reissue마다 새 토큰으로 교체, 이미 회전된 옛 토큰 재사용 시 **행 삭제(전체 무효화)** → 탈취 대응. (refresh JWT에 `jti`로 매 토큰 고유성 보장)
- **유저당 1행 = 단일 디바이스**: 두 번째 로그인 시 첫 기기 무효화. `User.fcm_token` 단일 정책과 일관, MVP 적합.
- **쿠키 속성은 프로파일별 config**: FE와 **다른 도메인**이라 prod=`SameSite=None; Secure`(HTTPS) + CORS `allowCredentials` + 명시적 origin, dev=`Lax`/`secure=false`.
- 리프레시 회전/재사용 처리는 `@Transactional(noRollbackFor)`로 예외 시에도 revoke가 커밋되게 함.

---

## 25. 회원가입 1단계 — 이메일 인증 (도메인 무관, 소유 검증)

**결정:** 회원가입 1단계에서 이메일로 6자리 인증번호를 발송·검증한다. `email_verification` 테이블에 이메일당 1행 저장(Redis 없이 DB).

**핵심:**
- **이메일 도메인 제한(@knu)은 제거**. 인증번호는 도메인을 제한하는 게 아니라 이메일 **소유**를 검증.
- send-code: 중복 이메일(이미 가입) 차단 → 6자리 발송, 재요청 시 기존 코드 무효화.
- verify-code: 만료(기본 3분, FE 타이머와 일치) / **시도횟수 제한(5회)** / 일치 검사.
- 회원가입(`signup`)은 **유효한 인증 완료(verified + 30분 이내)** 여야 통과, 가입 후 인증 내역 소비(재사용 방지).
- **발송**: dev=로그 출력(SMTP 불필요) / prod=`JavaMailSender`(SMTP env). 프로파일별 `MailSender` 구현 주입.

---

## 26. 비로그인 접근 정책

**결정:** 메인 "오늘의 학식 메뉴 리스트"만 비로그인 공개, 그 외(추천·메뉴 상세·리뷰·마이 등)는 인증 필요.

**구현:**
- 시큐리티 기본이 `anyRequest().authenticated()` → 메뉴 리스트 외 전부 보호. 비로그인 접근 시 401 `ApiResponse` → FE가 "로그인 후 이용" 안내 + 회원가입 유도(추천 카드/상세/타 페이지 공통).
- "오늘 메뉴 리스트" 엔드포인트는 Menu 도메인(김채윤) 소관 → 경로 확정 시 `SecurityConfig`에 `GET` 공개 1줄 추가. **메뉴 상세는 공개하지 않음**(추측 permitAll 시 상세까지 노출되는 보안 구멍 주의). constitution III 공유자원 협의.

---

## 27. 관리자 RBAC — 식당 운영자 + 운영팀 2단계 + 계정 발급

**결정:** `role`을 `USER` / `RESTAURANT_ADMIN` / `SUPER_ADMIN` 3단계로 둔다. (#23의 단일 `ADMIN`을 분리)
- **RESTAURANT_ADMIN(식당 운영자)** — `User.managed_restaurant_id`로 묶인 **자기 식당만** 관리(품절·신메뉴·이름/가격·리뷰). 다른 식당은 조작 불가.
- **SUPER_ADMIN(운영팀)** — 전체 메뉴/리뷰/계정 관리. 식당 운영자 계정 발급·권한 변경·삭제.

**구현:**
- **인가 2축**:
  1. *계정 관리 API*(`/api/admin/**`) → `SecurityConfig` 경로 게이트 `hasRole('SUPER_ADMIN')`. 거부 시 `ExceptionTranslationFilter`→`JwtAccessDeniedHandler`로 403 `ApiResponse`.
  2. *식당 단위 메뉴/리뷰 조작* → 경로가 아니라 각 도메인 서비스에서 `RestaurantAdminGuard.requirePermission(userId, restaurantId)` 호출. SUPER_ADMIN 전체 통과, RESTAURANT_ADMIN은 `managed_restaurant_id` 일치 시만 통과, 아니면 `A008`(403). 권한 규칙을 한 곳에 모아 도메인마다 흩어지지 않게 함.
- **계정 발급**: 운영자 계정은 일반 회원가입(이메일 인증)과 별개로 **SUPER_ADMIN이 직접 발급**(`POST /api/admin/users/restaurant-admin`). 이메일 인증 단계 없음.
- **부트스트랩**: 최초 SUPER_ADMIN은 가입 경로가 없으므로 `AdminAccountInitializer`가 env(`ADMIN_EMAIL`/`ADMIN_PASSWORD`/`ADMIN_NICKNAME`)로 **앱 시작 시 1회 멱등 시딩**. 평문은 env/Secrets에만, DB엔 BCrypt 해시. 이미 존재하면 스킵.
- **잠금 방지**: SUPER_ADMIN이 본인 계정의 권한 변경·삭제는 불가(`U009`).
- **managed_restaurant_id**: Restaurant 엔티티(채윤님)와의 조기 결합을 피해 JPA에선 FK 없는 `Long` 컬럼으로 보유. 식당 존재 검증은 Restaurant 도메인 머지 후 추가.

**이유/트레이드오프:**
- 공식당·복지관 등 식당이 5곳 → 식당마다 다른 운영자 계정이 자기 식당만 만지게 해야 사고(타 식당 메뉴 오조작) 방지.
- 운영팀(우리)은 전체를 봐야 하므로 상위 권한 분리.
- 경로 게이트(계정)와 가드 컴포넌트(식당 자원)를 나눈 이유: 식당 자원 조작은 같은 경로(`/api/menus/...`)라도 *누구의 식당이냐*에 따라 허용이 갈려 경로만으론 못 막음 → 서비스 레벨 가드가 필요.

**비고:** 메뉴/리뷰 관리 API 본체는 Menu/Restaurant/Review 도메인(김채윤) 위에 얹힌다. 본 작업은 RBAC 기반·계정 관리·가드까지 제공하고, 메뉴/리뷰 관리 엔드포인트는 채윤님 엔티티 머지 후 가드 호출만 추가하면 되도록 설계(api-spec.md "관리자" 절의 *예정* 항목).