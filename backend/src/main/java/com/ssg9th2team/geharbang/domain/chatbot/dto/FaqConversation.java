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
public class FaqConversation {
    private String conversationName;
    private String tag;
    private List<String> questions;
    private String answer;
}
