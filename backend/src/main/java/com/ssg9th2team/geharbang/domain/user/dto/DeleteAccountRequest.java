package com.ssg9th2team.geharbang.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DeleteAccountRequest {
    private List<String> reasons;
    private String otherReason;
}
