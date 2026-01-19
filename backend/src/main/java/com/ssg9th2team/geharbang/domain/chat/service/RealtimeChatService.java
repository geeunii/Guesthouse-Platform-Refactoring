package com.ssg9th2team.geharbang.domain.chat.service;

import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.chat.RealtimeChatRoom;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatMessageDto;
import com.ssg9th2team.geharbang.domain.chat.dto.ChatRoomDto;
import com.ssg9th2team.geharbang.domain.chat.repository.RealtimeChatRoomRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeChatService {

    private final RealtimeChatRoomRepository chatRoomRepository;
    private final RedisChatMessageService redisChatMessageService; // Redis로 변경
    private final UserRepository userRepository;
    private final AccommodationMapper accommodationMapper;
    private final RedisPublisher redisPublisher;
    private final ChannelTopic channelTopic;
    private final SimpMessagingTemplate messagingTemplate;

    // 유저의 채팅방 목록 조회
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(Long userId) {
        log.info("사용자 채팅방 목록 조회 시작. 사용자 ID: {}", userId);
        List<RealtimeChatRoom> rooms = chatRoomRepository.findByGuestUserIdOrHostUserId(userId, userId);
        log.info("사용자 ID {}에 대해 {}개의 채팅방을 찾았습니다.", userId, rooms.size());

        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }

        // 모든 참여자(호스트, 게스트) ID 수집
        Set<Long> allUserIds = rooms.stream()
                .flatMap(room -> Stream.of(room.getHostUserId(), room.getGuestUserId()))
                .collect(Collectors.toSet());
        log.info("모든 참여자 사용자 ID 목록: {}", allUserIds);

        Map<Long, User> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        log.info("사용자 정보 {}건을 찾았습니다.", userMap.size());

        // N+1 문제 해결: 모든 숙소의 대표 이미지를 한 번에 조회
        List<Long> accommodationIds = rooms.stream()
                .map(RealtimeChatRoom::getAccommodationId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> imageMap = accommodationMapper.selectMainImagesByAccommodationIds(accommodationIds)
                .stream()
                .collect(Collectors.toMap(
                        img -> img.getAccommodationsId(),
                        img -> img.getImageUrl(),
                        (existing, replacement) -> existing // 중복 시 첫 번째 값 유지
                ));
        log.info("숙소 대표 이미지 {}건을 일괄 조회했습니다.", imageMap.size());

        List<ChatRoomDto> chatRoomDtos = rooms.stream().map(room -> {
            Long otherUserId;
            String otherUserName;
            int unreadCount;

            if (userId.equals(room.getGuestUserId())) {
                otherUserId = room.getHostUserId();
                unreadCount = room.getGuestUnreadCount();
            } else {
                otherUserId = room.getGuestUserId();
                unreadCount = room.getHostUnreadCount();
            }

            User otherUser = userMap.get(otherUserId);
            otherUserName = (otherUser != null) ? otherUser.getNickname() : "알 수 없는 사용자";

            // 호스트와 게스트 정보 조회
            User hostUser = userMap.get(room.getHostUserId());
            User guestUser = userMap.get(room.getGuestUserId());

            String hostName = (hostUser != null) ? hostUser.getNickname() : "알 수 없는 호스트";
            String guestName = (guestUser != null) ? guestUser.getNickname() : "알 수 없는 게스트";
            String hostProfileImage = null; // User 엔티티에 프로필 이미지 없음, 필요시 추가
            String guestProfileImage = null;

            String currentImageUrl = imageMap.get(room.getAccommodationId());

            return ChatRoomDto.builder()
                .id(room.getId())
                .reservationId(room.getReservationId())
                .accommodationId(room.getAccommodationId())
                .accommodationName(room.getAccommodationName())
                .accommodationImage(currentImageUrl) // Use the fresh URL
                .hostUserId(room.getHostUserId())
                .guestUserId(room.getGuestUserId())
                .hostName(hostName)
                .guestName(guestName)
                .hostProfileImage(hostProfileImage)
                .guestProfileImage(guestProfileImage)
                .otherParticipantName(otherUserName)
                .lastMessage(room.getLastMessage())
                .lastMessageTime(room.getLastMessageTime())
                .unreadCount(unreadCount)
                .build();
        }).collect(Collectors.toList());

        // 최신 메시지 시간 기준으로 정렬 (내림차순 - 최신이 먼저)
        chatRoomDtos.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        log.info("사용자 ID {}에게 {}개의 채팅방 DTO를 반환합니다.", userId, chatRoomDtos.size());
        return chatRoomDtos;
    }

    // 특정 채팅방의 메시지 조회 (Redis에서 조회)
    public List<ChatMessageDto> getRoomMessages(Long roomId, Long currentUserId) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));

        // 사용자가 해당 채팅방의 참여자인지 확인
        if (!chatRoom.getHostUserId().equals(currentUserId) && !chatRoom.getGuestUserId().equals(currentUserId)) {
            throw new SecurityException("User is not a participant of this chat room");
        }

        // Redis에서 메시지 조회
        return redisChatMessageService.getMessages(roomId);
    }

    // 메시지 저장 (Redis에 저장)
    @Transactional
    public ChatMessageDto saveMessage(Long roomId, Long senderUserId, String content) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));

        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // Redis에 메시지 저장
        ChatMessageDto savedMessage = redisChatMessageService.saveMessage(
                roomId, senderUserId, sender.getNickname(), content);

        // 채팅방 정보 업데이트 (마지막 메시지, 시간, 안 읽은 메시지 수)
        chatRoom.setLastMessage(content);
        chatRoom.setLastMessageTime(savedMessage.getCreatedAt());

        // 메시지를 보낸 사람이 아닌 다른 참여자의 unreadCount 증가 및 수신자 ID 확인
        Long recipientUserId = null;
        int newUnreadCount = 0;
        if (chatRoom.getHostUserId().equals(senderUserId)) {
            newUnreadCount = chatRoom.getGuestUnreadCount() + 1;
            chatRoom.setGuestUnreadCount(newUnreadCount);
            recipientUserId = chatRoom.getGuestUserId();
        } else if (chatRoom.getGuestUserId().equals(senderUserId)) {
            newUnreadCount = chatRoom.getHostUnreadCount() + 1;
            chatRoom.setHostUnreadCount(newUnreadCount);
            recipientUserId = chatRoom.getHostUserId();
        }
        chatRoom.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        chatRoomRepository.save(chatRoom);

        // 수신자에게 새 메시지 알림 전송 (채팅방 목록 업데이트용)
        if (recipientUserId != null) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_MESSAGE");
            notification.put("roomId", roomId);
            notification.put("unreadCount", newUnreadCount);
            notification.put("lastMessage", content);
            notification.put("lastMessageTime", savedMessage.getCreatedAt().toString());
            notification.put("senderName", sender.getNickname());

            log.info("Sending notification to user {}: {}", recipientUserId, notification);
            messagingTemplate.convertAndSend("/topic/user/" + recipientUserId + "/notifications", notification);
        }

        return savedMessage;
    }

    // 메시지 읽음 처리 (Redis에서 처리)
    @Transactional
    public void markMessagesAsRead(Long roomId, Long readerUserId) {
        RealtimeChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));

        // 사용자가 해당 채팅방의 참여자인지 확인
        if (!chatRoom.getHostUserId().equals(readerUserId) && !chatRoom.getGuestUserId().equals(readerUserId)) {
            throw new SecurityException("User is not a participant of this chat room");
        }

        // Redis에서 읽음 처리
        redisChatMessageService.markMessagesAsRead(roomId, readerUserId);

        // 채팅방의 unreadCount를 0으로 초기화
        if (chatRoom.getHostUserId().equals(readerUserId)) {
            chatRoom.setHostUnreadCount(0);
        } else if (chatRoom.getGuestUserId().equals(readerUserId)) {
            chatRoom.setGuestUnreadCount(0);
        }
        chatRoomRepository.save(chatRoom);

        // 채팅방 전체에 읽음 알림 브로드캐스트 (해당 방을 구독 중인 모든 사용자가 수신)
        log.info("Broadcasting read receipt to room {} by user {}", roomId, readerUserId);
        redisPublisher.publish(
                channelTopic,
                Map.of(
                        "type", "MESSAGES_READ",
                        "roomId", roomId,
                        "readerId", readerUserId
                )
        );
    }
}
