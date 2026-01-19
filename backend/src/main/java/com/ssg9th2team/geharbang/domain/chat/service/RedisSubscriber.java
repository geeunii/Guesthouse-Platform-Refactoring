package com.ssg9th2team.geharbang.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis에서 메시지가 발행(publish)되면 대기하고 있던 onMessage가 해당 메시지를 받아 처리한다.
     */
    public void sendMessage(String publishMessage) {
        try {
            log.info("Redis received message: {}", publishMessage);
            JsonNode jsonNode = objectMapper.readTree(publishMessage);
            Long roomId = null;
            Object messageToSend = null;

            if (jsonNode.has("type") && "MESSAGES_READ".equals(jsonNode.get("type").asText())) {
                // 읽음 처리 알림 (Map)
                roomId = jsonNode.get("roomId").asLong();
                messageToSend = objectMapper.treeToValue(jsonNode, Map.class);
            } else {
                // 일반 채팅 메시지 (ChatMessageDto)
                // ChatMessageDto에는 type 필드가 없으므로 이를 기본값으로 간주
                if (jsonNode.has("chatRoomId")) {
                    roomId = jsonNode.get("chatRoomId").asLong();
                    messageToSend = objectMapper.treeToValue(jsonNode, ChatMessageDto.class);
                }
            }

            if (roomId != null && messageToSend != null) {
                // WebSocket 구독자에게 채팅방별로 메시지 전송
                log.info("Sending message to WS topic /topic/chatroom/{}: {}", roomId, messageToSend);
                messagingTemplate.convertAndSend("/topic/chatroom/" + roomId, messageToSend);
                log.info("Message sent successfully to /topic/chatroom/{}", roomId);
            } else {
                log.warn("Invalid message format received from Redis. RoomId or Message is null. Raw: {}", publishMessage);
            }

        } catch (Exception e) {
            log.error("Exception in RedisSubscriber while processing message: {}. Error: {}", publishMessage, e.getMessage(), e);
        }
    }
}
