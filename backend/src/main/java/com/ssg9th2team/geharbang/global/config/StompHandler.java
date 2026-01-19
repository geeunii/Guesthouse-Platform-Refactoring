package com.ssg9th2team.geharbang.global.config;

import com.ssg9th2team.geharbang.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String AUTHENTICATION_KEY = "AUTHENTICATION";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // STOMP 연결 요청일 때 JWT 토큰 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Authorization 헤더에서 토큰 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // 토큰 유효성 검사
                if (jwtTokenProvider.validateToken(token)) {
                    // 토큰으로부터 Authentication 객체 생성
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);

                    // STOMP 세션에 사용자 정보 설정
                    accessor.setUser(authentication);
                    accessor.getSessionAttributes().put(AUTHENTICATION_KEY, authentication);

                    log.info("STOMP user authenticated: {}", authentication.getName());
                } else {
                    log.warn("STOMP connection failed: Invalid JWT token");
                    throw new IllegalArgumentException("Invalid JWT token");
                }
            } else {
                log.warn("STOMP connection failed: No Authorization header");
                throw new IllegalArgumentException("No Authorization header");
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                   StompCommand.SEND.equals(accessor.getCommand())) {
            // SUBSCRIBE, SEND 등의 메시지는 세션의 인증 정보를 가져와서 헤더에 설정
            Authentication authentication = null;

            // 방법 1: User로 설정된 Authentication 가져오기
            if (accessor.getUser() instanceof Authentication) {
                authentication = (Authentication) accessor.getUser();
            }

            // 방법 2: 세션 속성에서 가져오기
            if (authentication == null && accessor.getSessionAttributes() != null) {
                authentication = (Authentication) accessor.getSessionAttributes().get(AUTHENTICATION_KEY);
            }

            if (authentication != null) {
                // 인증 정보를 헤더에 설정 (스레드가 바뀌어도 유지됨)
                accessor.setUser(authentication);
                accessor.setHeader("simpUser", authentication);

                // SecurityContext에도 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Restored authentication for user: {} (command: {})",
                         authentication.getName(), accessor.getCommand());

                // 수정된 메시지 반환
                return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
            } else {
                // 인증 정보가 없으면 거부
                log.warn("Unauthenticated {} command attempted", accessor.getCommand());
                throw new IllegalArgumentException("Authentication required for " + accessor.getCommand());
            }
        }
        return message;
    }
}
