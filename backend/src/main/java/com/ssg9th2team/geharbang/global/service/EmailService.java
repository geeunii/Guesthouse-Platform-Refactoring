package com.ssg9th2team.geharbang.global.service;

/// 이메일 관련 인터페이스
public interface EmailService {

    /// to = 이메일을 받을 주소 verivicationCode 전송할 인증 코드,
    void sendVerificationEmail(String to, String verificationCode);

    /**
     * 대기 목록 알림 이메일 발송
     * 
     * @param to                이메일 수신자
     * @param accommodationName 숙소명
     * @param roomName          객실명
     * @param checkin           체크인 날짜
     * @param checkout          체크아웃 날짜
     */
    void sendWaitlistNotificationEmail(String to, String accommodationName, String roomName,
            String checkin, String checkout);
}
