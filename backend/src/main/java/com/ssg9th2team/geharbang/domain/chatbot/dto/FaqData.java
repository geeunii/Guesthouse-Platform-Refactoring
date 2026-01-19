package com.ssg9th2team.geharbang.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaqData {
    private FaqMeta meta;
    private List<FaqConversation> conversations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaqMeta {
        private String generatedAt;
        private String sourceFile;
        private String sheet;
        private int conversationCount;
        private int questionCount;
    }
}
