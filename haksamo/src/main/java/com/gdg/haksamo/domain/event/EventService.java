package com.gdg.haksamo.domain.event;

import com.gdg.haksamo.domain.user.entity.User;
import com.gdg.haksamo.domain.user.repository.UserRepository;
import com.gdg.haksamo.global.exception.BusinessException;
import com.gdg.haksamo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // 목록 조회
    @Transactional(readOnly = true)
    public List<EventDto.Response> getEvents() {
        return eventRepository.findAll()
                .stream()
                .map(EventDto.Response::from)
                .collect(Collectors.toList());
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public EventDto.Response getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        return EventDto.Response.from(event);
    }

    // 등록 (운영팀 SUPER_ADMIN 전용)
    @Transactional
    public EventDto.Response createEvent(Long userId, EventDto.Request request) {
        requireSuperAdmin(userId);

        Event event = Event.builder()
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return EventDto.Response.from(eventRepository.save(event));
    }

    // 수정 (운영팀 SUPER_ADMIN 전용)
    @Transactional
    public EventDto.Response updateEvent(Long userId, Long eventId, EventDto.Request request) {
        requireSuperAdmin(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        event.update(request.getType(), request.getTitle(), request.getContent(), request.getImageUrl(),
                request.getLinkUrl(), request.getStartDate(), request.getEndDate());
        return EventDto.Response.from(event);
    }

    // 삭제 (운영팀 SUPER_ADMIN 전용)
    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        requireSuperAdmin(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        eventRepository.delete(event);
    }

    // 이벤트는 app-wide 운영팀 콘텐츠(식당 소속 없음) → SUPER_ADMIN만 등록/수정/삭제
    private void requireSuperAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
