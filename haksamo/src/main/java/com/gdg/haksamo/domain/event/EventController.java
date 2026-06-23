package com.gdg.haksamo.domain.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 목록 조회
    @GetMapping
    public ResponseEntity<List<EventDto.Response>> getEvents() {
        return ResponseEntity.ok(eventService.getEvents());
    }

    // 상세 조회
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto.Response> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    // 등록
    @PostMapping
    public ResponseEntity<EventDto.Response> createEvent(@RequestBody @Valid EventDto.Request request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    // 수정
    @PutMapping("/{eventId}")
    public ResponseEntity<EventDto.Response> updateEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid EventDto.Request request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    // 삭제
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}