package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component("aiSummaryClientFacade")
@Primary
@RequiredArgsConstructor
public class HostReviewAiSummaryFacade implements AiSummaryClient {

    private enum Provider {
        RULE,
        MOCK
    }

    private final MockAiSummaryClient mockAiSummaryClient;
    private final RuleBasedSummaryClient ruleBasedSummaryClient;

    @Value("${ai.summary.provider:RULE}")
    private String provider;

    @Override
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        Provider selected = parseProvider(provider);
        if (selected == Provider.MOCK) {
            log.info("AI summary provider=MOCK");
            return mockAiSummaryClient.generate(summary, request);
        }
        // Default to RULE provider
        log.info("AI summary provider=RULE");
        return ruleBasedSummaryClient.generate(summary, request);
    }

    private Provider parseProvider(String raw) {
        if (raw == null || raw.isBlank()) {
            return Provider.RULE;
        }
        try {
            return Provider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.info("AI summary provider=RULE fallback=none reason=unknown_provider");
            return Provider.RULE;
        }
    }
}
