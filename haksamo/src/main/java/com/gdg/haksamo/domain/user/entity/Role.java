package com.gdg.haksamo.domain.user.entity;

public enum Role {
    USER,              // 일반 학생
    RESTAURANT_ADMIN,  // 식당 운영자 — 자기 식당(managed_restaurant_id)만 관리
    SUPER_ADMIN        // 운영팀 — 전체 메뉴/리뷰/계정 관리
}