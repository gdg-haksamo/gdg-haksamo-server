package com.gdg.haksamo.domain.preference;

import com.gdg.haksamo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 선호 키워드 영속 로직. 회원가입(가입 3단계)과 마이페이지 수정이 공유한다.
 */
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    /**
     * 사용자의 선호 키워드를 주어진 목록으로 전체 교체한다.
     * keywords가 null이거나 비어 있으면 기존 키워드를 모두 제거하고 끝낸다.
     * (user, keyword) 유니크 제약이 없으므로 중복 입력은 distinct로 정리해 행 중복을 막는다.
     */
    @Transactional
    public void replaceKeywords(User user, List<PreferenceKeyword> keywords) {
        preferenceRepository.deleteByUser(user);
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        List<Preference> preferences = keywords.stream()
                .distinct()
                .map(keyword -> Preference.builder()
                        .user(user)
                        .keyword(keyword)
                        .build())
                .toList();
        preferenceRepository.saveAll(preferences);
    }
}
