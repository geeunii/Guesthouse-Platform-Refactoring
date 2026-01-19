package com.ssg9th2team.geharbang.domain.main.dto;

import java.util.List;

public record AvailableRoomResponse(List<Long> availableRoomIds) {
    public static AvailableRoomResponse of(List<Long> roomIds) {
        return new AvailableRoomResponse(roomIds == null ? List.of() : roomIds);
    }
}
