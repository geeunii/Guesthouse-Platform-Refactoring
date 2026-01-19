package com.ssg9th2team.geharbang.domain.chat.repository;

import com.ssg9th2team.geharbang.domain.chat.RealtimeChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RealtimeChatRoomRepository extends JpaRepository<RealtimeChatRoom, Long> {
    Optional<RealtimeChatRoom> findByReservationId(Long reservationId);

    List<RealtimeChatRoom> findByGuestUserIdOrHostUserId(Long guestUserId, Long hostUserId);
}
