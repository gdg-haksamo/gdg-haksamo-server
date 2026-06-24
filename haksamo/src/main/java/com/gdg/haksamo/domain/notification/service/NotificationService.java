package com.gdg.haksamo.domain.notification.service;

import com.gdg.haksamo.domain.menu.entity.MealTime;
import com.gdg.haksamo.domain.notification.dto.DemoPushRequest;
import com.gdg.haksamo.domain.notification.dto.DemoPushResponse;
import com.gdg.haksamo.domain.notification.dto.RecommendationPushResponse;
import com.gdg.haksamo.domain.notification.entity.Notification;
import com.gdg.haksamo.domain.notification.push.PushSender;
import com.gdg.haksamo.domain.notification.repository.NotificationRepository;
import com.gdg.haksamo.domain.recommendation.dto.AdhocRecommendation;
import com.gdg.haksamo.domain.recommendation.dto.TodayRecommendationResponse;
import com.gdg.haksamo.domain.recommendation.service.RecommendationService;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 알림(FCM 푸시) 발송.
 *
 * <p>추천 푸시는 <b>추천을 먼저 생성(또는 캐시 조회)한 뒤에만</b> 발송한다
 * ({@link RecommendationService#getToday}를 동기 호출). 따라서 "추천이 아직 없어서 빈 푸시가 나가는"
 * 상황은 발생하지 않는다 — 푸시 전 추천 존재가 보장된다. 문구는 끼니·식당·메뉴를 담는다.
 *
 * <p>이 메서드는 일부러 트랜잭션으로 감싸지 않는다. 외부 호출(Gemini 생성·FCM 발송)을 하나의 DB
 * 트랜잭션 안에 넣으면 외부 응답 지연 동안 커넥션을 점유해 풀이 고갈된다. 대신 각 단계가 자체 트랜잭션을
 * 갖는다: {@code getToday()}(@Transactional), {@code repository.save()}(Spring Data 자체 트랜잭션).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushSender pushSender;

    /**
     * 대상 사용자의 해당 끼니 추천을 생성(또는 캐시)한 뒤 즉시 FCM 푸시한다. (관리자 트리거/스케줄러 공용)
     * meal이 null이면 현재 시각 기준 끼니. 추천 내용을 응답에 함께 담아 발송 내용을 확인할 수 있게 한다.
     */
    public RecommendationPushResponse pushTodayRecommendation(Long targetUserId, MealTime meal) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1) 추천 생성 보장 — 없으면 생성(Gemini 1회), 있으면 캐시. 이 호출이 끝나야 푸시한다.
        TodayRecommendationResponse recommendation = recommendationService.getToday(targetUserId, meal);

        String title = "오늘의 " + recommendation.meal().label() + " 추천";
        String body = buildBody(recommendation.meal().label(), recommendation.restaurant(), recommendation.menuName());

        // 2) 발송 가능 여부 점검 — 마스터 + 해당 끼니 토글을 함께 본다(끼니별 off면 발송 안 함).
        if (!user.isMealNotificationEnabled(recommendation.meal())) {
            return new RecommendationPushResponse(targetUserId, false,
                    "대상 사용자가 " + recommendation.meal().label() + " 알림을 꺼두었습니다.", recommendation);
        }
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return new RecommendationPushResponse(targetUserId, false, "대상 사용자에게 등록된 FCM 토큰이 없습니다.", recommendation);
        }

        // 3) 푸시 발송
        boolean delivered = pushSender.send(
                user.getFcmToken(),
                title,
                body,
                Map.of("type", "RECOMMENDATION", "menuId", String.valueOf(recommendation.menuId())));

        // 4) 발송 이력 적재(성공 여부와 무관하게 "보낸 시도"를 기록)
        notificationRepository.save(Notification.create(targetUserId, title, body));

        String detail = delivered
                ? "FCM 발송 완료"
                : "발송 처리됨(Firebase 미설정 시 로그만, 또는 토큰 무효)";
        return new RecommendationPushResponse(targetUserId, delivered, detail, recommendation);
    }

    /**
     * 데모용: 입력(날짜·끼니·선호 키워드·선호 식당)으로 추천 4개를 생성하고 1순위를 즉시 푸시한다.
     * 유저의 저장된 선호와 무관하게 시연용 시나리오를 구성한다. 추천은 저장하지 않는다(무상태).
     */
    public DemoPushResponse demoPush(DemoPushRequest request) {
        User user = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate date = (request.date() != null) ? request.date() : LocalDate.now(KST);
        List<String> favorites = (request.favoriteRestaurant() != null && !request.favoriteRestaurant().isBlank())
                ? List.of(request.favoriteRestaurant())
                : List.of();

        List<AdhocRecommendation> recommendations =
                recommendationService.generateAdhoc(date, request.meal(), request.likedKeywords(), favorites);
        AdhocRecommendation top = recommendations.get(0);

        String title = "오늘의 " + request.meal().label() + " 추천";
        String body = buildBody(request.meal().label(), top.restaurant(), top.menuName());

        if (!user.isMealNotificationEnabled(request.meal())) {
            return new DemoPushResponse(false,
                    "대상 사용자가 " + request.meal().label() + " 알림을 꺼두었습니다.", recommendations);
        }
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return new DemoPushResponse(false, "대상 사용자에게 등록된 FCM 토큰이 없습니다.", recommendations);
        }

        boolean delivered = pushSender.send(
                user.getFcmToken(), title, body,
                Map.of("type", "RECOMMENDATION", "menuId", String.valueOf(top.menuId())));
        notificationRepository.save(Notification.create(request.targetUserId(), title, body));

        String detail = delivered ? "FCM 발송 완료" : "발송 처리됨(Firebase 미설정 시 로그만, 또는 토큰 무효)";
        return new DemoPushResponse(delivered, detail, recommendations);
    }

    /** "오늘 점심으로 OO식당의 OO 어때요?" (식당명이 없으면 식당 부분 생략) */
    private String buildBody(String mealLabel, String restaurant, String menuName) {
        if (restaurant != null && !restaurant.isBlank()) {
            return "오늘 " + mealLabel + "으로 " + restaurant + "의 " + menuName + " 어때요?";
        }
        return "오늘 " + mealLabel + "으로 " + menuName + " 어때요?";
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
}
