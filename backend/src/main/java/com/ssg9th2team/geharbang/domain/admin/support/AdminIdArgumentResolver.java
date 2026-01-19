package com.ssg9th2team.geharbang.domain.admin.support;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;

@Component
@RequiredArgsConstructor
public class AdminIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final AdminIdentityResolver adminIdentityResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isAdminId = parameter.hasParameterAnnotation(AdminId.class);
        Class<?> type = parameter.getParameterType();
        return isAdminId && (Long.class.equals(type) || long.class.equals(type));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return adminIdentityResolver.resolveAdminUserId(authentication);
    }
}
