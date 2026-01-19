package com.ssg9th2team.geharbang.domain.chatbot.controller;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.chatbot.dto.ChatbotDto;
import com.ssg9th2team.geharbang.domain.chatbot.service.ChatbotService;
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
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final UserRepository userRepository;

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }

    @PostMapping("/room")
    public ResponseEntity<Map<String, Long>> createRoom(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Long roomId = chatbotService.createChatRoom(user);
        return ResponseEntity.ok(Collections.singletonMap("roomId", roomId));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatbotDto.ChatRoomResponse>> getRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatbotService.getChatRooms(user));
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatbotDto.ChatHistoryResponse>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatbotService.getRoomMessages(roomId, user));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatbotDto.SendResponse> sendMessage(
            @RequestBody ChatbotDto.SendRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(chatbotService.sendMessage(request.getRoomId(), request.getMessage(), user));
    }

    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        chatbotService.deleteChatRoom(roomId, user);
        return ResponseEntity.ok().build();
    }
}
