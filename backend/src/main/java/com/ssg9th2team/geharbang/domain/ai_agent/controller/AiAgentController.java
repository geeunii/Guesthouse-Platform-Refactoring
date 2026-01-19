package com.ssg9th2team.geharbang.domain.ai_agent.controller;

import com.ssg9th2team.geharbang.domain.ai_agent.dto.AiAgentDto;
import com.ssg9th2team.geharbang.domain.ai_agent.service.AiAgentService;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai-agent")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService aiAgentService;
    private final UserRepository userRepository;

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }

    /**
     * 새 대화방 생성
     */
    @PostMapping("/rooms")
    public ResponseEntity<Map<String, Long>> createRoom(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Long roomId = aiAgentService.createRoom(user);
        log.info("AI Agent 대화방 생성: roomId={}, userId={}", roomId, user.getId());
        return ResponseEntity.ok(Collections.singletonMap("roomId", roomId));
    }

    /**
     * 대화방 목록 조회
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<AiAgentDto.ChatRoomResponse>> getRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiAgentService.getRooms(user));
    }

    /**
     * 특정 대화방의 메시지 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<AiAgentDto.MessageResponse>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiAgentService.getMessages(roomId, user));
    }

    /**
     * 메시지 전송 및 AI 응답 받기
     */
    @PostMapping("/rooms/{roomId}/chat")
    public ResponseEntity<AiAgentDto.ChatResponse> chat(
            @PathVariable Long roomId,
            @RequestBody AiAgentDto.ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("AI Agent 채팅: roomId={}, message={}", roomId, request.getMessage());
        AiAgentDto.ChatResponse response = aiAgentService.chat(roomId, request.getMessage(), user);
        return ResponseEntity.ok(response);
    }

    /**
     * 대화방 삭제
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        aiAgentService.deleteRoom(roomId, user);
        log.info("AI Agent 대화방 삭제: roomId={}", roomId);
        return ResponseEntity.ok().build();
    }
}
