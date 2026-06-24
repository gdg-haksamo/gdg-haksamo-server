package com.gdg.haksamo.domain.event;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventDto {

    @Getter
    public static class Request {
        @NotBlank(message = "제목은 필수입니다.")
        private String title;
        private String content;
        private String imageUrl;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long eventId;
        private String title;
        private String content;
        private String imageUrl;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime createdAt;

        public static Response from(Event event) {
            return Response.builder()
                    .eventId(event.getEventId())
                    .title(event.getTitle())
                    .content(event.getContent())
                    .imageUrl(event.getImageUrl())
                    .startDate(event.getStartDate())
                    .endDate(event.getEndDate())
                    .createdAt(event.getCreatedAt())
                    .build();
        }
    }
}
