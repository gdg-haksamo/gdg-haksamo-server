package com.gdg.haksamo.domain.notification.scheduler;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.notification.service.NotificationService;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 끼니 직전 추천 푸시 스케줄러. (아침 07:30 / 점심 11:30 / 저녁 17:30, KST)
 *
 * <p>발송 대상마다 {@link NotificationService#pushTodayRecommendation}을 호출한다 —
 * 이 메서드가 푸시 전에 추천을 생성(또는 캐시)하므로 "추천 없는 빈 푸시"는 나가지 않는다.
 * 사용자 규모가 작아 끼니마다 per-user 생성한다(docs/adr/0001).
 *
 * <p>한 사용자 발송 실패가 전체를 막지 않도록 개별 try/catch로 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MealRecommendationScheduler {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 30 7 * * *", zone = "Asia/Seoul")
    public void pushBreakfast() {
        dispatch(MealTime.BREAKFAST);
    }

    @Scheduled(cron = "0 30 11 * * *", zone = "Asia/Seoul")
    public void pushLunch() {
        dispatch(MealTime.LUNCH);
    }

    @Scheduled(cron = "0 30 17 * * *", zone = "Asia/Seoul")
    public void pushDinner() {
        dispatch(MealTime.DINNER);
    }

    private void dispatch(MealTime meal) {
        List<User> targets = userRepository.findByFcmTokenIsNotNullAndPushNotificationEnabledTrue();
        int sent = 0;
        int skipped = 0;
        for (User user : targets) {
            if (!user.isMealNotificationEnabled(meal)) {
                continue; // 해당 끼니 토글 off
            }
            try {
                notificationService.pushTodayRecommendation(user.getId(), meal);
                sent++;
            } catch (Exception e) {
                // 한 명 실패(추천 후보 없음/Gemini 오류 등)가 나머지를 막지 않게 격리
                skipped++;
                log.warn("끼니 추천 푸시 실패 userId={}, meal={}: {}", user.getId(), meal, e.getMessage());
            }
        }
        log.info("끼니 추천 푸시 완료 meal={}, 발송={}, 스킵={}", meal, sent, skipped);
    }
}
