package com.ssg9th2team.geharbang.domain.chatbot.repository;

import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatHistory;
import com.ssg9th2team.geharbang.domain.chatbot.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatHistory ch WHERE ch.chatRoom = :chatRoom")
    void deleteByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatHistory ch WHERE ch.chatRoom.id IN :chatRoomIds")
    void deleteByChatRoomIdIn(@Param("chatRoomIds") List<Long> chatRoomIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM chatbot_history WHERE chat_room_id IN (SELECT chat_room_id FROM chatbot_room WHERE user_id = :userId)", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") Long userId);
}
