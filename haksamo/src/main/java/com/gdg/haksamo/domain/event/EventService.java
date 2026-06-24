package com.gdg.haksamo.domain.event;

import com.gdg.haksamo.domain.user.entity.Role;
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

    // 등록 (관리자 전용)
    @Transactional
    public EventDto.Response createEvent(Long userId, EventDto.Request request) {
        requireAdmin(userId);

        Event event = Event.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return EventDto.Response.from(eventRepository.save(event));
    }

    // 수정 (관리자 전용)
    @Transactional
    public EventDto.Response updateEvent(Long userId, Long eventId, EventDto.Request request) {
        requireAdmin(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        event.update(request.getTitle(), request.getContent(), request.getImageUrl(),
                request.getStartDate(), request.getEndDate());
        return EventDto.Response.from(event);
    }

    // 삭제 (관리자 전용)
    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        requireAdmin(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        eventRepository.delete(event);
    }

    // 관리자(식당 운영자/운영팀 구분 없이) 권한 체크
    private void requireAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() == Role.USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
