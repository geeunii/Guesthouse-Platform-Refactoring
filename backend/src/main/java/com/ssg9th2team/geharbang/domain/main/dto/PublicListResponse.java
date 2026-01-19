package com.ssg9th2team.geharbang.domain.main.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PublicListResponse(
        List<ListDto> items,
        PublicPageMeta page
) {
    public static PublicListResponse of(List<ListDto> items, Page<?> page) {
        return new PublicListResponse(
                items,
                new PublicPageMeta(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.hasNext()
                )
        );
    }
}
