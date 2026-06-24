package com.gdg.haksamo.domain.preference;

public enum PreferenceKeyword {
    // 맛 취향
    SPICY("맛 취향", "매운 음식"),
    MILD("맛 취향", "순한 음식"),
    SWEET("맛 취향", "단 음식"),
    SOUR("맛 취향", "신 음식"),

    // 음식 종류
    NOODLE("음식 종류", "면류"),
    RICE("음식 종류", "밥류"),
    MEAT("음식 종류", "육류"),
    SEAFOOD("음식 종류", "해산물"),
    VEGETARIAN("음식 종류", "채식"),
    SOUP("음식 종류", "국/찌개"),
    SALAD("음식 종류", "샐러드"),
    SANDWICH("음식 종류", "빵/샌드위치"),

    // 요리 종류
    KOREAN("요리 종류", "한식"),
    CHINESE("요리 종류", "중식"),
    JAPANESE("요리 종류", "일식"),
    WESTERN("요리 종류", "양식"),

    // 건강 목표
    LOW_CALORIE("건강 목표", "저칼로리"),
    HIGH_PROTEIN("건강 목표", "고단백");

    /** 한 번에 선택 가능한 최대 키워드 수(= 전체 키워드 수). 요청 검증 상한으로 사용. 키워드 추가 시 함께 갱신. */
    public static final int MAX_SELECTION = 18;

    private final String category;
    private final String label;

    PreferenceKeyword(String category, String label) {
        this.category = category;
        this.label = label;
    }

    public String getCategory() {
        return category;
    }

    public String getLabel() {
        return label;
    }
}
