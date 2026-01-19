package com.ssg9th2team.geharbang.domain.auth.spec;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRole(UserRole role) {
        if (role == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<User> keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.where(null);
        }
        String normalized = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), normalized),
                cb.like(cb.lower(root.get("phone")), normalized)
        );
    }

    public static Specification<User> suspendedEquals(Boolean suspended) {
        if (suspended == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("suspended"), suspended);
    }

    public static Specification<User> hostApprovedEquals(Boolean approved) {
        if (approved == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.equal(root.get("hostApproved"), approved);
    }

    public static Specification<User> hostApprovedIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("hostApproved"));
    }
}
