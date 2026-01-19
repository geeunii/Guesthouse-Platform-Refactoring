package com.ssg9th2team.geharbang.global.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ForwardController implements ErrorController {

    // SPA 라우팅: 특정 프론트엔드 라우트만 index.html로 포워딩
    // "/**" 대신 구체적인 경로 패턴 사용하여 무한 루프 방지
    @RequestMapping({
            "/login",
            "/signup",
            "/register",
            "/find-email",
            "/find-password",
            "/oauth2/redirect",
            "/social-signup",
            "/room/{id}",
            "/room/{id}/**",
            "/map",
            "/search",
            "/search/**",
            "/mypage",
            "/mypage/**",
            "/host",
            "/host/**",
            "/admin",
            "/admin/**",
            "/booking",
            "/booking/**",
            "/payment",
            "/payment/**",
            "/policy",
            "/policy/**",
            "/api/oauth2/login-disabled",
            "/profile",
            "/reservations",
            "/reservations/**",
            "/wishlist",
            "/coupons",
            "/reviews",
            "/write-review",
            "/delete-account",
            "/reset-password",
            "/reset-password-success",
            "/list"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        return "forward:/index.html";
    }
}
