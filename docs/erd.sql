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

CREATE TABLE `User` (
    `user_id`                        BIGINT         NOT NULL AUTO_INCREMENT,
    `email`                          VARCHAR(255)   NOT NULL,
    `password`                       VARCHAR(255)   NOT NULL,
    `nickname`                       VARCHAR(255)   NOT NULL,
    `role`                           ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',  -- 관리자 구분
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
    `image_url`     VARCHAR(255)    NULL,
    `description`   VARCHAR(255)    NULL,
    `calories`      INT             NULL,
    `protein`       INT             NULL,
    `carb`          INT             NULL,
    `fat`           INT             NULL,
    PRIMARY KEY (`menu_id`),
    UNIQUE KEY `uq_menu_identity` (`restaurant_id`, `name`),
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

-- AI 추천 캐시 헤더 (하루 1회 생성 + 새로고침 시 동일 row UPDATE)
CREATE TABLE `Recommendation` (
    `recommendation_id` BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT   NOT NULL,
    `date`              DATE     NOT NULL,
    `reason`            TEXT     NULL,   -- Gemini 추천 이유
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 새로고침 시각
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
