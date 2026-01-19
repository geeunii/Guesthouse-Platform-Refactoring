package com.ssg9th2team.geharbang.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageRequest;
import com.ssg9th2team.geharbang.domain.chat.service.RealtimeChatService;
import com.ssg9th2team.geharbang.domain.chat.service.RedisPublisher;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RealtimeChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChatWebSocketController.class);
    private final RealtimeChatService chatService;
    private final UserRepository userRepository;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;

    // XSS 방지를 위한 HTML Sanitizer 정책 설정 (기본 포맷팅 및 링크 허용, 스크립트 제거)
    private final PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // 메시지 헤더에서 인증 정보 가져오기
        Authentication authentication = (Authentication) headerAccessor.getUser();

        log.info("Received message for room {}, user: {}, content: {}",
                roomId, authentication != null ? authentication.getName() : "NULL", request.getContent());

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated user attempted to send message to room {}", roomId);
            return;
        }

        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
            Long senderUserId = user.getId();

            // XSS 방지를 위해 메시지 내용 정제 (Sanitize)
            String sanitizedContent = policy.sanitize(request.getContent());

            log.info("Saving message from user {} (ID: {}) to room {}", user.getEmail(), senderUserId, roomId);
            ChatMessageDto message = chatService.saveMessage(roomId, senderUserId, sanitizedContent);

            log.info("Publishing message to Redis topic {} for room {}", channelTopic.getTopic(), roomId);
            redisPublisher.publish(channelTopic, message);

            log.info("Message successfully published to Redis for room {}", roomId);
        } catch (Exception e) {
            log.error("Error sending message in room {}: {}", roomId, e.getMessage(), e);
        }
    }
}