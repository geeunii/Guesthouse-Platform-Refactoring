package com.ssg9th2team.geharbang.domain.chat.repository;

import com.ssg9th2team.geharbang.domain.chat.RealtimeChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealtimeChatMessageRepository extends JpaRepository<RealtimeChatMessage, Long> {
    List<RealtimeChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    @Modifying
    @Query("UPDATE RealtimeChatMessage m SET m.isRead = true WHERE m.chatRoomId = :roomId AND m.senderUserId <> :readerId AND m.isRead = false")
    int markMessagesAsReadForRoomAndUser(@Param("roomId") Long roomId, @Param("readerId") Long readerId);

    // 사용자가 특정 채팅방에서 보낸 메시지가 아닌 메시지 중 읽지 않은 메시지 수
    long countByChatRoomIdAndSenderUserIdIsNotAndIsReadFalse(Long chatRoomId, Long senderUserId);

    @Modifying
    @Query("DELETE FROM RealtimeChatMessage m WHERE m.chatRoomId = :chatRoomId")
    void deleteByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Modifying
    @Query("DELETE FROM RealtimeChatMessage m WHERE m.chatRoomId IN :chatRoomIds")
    void deleteByChatRoomIdIn(@Param("chatRoomIds") List<Long> chatRoomIds);
}
