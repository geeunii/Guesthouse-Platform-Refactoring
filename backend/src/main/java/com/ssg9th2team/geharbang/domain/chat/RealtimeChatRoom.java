package com.ssg9th2team.geharbang.domain.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "realtime_chat_rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long reservationId;

    @Column(nullable = false)
    private Long accommodationId;

    private String accommodationName;
    private String accommodationImage;

    @Column(nullable = false)
    private Long hostUserId;

    @Column(nullable = false)
    private Long guestUserId;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    private LocalDateTime lastMessageTime;

    @Builder.Default
    private Integer hostUnreadCount = 0;

    @Builder.Default
    private Integer guestUnreadCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
