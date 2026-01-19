package com.ssg9th2team.geharbang.domain.ai_agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AgentChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private AgentChatRoom room;

    @Column(nullable = false, length = 10)
    private String role; // "user" or "model"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "accommodation_ids", length = 500)
    private String accommodationIds; // 추천된 숙소 ID들 (JSON array)

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
