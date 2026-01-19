package com.ssg9th2team.geharbang.domain.wishlist.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishlistDto {

    private Long wishId;
    private Long accommodationsId;
    private Long userId;
}
