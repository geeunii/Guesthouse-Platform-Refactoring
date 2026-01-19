package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.reservation.entity.Waitlist;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.WaitlistJpaRepository;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.global.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WaitlistService {

    private final WaitlistJpaRepository waitlistRepository;
    private final UserRepository userRepository;
    private final RoomJpaRepository roomRepository;
    private final AccommodationJpaRepository accommodationRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /**
     * 대기 목록 등록
     * - 1인당 최대 3개 제한
     * - 중복 대기 등록 확인
     */
    @Transactional
    public Long registerWaitlist(Long roomId, Long accommodationId, LocalDateTime checkin,
            LocalDateTime checkout, Integer guestCount) {
        // 현재 로그인 사용자 정보 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));

        // accommodationId가 null이면 roomId로 Room을 조회하여 가져오기
        Long resolvedAccommodationId = accommodationId;
        if (resolvedAccommodationId == null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("객실을 찾을 수 없습니다: " + roomId));
            resolvedAccommodationId = room.getAccommodationsId();
        }

        // 1인당 대기 등록 개수 제한 확인
        int currentWaitlistCount = waitlistRepository.countByUserIdAndIsNotifiedFalse(user.getId());
        if (currentWaitlistCount >= Waitlist.MAX_WAITLIST_PER_USER) {
            throw new IllegalStateException(
                    "대기 등록은 최대 " + Waitlist.MAX_WAITLIST_PER_USER + "개까지만 가능합니다. " +
                            "현재 등록 수: " + currentWaitlistCount);
        }

        // 중복 대기 등록 확인
        if (waitlistRepository.existsByUserIdAndRoomIdAndCheckinAndCheckoutAndIsNotifiedFalse(
                user.getId(), roomId, checkin, checkout)) {
            throw new IllegalStateException("이미 대기 등록되어 있습니다.");
        }

        Waitlist waitlist = Waitlist.builder()
                .userId(user.getId())
                .roomId(roomId)
                .accommodationsId(resolvedAccommodationId)
                .checkin(checkin)
                .checkout(checkout)
                .guestCount(guestCount)
                .build();

        Waitlist saved = waitlistRepository.save(waitlist);
        log.info("대기 목록 등록: userId={}, roomId={}, date={} ~ {}, 현재 대기 수: {}",
                user.getId(), roomId, checkin, checkout, currentWaitlistCount + 1);
        return saved.getId();
    }

    /**
     * 대기 목록 취소
     */
    @Transactional
    public void cancelWaitlist(Long waitlistId) {
        Waitlist waitlist = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new IllegalArgumentException("대기 정보를 찾을 수 없습니다: " + waitlistId));

        // 본인 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));

        if (!waitlist.getUserId().equals(user.getId())) {
            throw new IllegalStateException("본인의 대기 목록만 취소할 수 있습니다.");
        }

        waitlistRepository.delete(waitlist);
        log.info("대기 목록 취소: waitlistId={}", waitlistId);
    }

    /**
     * 빈자리 발생 시 대기자에게 알림 발송
     * - 체크인 7일 이상 전: 이메일 발송 + 24시간 예약 기회
     * - 체크인 7일 미만: 대기 자동 삭제 (알림 없음)
     */
    @Transactional
    public void notifyWaitingUsers(Long roomId, LocalDateTime checkin, LocalDateTime checkout) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minCheckinDate = now.plusDays(Waitlist.MIN_DAYS_BEFORE_CHECKIN);

        // 1. 7일 미만인 대기 자동 삭제 (알림 없이)
        List<Waitlist> expiredWaitlists = waitlistRepository.findExpiredBeforeMinDays(
                roomId, checkin, checkout, minCheckinDate);

        if (!expiredWaitlists.isEmpty()) {
            waitlistRepository.deleteAll(expiredWaitlists);
            log.info("체크인 7일 미만 대기 삭제: roomId={}, 삭제 수={}", roomId, expiredWaitlists.size());
        }

        // 2. 7일 이상 전인 대기자에게만 알림 발송
        List<Waitlist> eligibleWaitlists = waitlistRepository.findEligibleForNotification(
                roomId, checkin, checkout, minCheckinDate);

        if (eligibleWaitlists.isEmpty()) {
            return;
        }

        // 대기자의 이메일 정보 일괄 조회 (N+1 방지)
        java.util.Set<Long> userIds = eligibleWaitlists.stream()
                .map(Waitlist::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, String> userEmails = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getEmail));

        // 객실 및 숙소 정보 조회
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.warn("객실 정보를 찾을 수 없습니다: roomId={}", roomId);
            return;
        }

        Accommodation accommodation = accommodationRepository.findById(room.getAccommodationsId()).orElse(null);
        String accommodationName = accommodation != null ? accommodation.getAccommodationsName() : "숙소";
        String roomName = room.getRoomName() != null ? room.getRoomName() : "객실";

        // 대기자들에게 알림 발송 (24시간 예약 기회 부여)
        for (Waitlist waitlist : eligibleWaitlists) {
            String email = userEmails.get(waitlist.getUserId());
            if (email == null) {
                log.warn("대기자 사용자 정보를 찾을 수 없음: userId={}", waitlist.getUserId());
                continue;
            }
            try {
                emailService.sendWaitlistNotificationEmail(
                        email,
                        accommodationName,
                        roomName,
                        checkin.format(DATE_FORMATTER),
                        checkout.format(DATE_FORMATTER));
                waitlist.markAsNotified(now); // 일관된 알림 시각 사용 (메서드 진입 시점 now)
                log.info("대기자 알림 발송 완료: email={}, roomId={}, 만료시각={}",
                        email, roomId, waitlist.getExpiresAt());
            } catch (Exception e) {
                log.error("대기자 알림 발송 실패: email={}, error={}", email, e.getMessage());
            }
        }
    }

    /**
     * 24시간 만료된 알림 처리
     * - 만료된 대기 삭제
     * - 다음 대기자에게 기회 부여는 별도 로직 필요 시 확장
     */
    @Transactional
    public int processExpiredNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Waitlist> expiredList = waitlistRepository.findExpiredNotifications(now);

        if (expiredList.isEmpty()) {
            return 0;
        }

        waitlistRepository.deleteAll(expiredList);
        log.info("24시간 만료 대기 삭제: {}건", expiredList.size());
        return expiredList.size();
    }

    /**
     * 지난 체크인 날짜의 대기 정리
     */
    @Transactional
    public int cleanupPastCheckinWaitlists() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = waitlistRepository.deleteByCheckinBefore(now);
        if (deleted > 0) {
            log.info("체크인 지난 대기 삭제: {}건", deleted);
        }
        return deleted;
    }

    /**
     * 30일 이상 된 오래된 대기 정리
     */
    @Transactional
    public int cleanupOldWaitlists() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        int deleted = waitlistRepository.deleteOldWaitlist(cutoffTime);
        if (deleted > 0) {
            log.info("30일 이상 오래된 대기 삭제: {}건", deleted);
        }
        return deleted;
    }
}
