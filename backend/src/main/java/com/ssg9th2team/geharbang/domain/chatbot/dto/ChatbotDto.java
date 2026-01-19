package com.ssg9th2team.geharbang.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ChatbotDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        private Long roomId;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRoomResponse {
        private Long id;
        private String lastMessage;
        private LocalDateTime updatedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatHistoryResponse {
        private String message;
        private boolean fromBot;
        private LocalDateTime createdAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendResponse {
        private String reply; // 단순 텍스트 응답 (하위 호환)
        private List<Bubble> bubbles; // 구조화된 bubble 응답
        private boolean success;
        private String errorMessage;
    }

    // 챗봇 응답 Bubble
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Bubble {
        private String type; // text, button, template, carousel
        private TextData text; // type이 text일 때
        private ButtonData button; // type이 button일 때
        private List<Bubble> buttons; // text 타입 내 버튼들
    }

    // text 타입 데이터
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextData {
        private String description;
        private String url;
        private String urlAlias;
    }

    // button 타입 데이터
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ButtonData {
        private String title;
        private String actionType; // postback, link, etc.
        private String actionData; // 액션 실행 데이터
    }

    // 네이버 클로바 API 요청 형식
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClovaRequest {
        private String version;
        private String userId;
        private long timestamp;
        private List<ClovaBubble> bubbles;
        private String event;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ClovaBubble {
            private String type;
            private ClovaData data;

            @Getter
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class ClovaData {
                private String description;
            }
        }
    }
}
