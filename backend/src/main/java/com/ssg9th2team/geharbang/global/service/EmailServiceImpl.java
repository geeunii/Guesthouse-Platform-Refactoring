package com.ssg9th2team.geharbang.global.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/// 이메일 서비스 관련 기능
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    /// 사용자에게 이메일 인증 코드 전송
    @Override
    public void sendVerificationEmail(String to, String verificationCode) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("게스트하우스 플랫폼 회원가입 이메일 인증 코드");

            // 이메일 내용 (HTML 형식)
            String htmlContent = "<html>" +
                    "<body style='font-family: Arial, sans-serif;'>" +
                    "<h2 style='color: #004d40;'>이메일 인증 코드 안내</h2>" +
                    "<p>안녕하세요!</p>" +
                    "<p><지금 이곳> 플랫폼 회원가입을 위한 인증 코드를 안내해 드립니다.</p>" +
                    "<p style='font-size: 24px; font-weight: bold; color: #00796b;'>" + verificationCode + "</p>" +
                    "<p>위 코드를 회원가입 페이지에 입력하여 이메일 인증을 완료해 주세요.</p>" +
                    "<p>본 코드는 발송 시점으로부터 3분간 유효합니다.</p>" +
                    "<p>감사합니다.</p>" +
                    "</body>" +
                    "</html>";
            helper.setText(htmlContent, true); // true 설정 시 HTML 형식으로 전송

            javaMailSender.send(mimeMessage);
            log.info("인증 이메일 전송 완료: {} (코드: {})", to, verificationCode);
        } catch (MessagingException e) {
            log.error("인증 이메일 전송 실패: {} (에러: {})", to, e.getMessage());
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    /**
     * 대기 목록 알림 이메일 발송
     * 빈자리가 발생했을 때 대기자에게 알림
     */
    @Override
    public void sendWaitlistNotificationEmail(String to, String accommodationName, String roomName,
            String checkin, String checkout) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("[지금 이곳] 대기하신 객실에 빈자리가 생겼습니다!");

            String htmlContent = "<html>" +
                    "<body style='font-family: Arial, sans-serif;'>" +
                    "<h2 style='color: #004d40;'>🎉 빈자리 알림</h2>" +
                    "<p>안녕하세요!</p>" +
                    "<p>대기하신 객실에 빈자리가 생겼습니다.</p>" +
                    "<div style='background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0;'>" +
                    "<p><strong>숙소:</strong> " + accommodationName + "</p>" +
                    "<p><strong>객실:</strong> " + roomName + "</p>" +
                    "<p><strong>날짜:</strong> " + checkin + " ~ " + checkout + "</p>" +
                    "</div>" +
                    "<p style='color: #e11d48;'><strong>⚠️ 다른 분들도 대기 중일 수 있으니 서둘러 예약하세요!</strong></p>" +
                    "<p>미결제 예약은 10분 후 자동 취소되므로 빈자리가 생길 수 있습니다.</p>" +
                    "<p>감사합니다.</p>" +
                    "</body>" +
                    "</html>";
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("대기 목록 알림 이메일 전송 완료: {} (숙소: {}, 객실: {})", to, accommodationName, roomName);
        } catch (MessagingException e) {
            log.error("대기 목록 알림 이메일 전송 실패: {} (에러: {})", to, e.getMessage());
            // 이메일 전송 실패해도 예외를 던지지 않음 (알림은 부가 기능)
        }
    }
}
