package com.ssg9th2team.geharbang.domain.ai_agent.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class AiAgentDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRoomResponse {
        private Long id;
        private String title;
        private String lastMessage;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private Long id;
        private String role;
        private String content;
        private List<AccommodationSummary> recommendedAccommodations;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String message;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private String reply;
        private List<AccommodationSummary> recommendedAccommodations;
        private boolean success;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccommodationSummary {
        private Long id;
        private String name;
        private String city;
        private String district;
        private Double rating;
        private Integer reviewCount;
        private Integer minPrice;
        private String thumbnailUrl;
        private List<String> themes;
    }
}
