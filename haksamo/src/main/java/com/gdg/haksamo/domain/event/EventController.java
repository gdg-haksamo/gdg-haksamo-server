package com.gdg.haksamo.domain.event;

import com.gdg.haksamo.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 목록 조회
    @GetMapping
    public ApiResponse<List<EventDto.Response>> getEvents() {
        return ApiResponse.success(eventService.getEvents());
    }

    // 상세 조회
    @GetMapping("/{eventId}")
    public ApiResponse<EventDto.Response> getEvent(@PathVariable Long eventId) {
        return ApiResponse.success(eventService.getEvent(eventId));
    }

    // 등록 (관리자 전용)
    @PostMapping
    public ApiResponse<EventDto.Response> createEvent(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid EventDto.Request request
    ) {
        return ApiResponse.success(eventService.createEvent(userId, request));
    }

    // 수정 (관리자 전용)
    @PutMapping("/{eventId}")
    public ApiResponse<EventDto.Response> updateEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid EventDto.Request request
    ) {
        return ApiResponse.success(eventService.updateEvent(userId, eventId, request));
    }

    // 삭제 (관리자 전용)
    @DeleteMapping("/{eventId}")
    public ApiResponse<Void> deleteEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId
    ) {
        eventService.deleteEvent(userId, eventId);
        return ApiResponse.success();
    }
}
