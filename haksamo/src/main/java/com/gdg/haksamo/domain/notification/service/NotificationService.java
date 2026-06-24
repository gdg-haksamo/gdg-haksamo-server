package com.gdg.haksamo.domain.notification.service;

import com.gdg.haksamo.domain.notification.dto.RecommendationPushResponse;
import com.gdg.haksamo.domain.notification.entity.Notification;
import com.gdg.haksamo.domain.notification.push.PushSender;
import com.gdg.haksamo.domain.notification.repository.NotificationRepository;
import com.gdg.haksamo.domain.recommendation.dto.TodayRecommendationResponse;
import com.gdg.haksamo.domain.recommendation.service.RecommendationService;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 알림(FCM 푸시) 발송.
 *
 * <p>추천 푸시는 <b>추천을 먼저 생성(또는 캐시 조회)한 뒤에만</b> 발송한다
 * ({@link RecommendationService#getToday}를 동기 호출). 따라서 "추천이 아직 없어서 빈 푸시가 나가는"
 * 상황은 발생하지 않는다 — 푸시 전 추천 존재가 보장된다.
 *
 * <p>이 메서드는 일부러 트랜잭션으로 감싸지 않는다. 외부 호출(Gemini 생성·FCM 발송)을 하나의 DB
 * 트랜잭션 안에 넣으면 외부 응답 지연 동안 커넥션을 점유해 풀이 고갈된다. 대신 각 단계가 자체 트랜잭션을
 * 갖는다: {@code getToday()}(@Transactional), {@code repository.save()}(Spring Data 자체 트랜잭션).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String PUSH_TITLE = "오늘의 학식 추천";

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushSender pushSender;

    /**
     * 대상 사용자의 오늘 추천을 생성(또는 캐시)한 뒤 즉시 FCM 푸시한다. (관리자 트리거)
     * 추천 내용을 응답에 함께 담아 관리자가 발송 내용을 확인할 수 있게 한다.
     */
    public RecommendationPushResponse pushTodayRecommendation(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1) 추천 생성 보장 — 없으면 생성(Gemini 1회), 있으면 캐시. 이 호출이 끝나야 푸시한다.
        TodayRecommendationResponse recommendation = recommendationService.getToday(targetUserId);

        String body = recommendation.menuName() + " — " + recommendation.reason();

        // 2) 발송 가능 여부 점검(끄지 않았는지/토큰 있는지)
        if (!user.isPushNotificationEnabled()) {
            return new RecommendationPushResponse(targetUserId, false, "대상 사용자가 알림을 꺼두었습니다.", recommendation);
        }
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return new RecommendationPushResponse(targetUserId, false, "대상 사용자에게 등록된 FCM 토큰이 없습니다.", recommendation);
        }

        // 3) 푸시 발송
        boolean delivered = pushSender.send(
                user.getFcmToken(),
                PUSH_TITLE,
                body,
                Map.of("type", "RECOMMENDATION", "menuId", String.valueOf(recommendation.menuId())));

        // 4) 발송 이력 적재(성공 여부와 무관하게 "보낸 시도"를 기록)
        notificationRepository.save(Notification.create(targetUserId, PUSH_TITLE, body));

        String detail = delivered
                ? "FCM 발송 완료"
                : "발송 처리됨(Firebase 미설정 시 로그만, 또는 토큰 무효)";
        return new RecommendationPushResponse(targetUserId, delivered, detail, recommendation);
    }
}
