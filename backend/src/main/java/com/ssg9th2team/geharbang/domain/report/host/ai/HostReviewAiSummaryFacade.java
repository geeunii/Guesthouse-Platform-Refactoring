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
        OPENAI,
        MOCK
    }

    private final MockAiSummaryClient mockAiSummaryClient;
    private final OpenAiSummaryClient openAiSummaryClient;
    private final RuleBasedSummaryClient ruleBasedSummaryClient;

    @Value("${ai.summary.provider:RULE}")
    private String provider;

    @Override
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        Provider selected = parseProvider(provider);
        if (selected == Provider.MOCK) {
            log.info("AI summary provider=MOCK fallback=none");
            return mockAiSummaryClient.generate(summary, request);
        }
        if (selected == Provider.RULE) {
            log.info("AI summary provider=RULE fallback=none");
            return ruleBasedSummaryClient.generate(summary, request);
        }
        int reviewCount = summary.getReviewCount() != null ? summary.getReviewCount() : 0;
        if (reviewCount == 0) {
            log.info("AI summary provider=OPENAI fallback=RULE reason=insufficient_data");
            return ruleBasedSummaryClient.generate(summary, request);
        }
        if (!openAiSummaryClient.isConfigured()) {
            log.info("AI summary provider=OPENAI fallback=RULE reason=missing_api_key");
            return ruleBasedSummaryClient.generate(summary, request);
        }
        try {
            HostReviewAiSummaryResponse response = openAiSummaryClient.generate(summary, request);
            boolean hasContent = response.getOverview() != null && !response.getOverview().isEmpty();
            if (!hasContent) {
                throw new HostReportAiException("OpenAI response empty");
            }
            log.info("AI summary provider=OPENAI fallback=none");
            return response;
        } catch (HostReportAiException ex) {
            log.warn("AI summary provider=OPENAI fallback=RULE reason=request_failed", ex);
            return ruleBasedSummaryClient.generate(summary, request);
        } catch (RuntimeException ex) {
            log.error("AI summary provider=OPENAI failed with unexpected error.", ex);
            throw ex;
        }
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
