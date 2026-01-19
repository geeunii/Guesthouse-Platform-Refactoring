package com.ssg9th2team.geharbang.domain.room.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseListDto {
    private Long roomId;
    private String roomName;
    private String roomDescription;
    private String roomIntroduction; // Added
    private int maxGuests;
    private int minGuests; // Added
    private int price;
    private int weekendPrice;
    private String mainImageUrl;
    private int bathroomCount; // Added
    private int bedCount; // Added
    private int roomStatus; // Added
}
