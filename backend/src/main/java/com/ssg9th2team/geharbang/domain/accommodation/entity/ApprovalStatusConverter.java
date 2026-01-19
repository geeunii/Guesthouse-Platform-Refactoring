package com.ssg9th2team.geharbang.domain.accommodation.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ApprovalStatusConverter implements AttributeConverter<ApprovalStatus, String> {

    @Override
    public String convertToDatabaseColumn(ApprovalStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public ApprovalStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING", "WAIT", "REQUEST" -> ApprovalStatus.PENDING;
            case "APPROVED", "APPROVE" -> ApprovalStatus.APPROVED;
            case "REJECTED", "REJECT", "DENIED" -> ApprovalStatus.REJECTED;
            default -> throw new IllegalArgumentException("Unknown approval status: " + dbData);
        };
    }
}
