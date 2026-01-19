package com.ssg9th2team.geharbang.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatRoomDto;
import com.ssg9th2team.geharbang.domain.chat.service.RealtimeChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/realtime-chat")
@RequiredArgsConstructor
public class RealtimeChatController {

    private final RealtimeChatService chatService;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // 내 채팅방 목록 조회
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        return ResponseEntity.ok(chatService.getUserChatRooms(userId));
    }

    // 특정 채팅방 메시지 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        return ResponseEntity.ok(chatService.getRoomMessages(roomId, userId));
    }

    // 메시지 읽음 처리
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        chatService.markMessagesAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    // Redis 연결 테스트
    @GetMapping("/redis-test")
    public ResponseEntity<Map<String, Object>> testRedisConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Redis PING 테스트
            String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            result.put("status", "connected");
            result.put("ping", pong);

            // 간단한 Set/Get 테스트
            String testKey = "chat:test:" + System.currentTimeMillis();
            stringRedisTemplate.opsForValue().set(testKey, "test-value");
            String testValue = stringRedisTemplate.opsForValue().get(testKey);
            stringRedisTemplate.delete(testKey);

            result.put("setGetTest", testValue != null && testValue.equals("test-value") ? "success" : "failed");
            log.info("Redis connection test successful: {}", result);
        } catch (Exception e) {
            result.put("status", "disconnected");
            result.put("error", e.getMessage());
            log.error("Redis connection test failed: {}", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
