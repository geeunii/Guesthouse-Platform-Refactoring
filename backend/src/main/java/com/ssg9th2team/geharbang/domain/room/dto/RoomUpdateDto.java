package com.ssg9th2team.geharbang.domain.room.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateDto {
    private String roomName;
    private Integer price;
    private Integer weekendPrice;
    private Integer minGuests;
    private Integer maxGuests;
    private String roomDescription;
    private String roomIntroduction;
    private String mainImageUrl;
    private Integer roomStatus;
    private Integer bathroomCount;
    private String roomType;
    private Integer bedCount;
}
