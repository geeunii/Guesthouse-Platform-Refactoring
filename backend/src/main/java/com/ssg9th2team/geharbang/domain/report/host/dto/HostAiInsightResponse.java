package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HostAiInsightResponse {
    private String engine;
    private boolean fallbackUsed;
    private String generatedAt;
    private List<HostAiInsightSection> sections;
    private HostAiInsightMeta meta;
}
