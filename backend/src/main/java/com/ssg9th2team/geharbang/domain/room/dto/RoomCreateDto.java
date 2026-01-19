package com.ssg9th2team.geharbang.domain.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateDto {
    @NotNull
    private String roomName;

    @NotNull
    private Integer price;

    @NotNull
    private Integer weekendPrice;

    @NotNull
    private Integer minGuests;

    @NotNull
    private Integer maxGuests;

    @NotNull
    private String roomDescription;

    private String roomIntroduction;

    @NotNull
    private String mainImageUrl;

    @NotNull
    private Integer bathroomCount;

    @NotNull
    private String roomType;

    @NotNull
    private Integer bedCount;

    private Integer roomStatus;
}
