package com.ssg9th2team.geharbang.domain.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.chatbot.dto.ChatbotDto;
import com.ssg9th2team.geharbang.domain.chatbot.dto.FaqConversation;
import com.ssg9th2team.geharbang.domain.chatbot.dto.FaqData;
import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatHistory;
import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatRoom;
import com.ssg9th2team.geharbang.domain.chatbot.repository.ChatHistoryRepository;
import com.ssg9th2team.geharbang.domain.chatbot.repository.ChatRoomRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatRoomRepository chatRoomRepository;
    private List<FaqConversation> faqList = new ArrayList<>();
    private String welcomeMessage = """
            안녕하세요 지금이곳 FAQ 챗봇 입니다. 무엇을 도와드릴까요?
            """;

    @PostConstruct
    public void init() {
        loadFaqData();
    }

    /**
     * JSON 파일에서 FAQ 데이터 로드
     */
    private void loadFaqData() {
        try {
            ClassPathResource resource = new ClassPathResource("chatbot/thismo_faq_conversations.json");
            InputStream inputStream = resource.getInputStream();

            ObjectMapper mapper = new ObjectMapper();
            FaqData faqData = mapper.readValue(inputStream, FaqData.class);

            this.faqList = faqData.getConversations();
            log.info("FAQ 데이터 로드 완료: {} 개의 대화", faqList.size());

        } catch (Exception e) {
            log.error("FAQ 데이터 로드 실패", e);
        }
    }

    /**
     * 채팅방 생성 (새 문의하기)
     */
    @Transactional
    public Long createChatRoom(User user) {
        // 방 생성
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .user(user)
                .lastMessage(welcomeMessage)
                .build());

        // 웰컴 메시지 저장
        chatHistoryRepository.save(ChatHistory.builder()
                .chatRoom(room)
                .message(welcomeMessage)
                .isBot(true)
                .build());

        return room.getId();
    }

    /**
     * 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatbotDto.ChatRoomResponse> getChatRooms(User user) {
        return chatRoomRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                .map(r -> ChatbotDto.ChatRoomResponse.builder()
                        .id(r.getId())
                        .lastMessage(r.getLastMessage())
                        .updatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 방의 메시지 조회
     */
    @Transactional(readOnly = true)
    public List<ChatbotDto.ChatHistoryResponse> getRoomMessages(Long roomId, User user) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return chatHistoryRepository.findByChatRoomOrderByCreatedAtAsc(room).stream()
                .map(h -> ChatbotDto.ChatHistoryResponse.builder()
                        .message(h.getMessage())
                        .fromBot(h.isBot())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatbotDto.SendResponse sendMessage(Long roomId, String userMessage, User user) {
        ChatRoom room = null;
        if (user != null && roomId != null) {
            room = chatRoomRepository.findById(roomId)
                    .filter(r -> r.getUser().getId().equals(user.getId()))
                    .orElse(null);
        }

        if (room != null) {
            chatHistoryRepository.save(ChatHistory.builder()
                    .chatRoom(room)
                    .message(userMessage)
                    .isBot(false)
                    .build());
        }

        String answer;
        try {
            answer = findAnswer(userMessage);
        } catch (Exception e) {
            log.error("메시지 처리 실패", e);
            answer = "죄송합니다. 오류가 발생했습니다.";
        }

        if (room != null) {
            chatHistoryRepository.save(ChatHistory.builder()
                    .chatRoom(room)
                    .message(answer)
                    .isBot(true)
                    .build());
            room.updateLastMessage(answer);
        }

        return ChatbotDto.SendResponse.builder().reply(answer).success(true).build();
    }

    /**
     * 채팅방 삭제
     */
    @Transactional
    public void deleteChatRoom(Long roomId, User user) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        chatHistoryRepository.deleteByChatRoom(room);
        chatRoomRepository.delete(room);
    }

    /**
     * 질문에 맞는 답변 찾기
     */
    /**
     * 질문에 맞는 답변 찾기
     */
    private String findAnswer(String userMessage) {
        String msg = normalize(userMessage);
        log.info("사용자 메시지: '{}' -> 정규화: '{}'", userMessage, msg);

        // 1. conversationName 정확 일치
        for (FaqConversation conv : faqList) {
            String convName = normalize(conv.getConversationName());
            if (convName.equals(msg)) {
                log.info("매칭 성공 (conversationName): {}", conv.getConversationName());
                return conv.getAnswer();
            }
        }

        // 2. questions 배열에서 정확 일치
        for (FaqConversation conv : faqList) {
            for (String question : conv.getQuestions()) {
                String normalizedQ = normalize(question);
                if (normalizedQ.equals(msg)) {
                    log.info("매칭 성공 (question exact): {} -> {}", question, conv.getConversationName());
                    return conv.getAnswer();
                }
            }
        }

        // 3. questions 배열에서 부분 일치 (사용자 입력이 question에 포함)
        FaqConversation bestMatch = null;
        int bestScore = 0;

        for (FaqConversation conv : faqList) {
            for (String question : conv.getQuestions()) {
                String q = normalize(question);
                // 키워드 매칭 (공백 무시)
                if (q.contains(msg) || msg.contains(q)) {
                    int score = Math.min(msg.length(), q.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = conv;
                    }
                }
            }
        }

        if (bestMatch != null) {
            log.info("매칭 성공 (partial): {} (score: {})", bestMatch.getConversationName(), bestScore);
            return bestMatch.getAnswer();
        }

        log.warn("매칭 실패: {} (FAQ 목록 크기: {})", userMessage, faqList.size());
        // 디버깅용: 처음 몇 개의 질문만 로깅
        if (!faqList.isEmpty()) {
            FaqConversation first = faqList.get(0);
            log.info("첫 번째 데이터 확인 - 이름: {}, 질문들: {}", first.getConversationName(), first.getQuestions());
            log.info("첫 번째 질문 정규화 확인: '{}'", normalize(first.getQuestions().get(0)));
        }

        // 4. 매칭 실패 - 기본 메뉴 안내
        return "죄송합니다. 답변을 찾을 수 없습니다.\n\n" + welcomeMessage;
    }

    /**
     * 문자열 정규화 (소문자 변환 + 공백 제거)
     */
    private String normalize(String str) {
        return str.trim().toLowerCase().replaceAll("\\s+", "");
    }
}
