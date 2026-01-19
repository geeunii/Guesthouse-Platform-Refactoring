package com.ssg9th2team.geharbang.domain.ai_agent.repository;

import com.ssg9th2team.geharbang.domain.ai_agent.entity.AgentChatMessage;
import com.ssg9th2team.geharbang.domain.ai_agent.entity.AgentChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentChatMessageRepository extends JpaRepository<AgentChatMessage, Long> {

    List<AgentChatMessage> findByRoomOrderByCreatedAtAsc(AgentChatRoom room);

    List<AgentChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    void deleteByRoom(AgentChatRoom room);
}
