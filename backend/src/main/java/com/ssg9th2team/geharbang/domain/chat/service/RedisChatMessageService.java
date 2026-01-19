package com.ssg9th2team.geharbang.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatMessageService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_MESSAGES_KEY = "chat:room:%d:messages";
    private static final String CHAT_READ_STATUS_KEY = "chat:room:%d:read:%d"; // roomId:messageId
    private static final long MESSAGE_TTL_DAYS = 7; // 메시지 보관 기간 (7일)

    /**
     * 메시지 저장 (Sorted Set 사용 - 시간순 정렬)
     */
    public ChatMessageDto saveMessage(Long roomId, Long senderUserId, String senderName, String content) {
        String key = String.format(CHAT_MESSAGES_KEY, roomId);

        // 메시지 ID 생성 (timestamp + random)
        long messageId = System.currentTimeMillis() * 1000 + (long)(Math.random() * 1000);
        
        // 서버 시간대와 관계없이 한국 시간(KST)으로 저장
        ZoneId kstZoneId = ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(kstZoneId);
        
        // Score는 KST 기준의 타임스탬프로 설정
        double score = now.atZone(kstZoneId).toInstant().toEpochMilli();

        ChatMessageDto message = ChatMessageDto.builder()
                .id(messageId)
                .chatRoomId(roomId)
                .senderUserId(senderUserId)
                .senderName(senderName)
                .messageContent(content)
                .createdAt(now)
                .isRead(false)
                .build();

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForZSet().add(key, messageJson, score);

            // TTL 갱신 (새 메시지가 올 때마다 7일로 연장)
            redisTemplate.expire(key, MESSAGE_TTL_DAYS, TimeUnit.DAYS);

            log.info("Message saved to Redis. Room: {}, MessageId: {}", roomId, messageId);
            return message;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            throw new RuntimeException("Failed to save message", e);
        }
    }

    /**
     * 채팅방의 모든 메시지 조회 (시간순)
     */
    public List<ChatMessageDto> getMessages(Long roomId) {
        String key = String.format(CHAT_MESSAGES_KEY, roomId);

        Set<Object> messagesJson = redisTemplate.opsForZSet().range(key, 0, -1);

        if (messagesJson == null || messagesJson.isEmpty()) {
            return Collections.emptyList();
        }

        return messagesJson.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json.toString(), ChatMessageDto.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize message: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(msg -> msg != null)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 읽음 처리 - 해당 채팅방에서 특정 사용자가 보낸 메시지 제외하고 모두 읽음 처리
     */
    public void markMessagesAsRead(Long roomId, Long readerUserId) {
        String key = String.format(CHAT_MESSAGES_KEY, roomId);

        Set<Object> messagesJson = redisTemplate.opsForZSet().range(key, 0, -1);

        if (messagesJson == null || messagesJson.isEmpty()) {
            return;
        }

        for (Object json : messagesJson) {
            try {
                ChatMessageDto message = objectMapper.readValue(json.toString(), ChatMessageDto.class);

                // 내가 보낸 메시지가 아니고 아직 읽지 않은 경우
                if (!message.getSenderUserId().equals(readerUserId) && !Boolean.TRUE.equals(message.getIsRead())) {
                    // 기존 메시지 삭제
                    redisTemplate.opsForZSet().remove(key, json.toString());

                    // 읽음 처리된 메시지로 다시 저장
                    ChatMessageDto updatedMessage = ChatMessageDto.builder()
                            .id(message.getId())
                            .chatRoomId(message.getChatRoomId())
                            .senderUserId(message.getSenderUserId())
                            .senderName(message.getSenderName())
                            .messageContent(message.getMessageContent())
                            .createdAt(message.getCreatedAt())
                            .isRead(true)
                            .build();

                    ZoneId kstZoneId = ZoneId.of("Asia/Seoul");
                    double score = message.getCreatedAt().atZone(kstZoneId).toInstant().toEpochMilli();
                    String updatedJson = objectMapper.writeValueAsString(updatedMessage);
                    redisTemplate.opsForZSet().add(key, updatedJson, score);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to process message for read status: {}", e.getMessage());
            }
        }

        log.info("Messages marked as read. Room: {}, Reader: {}", roomId, readerUserId);
    }

    /**
     * 채팅방의 모든 메시지 삭제
     */
    public void deleteAllMessages(Long roomId) {
        String key = String.format(CHAT_MESSAGES_KEY, roomId);
        redisTemplate.delete(key);
        log.info("All messages deleted for room: {}", roomId);
    }

    /**
     * 오래된 메시지 정리 (스케줄러에서 호출)
     */
    public void cleanupOldMessages(Long roomId, int daysToKeep) {
        String key = String.format(CHAT_MESSAGES_KEY, roomId);
        ZoneId kstZoneId = ZoneId.of("Asia/Seoul");
        long cutoffTime = LocalDateTime.now(kstZoneId)
                .minusDays(daysToKeep)
                .atZone(kstZoneId)
                .toInstant()
                .toEpochMilli();

        Long removedCount = redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoffTime);
        if (removedCount != null && removedCount > 0) {
            log.info("Removed {} old messages from room {}", removedCount, roomId);
        }
    }
}
