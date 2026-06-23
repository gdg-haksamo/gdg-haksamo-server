# 학사모 API 명세

> 구현 완료 API를 기록한다(constitution IV). FE 연동 기준 문서이며, 동일 내용을 팀 Notion에도 미러링한다.

## 공통 규약

- **Base URL**: `/api`
- **응답 래퍼** (`ApiResponse`)
  - 성공: `{ "success": true, "data": { ... } }`
  - 실패: `{ "success": false, "code": "U001", "message": "이미 가입된 이메일입니다." }`
  - (null 필드는 생략됨)
- **인증**: `Authorization: Bearer {accessToken}` 헤더.
  - Access Token = 로그인 응답 본문 → **FE 메모리 보관**.
  - Refresh Token = **httpOnly 쿠키**(`refreshToken`) → FE가 직접 접근 불가, 요청 시 `credentials: 'include'` 필수.
- **401 처리(FE)**: 보호 API에서 401 수신 → `POST /api/auth/reissue`로 Access 재발급 후 재시도. reissue도 401이면 로그인 화면으로.

## 인증 (Auth)

### 1) 이메일 인증번호 발송 — 회원가입 1단계
`POST /api/auth/email/send-code` · 인증 X
```json
{ "email": "user@example.com" }
```
- 200: 발송 성공 (dev는 서버 로그로 코드 출력, 실제 메일 미발송)
- 409 `U001`: 이미 가입된 이메일 / 400 `C001`: 이메일 형식 오류
- 재요청 시 기존 코드 무효화. 코드 6자리, 유효 **3분**(FE 타이머와 일치).

### 2) 이메일 인증번호 검증
`POST /api/auth/email/verify-code` · 인증 X
```json
{ "email": "user@example.com", "code": "123456" }
```
- 200: 인증 완료 (이후 30분 내 회원가입 가능)
- 400 `U004`(요청내역 없음)/`U005`(만료)/`U006`(불일치), 429 `U007`(시도 5회 초과)

### 3) 회원가입 — 2단계(계정 생성)
`POST /api/auth/signup` · 인증 X · **이메일 인증 완료 필수**
```json
{ "email": "user@example.com", "password": "********", "nickname": "경대생", "department": "컴퓨터학부", "grade": 2 }
```
- 201: `{ "userId": 1, "email": "...", "nickname": "..." }`
- 400 `U003`(이메일 미인증) / 400 `C001`(검증 실패: 비밀번호 8자↑, 닉네임 2~8자) / 409 `U001`(중복)
- 도메인 제한 없음(@knu 불요). 자주 가는 식당/선호 키워드(3단계)는 Restaurant/Preference 도메인 연동 후 추가 예정.

### 4) 로그인
`POST /api/auth/login` · 인증 X
```json
{ "email": "user@example.com", "password": "********" }
```
- 200: `{ "accessToken": "...", "accessTokenExpiresIn": 3600000 }` + `Set-Cookie: refreshToken=...; HttpOnly; ...`
- 401 `A002`: 이메일 또는 비밀번호 불일치

### 5) Access Token 재발급
`POST /api/auth/reissue` · refresh 쿠키 필요
- 200: 새 `accessToken` + 회전된 refresh 쿠키
- 401 `A005`(쿠키 없음)/`A003`(유효하지 않음)/`A006`(불일치·재사용 → 전체 무효화, 재로그인 필요)

### 6) 로그아웃
`POST /api/auth/logout`
- 200: refresh 쿠키 만료 + 서버 저장 토큰 삭제

## 사용자 (User)

### 7) FCM 토큰 등록/갱신
`PATCH /api/users/me/fcm-token` · 인증 O
```json
{ "fcmToken": "..." }
```
- 200 / 401 `A001`(미인증) / 404 `U002`(사용자 없음)

## 관리자 (Admin)

> 권한 모델: `USER`(학생) / `RESTAURANT_ADMIN`(식당 운영자, 자기 식당만) / `SUPER_ADMIN`(운영팀, 전체). 설계: erd-decisions #27.

### 계정 관리 — SUPER_ADMIN 전용 (구현 완료)

모든 `/api/admin/**` 경로는 `SUPER_ADMIN`만 접근 가능(아니면 403 `A007`).

#### 8) 계정 목록 조회
`GET /api/admin/users?role={ROLE}&page=0&size=20` · SUPER_ADMIN
- `role` 생략 시 전체. 값: `USER` | `RESTAURANT_ADMIN` | `SUPER_ADMIN`
- 200: `{ "users": [ { "userId", "email", "nickname", "role", "managedRestaurantId" } ], "page", "size", "totalElements", "totalPages" }`

#### 9) 식당 운영자 계정 발급
`POST /api/admin/users/restaurant-admin` · SUPER_ADMIN
```json
{ "email": "gongsikdang@knu.ac.kr", "password": "********", "nickname": "공식당", "restaurantId": 1 }
```
- 201: `{ "userId", "email", "nickname", "role": "RESTAURANT_ADMIN", "managedRestaurantId": 1 }`
- 400 `U008`(restaurantId 누락) / 400 `C001`(검증 실패) / 409 `U001`(이메일 중복)
- 일반 회원가입과 달리 **이메일 인증 단계 없음**(운영팀이 직접 발급).

#### 10) 권한/담당 식당 변경
`PATCH /api/admin/users/{userId}/role` · SUPER_ADMIN
```json
{ "role": "RESTAURANT_ADMIN", "managedRestaurantId": 2 }
```
- 200: 변경된 계정 정보. `role`이 RESTAURANT_ADMIN이 아니면 담당 식당 자동 해제(null).
- 400 `U008`(RESTAURANT_ADMIN인데 식당 미지정) / 400 `U009`(본인 계정 변경 불가) / 404 `U002`

#### 11) 비밀번호 재설정
`PATCH /api/admin/users/{userId}/password` · SUPER_ADMIN
```json
{ "newPassword": "********" }
```
- 200 / 400 `C001`(8자 미만) / 404 `U002`

#### 12) 계정 삭제
`DELETE /api/admin/users/{userId}` · SUPER_ADMIN
- 200 / 400 `U009`(본인 계정 삭제 불가) / 404 `U002`

### 메뉴/리뷰 관리 — RESTAURANT_ADMIN·SUPER_ADMIN (예정 · Menu/Review 도메인 연동)

> 아래는 Menu/Restaurant/Review 도메인(김채윤) 위에 얹힌다. 각 서비스가 조작 전
> `RestaurantAdminGuard.requirePermission(userId, restaurantId)`를 호출해 **본인 담당 식당만** 허용한다.
> RESTAURANT_ADMIN이 타 식당 자원을 건드리면 403 `A008`. SUPER_ADMIN은 전체 통과.

- **품절 토글** `PATCH /api/menus/schedules/{scheduleId}/sold-out` — `MenuSchedule.is_sold_out` 토글
- **신메뉴 등록** `POST /api/menus` — `Menu` + 당일 `MenuSchedule` 생성
- **이름·가격 수정** `PATCH /api/menus/{menuId}` — `Menu.name` / `Menu.price`
- **리뷰 삭제(부적절 리뷰)** `DELETE /api/reviews/{reviewId}` — SUPER_ADMIN 전체, RESTAURANT_ADMIN은 자기 식당 메뉴의 리뷰만

(경로·요청 형식은 채윤님 도메인 구현 시 확정 → 본 절에 반영)

## 에러 코드 표

| code | HTTP | 의미 |
|---|---|---|
| C001 | 400 | 입력값 검증 실패 |
| A001 | 401 | 인증 필요 |
| A002 | 401 | 이메일/비밀번호 불일치 |
| A003 | 401 | 유효하지 않은 토큰 |
| A004 | 401 | 만료된 토큰 |
| A005 | 401 | 리프레시 토큰 없음 |
| A006 | 401 | 리프레시 토큰 불일치(재사용) |
| A007 | 403 | 권한 없음 |
| A008 | 403 | 담당하지 않은 식당 자원 접근(식당 운영자) |
| U001 | 409 | 이메일 중복 |
| U002 | 404 | 사용자 없음 |
| U003 | 400 | 이메일 미인증 |
| U004 | 400 | 인증 요청 내역 없음 |
| U005 | 400 | 인증번호 만료 |
| U006 | 400 | 인증번호 불일치 |
| U007 | 429 | 인증 시도 횟수 초과 |
| U008 | 400 | 식당 운영자 계정에 담당 식당 미지정 |
| U009 | 400 | 본인 계정 권한 변경/삭제 불가 |

## 접근 정책 (비로그인)

- **공개(비로그인 가능)**: 메인 "오늘의 학식 메뉴 리스트" (Menu 도메인 구현 시 `GET` 공개 예정).
- **보호(로그인 필요)**: AI 추천, 메뉴 상세, 리뷰, 마이페이지, FCM 등 그 외 전부 → 비로그인 시 401 → FE가 로그인/회원가입 유도.
