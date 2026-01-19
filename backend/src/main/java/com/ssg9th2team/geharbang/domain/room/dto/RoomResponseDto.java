package com.ssg9th2team.geharbang.domain.room.dto;

import com.ssg9th2team.geharbang.domain.room.entity.Room;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDto {
    private Long roomId;
    private Long accommodationsId;
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
    private LocalDateTime createRoom;
    private LocalDateTime changeInfoRoom;

}
