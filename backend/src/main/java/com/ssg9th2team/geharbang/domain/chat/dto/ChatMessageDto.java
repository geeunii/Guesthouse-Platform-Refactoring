package com.ssg9th2team.geharbang.domain.chat.dto;

import com.ssg9th2team.geharbang.domain.chat.RealtimeChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long chatRoomId;
    private Long senderUserId;
    private String senderName;
    private String messageContent;
    private LocalDateTime createdAt;
    private Boolean isRead;

    public static ChatMessageDto fromEntity(RealtimeChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoomId())
                .senderUserId(message.getSenderUserId())
                .senderName(message.getSenderName())
                .messageContent(message.getMessageContent())
                .createdAt(message.getCreatedAt())
                .isRead(message.getIsRead())
                .build();
    }
}
