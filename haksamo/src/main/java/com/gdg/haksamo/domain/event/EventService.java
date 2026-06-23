package com.gdg.haksamo.domain.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
        return EventDto.Response.from(event);
    }

    // 등록
    @Transactional
    public EventDto.Response createEvent(EventDto.Request request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return EventDto.Response.from(eventRepository.save(event));
    }

    // 수정
    @Transactional
    public EventDto.Response updateEvent(Long eventId, EventDto.Request request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
        event.update(request.getTitle(), request.getContent(), request.getImageUrl(),
                request.getStartDate(), request.getEndDate());
        return EventDto.Response.from(event);
    }

    // 삭제
    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트입니다."));
        eventRepository.delete(event);
    }
}