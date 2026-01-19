package com.ssg9th2team.geharbang.domain.ai_agent.repository;

import com.ssg9th2team.geharbang.domain.ai_agent.entity.AgentChatRoom;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentChatRoomRepository extends JpaRepository<AgentChatRoom, Long> {

    List<AgentChatRoom> findByUserOrderByUpdatedAtDesc(User user);

    List<AgentChatRoom> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
