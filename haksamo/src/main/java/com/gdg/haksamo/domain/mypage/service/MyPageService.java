package com.gdg.haksamo.domain.mypage.service;

import com.gdg.haksamo.domain.event.EventRepository;
import com.gdg.haksamo.domain.mypage.dto.FavoriteRestaurantResponse;
import com.gdg.haksamo.domain.mypage.dto.FavoriteRestaurantsUpdateRequest;
import com.gdg.haksamo.domain.mypage.dto.MyPageResponse;
import com.gdg.haksamo.domain.mypage.dto.NotificationSettingsResponse;
import com.gdg.haksamo.domain.mypage.dto.NotificationSettingsUpdateRequest;
import com.gdg.haksamo.domain.mypage.dto.PreferencesUpdateRequest;
import com.gdg.haksamo.domain.preference.PreferenceRepository;
import com.gdg.haksamo.domain.preference.PreferenceService;
import com.gdg.haksamo.domain.restaurant.FavoriteRestaurantRepository;
import com.gdg.haksamo.domain.restaurant.FavoriteRestaurantService;
import com.gdg.haksamo.domain.review.repository.ReviewHelpfulRepository;
import com.gdg.haksamo.domain.review.repository.ReviewRepository;
import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final FavoriteRestaurantService favoriteRestaurantService;
    private final PreferenceRepository preferenceRepository;
    private final PreferenceService preferenceService;
    private final EventRepository eventRepository;

    public MyPageResponse getMyPage(Long userId) {
        User user = getUser(userId);

        long reviewCount = reviewRepository.countByUserId(userId);
        long helpfulReceivedCount = reviewHelpfulRepository.countHelpfulReceivedByUserId(userId);
        long activeEventCount = eventRepository.countActiveEvents(LocalDate.now());

        List<FavoriteRestaurantResponse> favoriteRestaurants = favoriteRestaurantRepository.findByUser(user)
                .stream()
                .map(fr -> new FavoriteRestaurantResponse(
                        fr.getRestaurant().getRestaurantId(),
                        fr.getRestaurant().getName()
                ))
                .toList();

        List<String> preferenceKeywords = preferenceRepository.findByUser(user)
                .stream()
                .map(preference -> preference.getKeyword().getLabel())
                .toList();

        return new MyPageResponse(
                user.getNickname(),
                user.getDepartment(),
                reviewCount,
                helpfulReceivedCount,
                activeEventCount,
                favoriteRestaurants,
                preferenceKeywords,
                toNotificationSettingsResponse(user)
        );
    }

    @Transactional
    public void updateFavoriteRestaurants(Long userId, FavoriteRestaurantsUpdateRequest request) {
        User user = getUser(userId);
        favoriteRestaurantService.replaceFavorites(user, request.restaurantIds());
    }

    @Transactional
    public void updatePreferences(Long userId, PreferencesUpdateRequest request) {
        User user = getUser(userId);
        preferenceService.replaceKeywords(user, request.keywords());
    }

    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(Long userId, NotificationSettingsUpdateRequest request) {
        User user = getUser(userId);

        user.updateNotificationSettings(
                request.breakfast(),
                request.lunch(),
                request.dinner(),
                request.event(),
                request.pushNotificationEnabled()
        );

        return toNotificationSettingsResponse(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private NotificationSettingsResponse toNotificationSettingsResponse(User user) {
        return new NotificationSettingsResponse(
                user.isPushNotificationEnabled(),
                user.isNotificationBreakfastEnabled(),
                user.isNotificationLunchEnabled(),
                user.isNotificationDinnerEnabled(),
                user.isNotificationEventEnabled()
        );
    }
}
