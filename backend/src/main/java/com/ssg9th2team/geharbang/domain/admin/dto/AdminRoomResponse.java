package com.ssg9th2team.geharbang.domain.admin.dto;

import com.ssg9th2team.geharbang.domain.room.entity.Room;

public record AdminRoomResponse(
        Long roomId,
        String roomName,
        Integer price,
        Integer weekendPrice,
        Integer minGuests,
        Integer maxGuests,
        String roomDescription,
        String mainImageUrl,
        Integer roomStatus,
        Integer bathroomCount,
        String roomType,
        Integer bedCount
) {
    public static AdminRoomResponse from(Room room) {
        return new AdminRoomResponse(
                room.getRoomId(),
                room.getRoomName(),
                room.getPrice(),
                room.getWeekendPrice(),
                room.getMinGuests(),
                room.getMaxGuests(),
                room.getRoomDescription(),
                room.getMainImageUrl(),
                room.getRoomStatus(),
                room.getBathroomCount(),
                room.getRoomType(),
                room.getBedCount()
        );
    }
}
