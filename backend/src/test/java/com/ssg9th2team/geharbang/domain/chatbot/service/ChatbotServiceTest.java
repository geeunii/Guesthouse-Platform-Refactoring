package com.ssg9th2team.geharbang.domain.chatbot.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.chatbot.dto.ChatbotDto;
import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatHistory;
import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatRoom;
import com.ssg9th2team.geharbang.domain.chatbot.repository.ChatHistoryRepository;
import com.ssg9th2team.geharbang.domain.chatbot.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        // 실제 FAQ 파일 로드 (src/main/resources/chatbot/thismo_faq_conversations.json)
        // 단위 테스트 환경에서도 접근 가능해야 함
        chatbotService.init();
    }

    @Test
    @DisplayName("채팅방 생성 테스트")
    void createChatRoom() {
        // given
        User user = mock(User.class);
        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoom.getId()).willReturn(1L);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);

        // when
        Long roomId = chatbotService.createChatRoom(user);

        // then
        assertThat(roomId).isEqualTo(1L);
        verify(chatRoomRepository).save(any(ChatRoom.class)); // 방 저장 확인
        verify(chatHistoryRepository).save(any(ChatHistory.class)); // 웰컴 메시지 저장 확인
    }

    @Test
    @DisplayName("메시지 전송 - 예약 문의 (정확 매칭)")
    void sendMessage_ExactMatch() {
        // given
        Long roomId = 1L;
        String userMessage = "예약은 어떻게 하나요?";
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);

        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatRoom.getUser()).willReturn(user); // 방 주인 확인

        // when
        ChatbotDto.SendResponse response = chatbotService.sendMessage(roomId, userMessage, user);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getReply()).contains("예약"); // 예상 답변 포함 확인

        // 1. 유저 메시지 저장
        // 2. 봇 답변 저장
        verify(chatHistoryRepository, org.mockito.Mockito.times(2)).save(any(ChatHistory.class));
    }

    @Test
    @DisplayName("메시지 전송 - 매칭 실패 시 기본 답변")
    void sendMessage_NoMatch() {
        // given
        Long roomId = 1L;
        String userMessage = "알 수 없는 질문입니다~~~";
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);

        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatRoom.getUser()).willReturn(user);

        // when
        ChatbotDto.SendResponse response = chatbotService.sendMessage(roomId, userMessage, user);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getReply()).contains("죄송합니다"); // 기본 에러/미매칭 메시지
    }

    @Test
    @DisplayName("채팅방 삭제 테스트")
    void deleteChatRoom() {
        // given
        Long roomId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);

        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatRoom.getUser()).willReturn(user);

        // when
        chatbotService.deleteChatRoom(roomId, user);

        // then
        verify(chatHistoryRepository).deleteByChatRoom(chatRoom);
        verify(chatRoomRepository).delete(chatRoom);
    }
}
