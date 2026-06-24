package com.gdg.haksamo.domain.user.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 관리자 계정 목록(페이지) 응답.
 */
public record AdminUserListResponse(
        List<AdminUserResponse> users,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminUserListResponse from(Page<AdminUserResponse> page) {
        return new AdminUserListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}