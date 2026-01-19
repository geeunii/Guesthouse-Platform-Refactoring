package com.ssg9th2team.geharbang.domain.ai.vision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VisionLabel {
    private final String description;
    private final float score;
}
