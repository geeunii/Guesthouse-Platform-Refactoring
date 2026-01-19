package com.ssg9th2team.geharbang.domain.report.host.dto;

import com.ssg9th2team.geharbang.domain.report.host.ai.HostAiInsightEligibilityResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostAiInsightEligibilityResponse {
    private HostAiInsightEligibilityResult review;
    private HostAiInsightEligibilityResult theme;
    private HostAiInsightEligibilityResult demand;
}
