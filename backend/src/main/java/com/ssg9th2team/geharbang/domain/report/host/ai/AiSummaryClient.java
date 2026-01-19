package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;

public interface AiSummaryClient {
    HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request);
}
