package com.gdg.haksamo.domain.event;

import com.gdg.haksamo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "이벤트")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 목록 조회", description = "등록된 공지/이벤트 카드 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<EventDto.Response>> getEvents() {
        return ApiResponse.success(eventService.getEvents());
    }

    @Operation(summary = "이벤트 상세 조회", description = "이벤트 1건의 상세 정보를 조회합니다.")
    @GetMapping("/{eventId}")
    public ApiResponse<EventDto.Response> getEvent(@PathVariable Long eventId) {
        return ApiResponse.success(eventService.getEvent(eventId));
    }

    @Operation(summary = "이벤트 등록", description = "운영팀(SUPER_ADMIN)이 공지/이벤트를 등록합니다.")
    @PostMapping
    public ApiResponse<EventDto.Response> createEvent(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid EventDto.Request request
    ) {
        return ApiResponse.success(eventService.createEvent(userId, request));
    }

    @Operation(summary = "이벤트 수정", description = "운영팀(SUPER_ADMIN)이 이벤트를 수정합니다.")
    @PutMapping("/{eventId}")
    public ApiResponse<EventDto.Response> updateEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid EventDto.Request request
    ) {
        return ApiResponse.success(eventService.updateEvent(userId, eventId, request));
    }

    @Operation(summary = "이벤트 삭제", description = "운영팀(SUPER_ADMIN)이 이벤트를 삭제합니다.")
    @DeleteMapping("/{eventId}")
    public ApiResponse<Void> deleteEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId
    ) {
        eventService.deleteEvent(userId, eventId);
        return ApiResponse.success();
    }
}
