# BE 업무 분담 (김동우 / 김채윤)

분담 기준: 도메인 수직 분리. 각자 담당 도메인의 엔티티 → 서비스 → API까지 전담.

**Auth 완성은 선행 조건이 아님.**
dev 프로파일에서 `DummyAuthFilter`가 항상 userId=1인 가짜 인증을 주입하므로
김채윤은 Auth 완성 전에도 인증이 필요한 API를 병렬 개발 가능.
단, **`@AuthenticationPrincipal`로 userId를 추출하는 방식은 1주차에 합의 필수** (나중에 컨트롤러 전체를 수정하는 사태 방지).

---

## 담당 도메인

| 도메인 | 김동우 | 김채윤 |
|--------|--------|--------|
| 공통 인프라 (예외처리, 응답 형식) | ✅ 선행 | - |
| Auth (회원가입·로그인·JWT) | ✅ | - |
| Recommendation (Gemini AI) | ✅ | - |
| Notification (FCM 푸시 알림) | ✅ | - |
| EC2 배포 | ✅ | - |
| Restaurant | - | ✅ |
| Menu (크롤러 포함) | - | ✅ |
| Review | - | ✅ |
| Preference | - | ✅ |
| Event | - | ✅ |
| Admin (메뉴 수정·품절) | - | ✅ |

---

## 분담 이유

**김동우 — 인증·AI·인프라**
- Auth는 Security 설정과 묶여 있어 한 명이 전담해야 다른 API에 인증 필터를 일관되게 적용 가능
- Gemini API, FCM은 외부 API 연동이라 학습 비용이 있어 한 명이 집중하는 게 효율적
- EC2 배포 경험자가 배포까지 담당하면 환경 이슈 대응이 빠름

**김채윤 — 데이터·비즈니스**
- 크롤러 + Menu는 연결된 작업이라 분리하면 오히려 비효율
- Restaurant, Review, Preference, Event는 CRUD 위주라 병렬 개발 가능
- Admin 기능은 Menu와 같은 엔티티를 다루므로 동일인이 담당

---

## 주차별 작업 계획

### 1주차 — 설계 + 공통 기반
| 작업 | 담당 |
|------|------|
| 엔티티 전체 생성 (JPA Entity) | 공동 |
| `@AuthenticationPrincipal` 추출 방식 합의 | 공동 |
| 공통 응답 형식 (`ApiResponse`), 전역 예외 처리 | 김동우 |
| DummyAuthFilter (dev 환경 mock 인증) | 김동우 |
| Security 기본 설정 | 김동우 |
| 생협 크롤러 프로토타입 | 김채윤 |
| Restaurant 조회 API | 김채윤 |

### 2주차 — 핵심 기능
| 작업 | 담당 |
|------|------|
| 회원가입·로그인·JWT 발급 API | 김동우 |
| JWT 인증 필터 완성 + DummyAuthFilter 제거 | 김동우 |
| Menu 조회 API + 크롤러 스케줄러 연동 | 김채윤 |
| Review CRUD API (메뉴별 리뷰 개수 집계 포함) | 김채윤 |
| Preference 설정 API | 김채윤 |
| 검색 API (식당·메뉴) | 김채윤 |

### 3주차 — AI·알림·관리자
| 작업 | 담당 |
|------|------|
| Gemini API 연동 + 추천 캐싱 (자동/새로고침 UPDATE) | 김동우 |
| FCM 토큰 등록 + 끼니별 푸시 발송 (조건부 필터) | 김동우 |
| 알림 설정 API — 토글 5종 (마스터 + 끼니 3종 + 이벤트) | 김동우 |
| 자주 가는 학식당 API (`FavoriteRestaurant` CRUD) | 김채윤 |
| 리뷰 "도움됐어요" API (`ReviewHelpful` CRUD + 카운트 집계) | 김채윤 |
| 메뉴 상세 별점 분포 그래프 API (`Review` GROUP BY) | 김채윤 |
| Admin 메뉴 수정·품절 API | 김채윤 |
| Event CRUD API | 김채윤 |

### 4주차 — 배포·QA
| 작업 | 담당 |
|------|------|
| EC2 배포, application-prod 설정 | 김동우 |
| FE 연동 테스트 + 버그 수정 | 공동 |

---

## 협업 규칙

- **공유 작업 전 필수:** 엔티티 변경은 반드시 상대방에게 공지 후 진행
- **브랜치 전략:** `feat/{도메인명}` 브랜치에서 작업 → `dev` PR
- **API 명세:** 담당 도메인 API 완성 시 `/docs/api-spec.md`에 직접 추가
