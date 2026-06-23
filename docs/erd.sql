-- 학사모 ERD (v1.2)
-- 작성: 김동우 / 최종수정: 2026-05-21
-- v1.2: 학사모.png 기획 완성본 반영
--   - User: 알림 세분화(4종), favorite_restaurant 관계 분리
--   - FavoriteRestaurant 신규 (자주 가는 학식당 N개)
--   - ReviewHelpful 신규 (리뷰 도움됐어요)
-- 작성: 김채윤 / 최종수정: 2026-06-21
-- v1.3: Menu/MenuSchedule 분리 설계 반영
--   - MenuSchedule 추가 (오늘 메뉴 리스트 관리)
--   - Menu 수정 (설명 추가, 품절->MenuSchedule, 특식 삭제)
-- 작성: 김동우 / 최종수정: 2026-06-23
-- v1.4: 디자인(Figma)·공식당 미팅 + 인증 구현 반영
--   - User: department(학과)·grade(학년) 추가 (회원가입 시 수집)
--   - Menu: image_url·description·영양정보는 신규 메뉴 첫 크롤링 시 Gemini 생성 (주석 명시)
--   - Recommendation: refresh_count·excluded_menu_ids 추가 (새로고침 3회 제한·중복 추천 방지)
--   - 인증: refresh_token(토큰 회전), email_verification(회원가입 1단계 이메일 인증) 테이블 추가
--   - 회원가입 이메일 도메인 제한(@knu) 제거 — 형식·중복·소유(인증번호)만 검증
-- 작성: 김동우 / 최종수정: 2026-06-24
-- v1.5: 관리자 RBAC 3단계 반영
--   - User.role: ENUM('USER','ADMIN') → ENUM('USER','RESTAURANT_ADMIN','SUPER_ADMIN')
--   - User.managed_restaurant_id 추가 (RESTAURANT_ADMIN의 담당 식당 — 자기 식당만 관리)
--   - 관리자 페이지: 식당 운영자(자기 식당만) + 운영팀(전체 메뉴/리뷰/계정) 분리 (erd-decisions #27)

CREATE TABLE `User` (
    `user_id`                        BIGINT         NOT NULL AUTO_INCREMENT,
    `email`                          VARCHAR(255)   NOT NULL,
    `password`                       VARCHAR(255)   NOT NULL,
    `nickname`                       VARCHAR(255)   NOT NULL,
    `department`                     VARCHAR(255)   NULL,                             -- 학과 (회원가입 시 입력, 예: 컴퓨터학부)
    `grade`                          INT            NULL,                             -- 학년 (예: 2 = 2학년)
    -- 권한 3단계: USER(학생) / RESTAURANT_ADMIN(식당 운영자, 자기 식당만) / SUPER_ADMIN(운영팀, 전체)
    `role`                           ENUM('USER', 'RESTAURANT_ADMIN', 'SUPER_ADMIN') NOT NULL DEFAULT 'USER',
    -- RESTAURANT_ADMIN이 관리하는 식당. USER/SUPER_ADMIN은 NULL. (앱 레벨 FK, JPA는 Long 컬럼으로 보유)
    `managed_restaurant_id`          BIGINT         NULL,
    `fcm_token`                      VARCHAR(255)   NULL,                             -- FCM 푸시 알림용
    -- 알림 설정 (마이페이지 토글 5종)
    `push_notification_enabled`      BOOLEAN        NOT NULL DEFAULT TRUE,            -- 마스터 푸시 on/off
    `notification_breakfast_enabled` BOOLEAN        NOT NULL DEFAULT TRUE,            -- 아침 추천 알림
    `notification_lunch_enabled`     BOOLEAN        NOT NULL DEFAULT TRUE,            -- 점심 추천 알림
    `notification_dinner_enabled`    BOOLEAN        NOT NULL DEFAULT TRUE,            -- 저녁 추천 알림
    `notification_event_enabled`     BOOLEAN        NOT NULL DEFAULT TRUE,            -- 이벤트/공지 알림
    `created_at`                     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uq_user_email` (`email`)
);

-- 자주 가는 학식당 (마이페이지에서 사용자가 N개 등록)
CREATE TABLE `FavoriteRestaurant` (
    `user_id`       BIGINT   NOT NULL,
    `restaurant_id` BIGINT   NOT NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `restaurant_id`),
    CONSTRAINT `fk_favrest_user`       FOREIGN KEY (`user_id`)       REFERENCES `User` (`user_id`),
    CONSTRAINT `fk_favrest_restaurant` FOREIGN KEY (`restaurant_id`) REFERENCES `Restaurant` (`restaurant_id`)
);

CREATE TABLE `Restaurant` (
    `restaurant_id`  BIGINT          NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(255)    NOT NULL,
    `map_x`          DECIMAL(10, 7)  NULL,   -- 경도 (longitude)
    `map_y`          DECIMAL(10, 7)  NULL,   -- 위도 (latitude)
    `operating_hour` VARCHAR(255)    NULL,
    PRIMARY KEY (`restaurant_id`)
);

CREATE TABLE `Menu` (
    `menu_id`       BIGINT          NOT NULL AUTO_INCREMENT,
    `restaurant_id` BIGINT          NOT NULL,
    `name`          VARCHAR(255)    NOT NULL,
    `price`         INT             NULL,
    `category`      VARCHAR(255)    NULL,
    `image_url`     VARCHAR(255)    NULL,   -- 신규 메뉴 첫 크롤링 시 Gemini 생성 (기존 메뉴는 스킵)
    `description`   VARCHAR(255)    NULL,   -- AI 한줄설명: 신규 메뉴 첫 크롤링 시 Gemini 생성 (메뉴 상세의 'AI 한줄 설명')
    `calories`      INT             NULL,   -- 영양정보(칼·탄·단·지): 신규 메뉴 첫 크롤링 시 Gemini 생성, null인 경우만 호출
    `protein`       INT             NULL,
    `carb`          INT             NULL,
    `fat`           INT             NULL,
    PRIMARY KEY (`menu_id`),
    UNIQUE KEY `uq_menu_identity` (`restaurant_id`, `name`),  -- (식당, 이름) 식별자 → upsert 기준, 리뷰 정합성 방어선
    CONSTRAINT `fk_menu_restaurant` FOREIGN KEY (`restaurant_id`) REFERENCES `Restaurant` (`restaurant_id`)
);

CREATE TABLE `MenuSchedule` (
    `schedule_id`   BIGINT   NOT NULL AUTO_INCREMENT,
    `menu_id`       BIGINT   NOT NULL,
    `date`          DATE     NOT NULL,
    `time`          ENUM('BREAKFAST', 'LUNCH', 'DINNER')  NOT NULL,
    `operating_time` VARCHAR(255)  NOT NULL,
    `is_sold_out`   BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (`schedule_id`),
    UNIQUE KEY `uq_sched` (`menu_id`, `date`, `time`),
    CONSTRAINT `fk_sched_menu`       FOREIGN KEY (`menu_id`)       REFERENCES `Menu` (`menu_id`)
);

CREATE TABLE `Review` (
    `review_id`  BIGINT   NOT NULL AUTO_INCREMENT,
    `menu_id`    BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `rating`     INT      NOT NULL,   -- 1~5
    `content`    TEXT     NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`review_id`),
    CONSTRAINT `fk_review_menu` FOREIGN KEY (`menu_id`) REFERENCES `Menu` (`menu_id`),
    CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `User` (`user_id`)
);

-- 리뷰 "도움됐어요" (1 사용자 = 1 리뷰에 1번)
CREATE TABLE `ReviewHelpful` (
    `review_id`  BIGINT   NOT NULL,
    `user_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`review_id`, `user_id`),
    CONSTRAINT `fk_rhelpful_review` FOREIGN KEY (`review_id`) REFERENCES `Review` (`review_id`),
    CONSTRAINT `fk_rhelpful_user`   FOREIGN KEY (`user_id`)   REFERENCES `User` (`user_id`)
);

CREATE TABLE `Preference` (
    `preference_id` BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT         NOT NULL,
    `keyword`       VARCHAR(255)   NOT NULL,
    `type`          ENUM('LIKED', 'DISLIKED') NOT NULL,  -- 선호/기피 구분
    PRIMARY KEY (`preference_id`),
    CONSTRAINT `fk_preference_user` FOREIGN KEY (`user_id`) REFERENCES `User` (`user_id`)
);

-- AI 추천 캐시 헤더 (하루 1회 생성 + 새로고침 시 동일 row UPDATE, 최대 3회)
CREATE TABLE `Recommendation` (
    `recommendation_id`  BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT   NOT NULL,
    `date`               DATE     NOT NULL,
    `reason`             TEXT     NULL,    -- Gemini 추천 이유 (해당 추천 row 단위)
    `refresh_count`      INT      NOT NULL DEFAULT 0,   -- 오늘 새로고침 횟수 (최대 3회)
    `excluded_menu_ids`  JSON     NULL,    -- 오늘 이미 추천한 menu_id 목록 (중복 추천 방지용)
    `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 마지막 새로고침 시각
    PRIMARY KEY (`recommendation_id`),
    UNIQUE KEY `uq_recommendation_user_date` (`user_id`, `date`),  -- 하루 1행 강제
    CONSTRAINT `fk_recommendation_user` FOREIGN KEY (`user_id`) REFERENCES `User` (`user_id`)
);

-- AI 추천 메뉴 목록 (1:N)
CREATE TABLE `RecommendationMenu` (
    `id`                BIGINT NOT NULL AUTO_INCREMENT,
    `recommendation_id` BIGINT NOT NULL,
    `menu_id`           BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_recmenu_recommendation` FOREIGN KEY (`recommendation_id`) REFERENCES `Recommendation` (`recommendation_id`),
    CONSTRAINT `fk_recmenu_menu`           FOREIGN KEY (`menu_id`)           REFERENCES `Menu` (`menu_id`)
);

CREATE TABLE `Notification` (
    `notification_id` BIGINT         NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT         NOT NULL,
    `title`           VARCHAR(255)   NOT NULL,
    `content`         TEXT           NULL,
    `is_read`         BOOLEAN        NOT NULL DEFAULT FALSE,
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`notification_id`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `User` (`user_id`)
);

CREATE TABLE `Event` (
    `event_id`   BIGINT         NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(255)   NOT NULL,
    `content`    TEXT           NULL,
    `image_url`  VARCHAR(255)   NULL,
    `start_date` DATE           NULL,
    `end_date`   DATE           NULL,
    `created_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`event_id`)
);

-- ===== 인증 (Auth) =====

-- Refresh Token (사용자당 1행 = 단일 디바이스). 토큰 해시 저장 + 회전/재사용 감지용.
CREATE TABLE `refresh_token` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `token_hash` VARCHAR(512) NOT NULL,                  -- 원문 대신 SHA-256 해시 저장
    `expires_at` DATETIME     NOT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_refresh_user` (`user_id`)              -- 유저당 1행 (재로그인 시 회전)
);

-- 회원가입 1단계 이메일 인증 (이메일당 1행, 재요청 시 덮어씀). 도메인 제한 없음 — 소유 검증.
CREATE TABLE `email_verification` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT,
    `email`               VARCHAR(255) NOT NULL,
    `code`                VARCHAR(6)  NOT NULL,           -- 6자리 인증번호
    `expires_at`          DATETIME    NOT NULL,           -- 인증번호 유효 만료 (기본 3분, FE 타이머와 일치)
    `verified`            BOOLEAN     NOT NULL DEFAULT FALSE,
    `attempt_count`       INT         NOT NULL DEFAULT 0, -- 검증 시도 횟수 (제한 5회)
    `verified_expires_at` DATETIME    NULL,               -- 인증 완료 상태 만료(회원가입 마감 시한, 기본 30분)
    `created_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_email_verification_email` (`email`)
);
