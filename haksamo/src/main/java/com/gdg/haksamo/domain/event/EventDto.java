package com.gdg.haksamo.domain.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventDto {

    @Getter
    public static class Request {
        @NotNull(message = "이벤트/공지 구분은 필수입니다.")
        private EventType type;

        @NotBlank(message = "제목은 필수입니다.")
        private String title;

        private String content;
        private String imageUrl;
        private String linkUrl;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long eventId;
        private EventType type;
        private String title;
        private String content;
        private String imageUrl;
        private String linkUrl;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime createdAt;

        public static Response from(Event event) {
            return Response.builder()
                    .eventId(event.getEventId())
                    .type(event.getType())
                    .title(event.getTitle())
                    .content(event.getContent())
                    .imageUrl(event.getImageUrl())
                    .linkUrl(event.getLinkUrl())
                    .startDate(event.getStartDate())
                    .endDate(event.getEndDate())
                    .createdAt(event.getCreatedAt())
                    .build();
        }
    }
}
