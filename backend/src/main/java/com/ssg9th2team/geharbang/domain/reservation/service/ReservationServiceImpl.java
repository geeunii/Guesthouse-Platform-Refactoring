package com.ssg9th2team.geharbang.domain.reservation.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.UserCouponJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.entity.Payment;
import com.ssg9th2team.geharbang.domain.payment.repository.jpa.PaymentJpaRepository;
import com.ssg9th2team.geharbang.domain.payment.service.PaymentService;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationRequestDto;
import com.ssg9th2team.geharbang.domain.reservation.dto.ReservationResponseDto;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.global.lock.DistributedLock;
import com.ssg9th2team.geharbang.domain.chat.repository.RealtimeChatRoomRepository;
import com.ssg9th2team.geharbang.domain.chat.RealtimeChatRoom;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

        private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);
        private final ReservationJpaRepository reservationRepository;
        private final AccommodationJpaRepository accommodationRepository;
        private final AccommodationMapper accommodationMapper;
        private final UserRepository userRepository;
        private final ReviewJpaRepository reviewJpaRepository;
        private final PaymentService paymentService;
        private final RoomJpaRepository roomJpaRepository;
        private final PaymentJpaRepository paymentRepository;
        private final WaitlistService waitlistService;
        private final RealtimeChatRoomRepository realtimeChatRoomRepository;
        private final UserCouponJpaRepository userCouponJpaRepository;
        private final CouponJpaRepository couponJpaRepository;

        @Override
        @DistributedLock(key = "'reservation:room:' + #requestDto.roomId() + ':date:' + #requestDto.checkin().toString().substring(0,10)")
        @Transactional
        public ReservationResponseDto createReservation(ReservationRequestDto requestDto) {
                // JWT 토큰에서 인증된 사용자 정보 추출
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));
                Long userId = user.getId();

                log.debug("DEBUG: createReservation called for user: {} (ID: {})", email, userId);

                // Room ID 필수 확인
                if (requestDto.roomId() == null) {
                        throw new IllegalArgumentException("Room ID is required for reservation.");
                }

                // [동시성 제어] Redis 분산 락으로 동시성 제어 (메서드 레벨 @DistributedLock)
                com.ssg9th2team.geharbang.domain.room.entity.Room room = roomJpaRepository
                                .findById(requestDto.roomId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "객실을 찾을 수 없습니다: " + requestDto.roomId()));

                // Instant를 LocalDate로 변환 (UTC 기준 - 분산 락 키와 동일한 시간대 사용)
                java.time.LocalDate checkinDate = java.time.LocalDateTime.ofInstant(
                                requestDto.checkin(), java.time.ZoneOffset.UTC).toLocalDate();
                java.time.LocalDate checkoutDate = java.time.LocalDateTime.ofInstant(
                                requestDto.checkout(), java.time.ZoneOffset.UTC).toLocalDate();

                // 1년(365일) 이후 예약 제한 (UTC 기준)
                if (checkinDate.isAfter(java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(365))) {
                        throw new IllegalArgumentException("예약은 오늘부터 365일 이내만 가능합니다.");
                }

                // 시간 강제 설정: 체크인 15:00, 체크아웃 11:00
                java.time.LocalDateTime checkinDateTime = checkinDate.atTime(15, 0);
                java.time.LocalDateTime checkoutDateTime = checkoutDate.atTime(11, 0);

                // [정원 기반 재고 관리] 날짜가 겹치는 기존 예약의 총 인원 조회
                Integer reservedGuestCount = reservationRepository.sumGuestCountByRoomIdAndDateRange(
                                requestDto.roomId(), checkinDateTime, checkoutDateTime);

                // 잔여 정원 계산 및 검증
                int maxGuests = room.getMaxGuests() != null ? room.getMaxGuests() : 0;
                int remainingCapacity = maxGuests - reservedGuestCount;

                if (requestDto.guestCount() > remainingCapacity) {
                        throw new IllegalStateException(
                                        "정원 초과: 해당 날짜의 남은 정원은 " + remainingCapacity + "명입니다. (최대 정원: " + maxGuests
                                                        + "명) 미결제 예약은 10분 후 자동 취소됩니다. 대기 목록에 등록하시면 빈자리 발생 시 이메일로 알려드립니다.");
                }

                // 숙박 박수 계산
                int stayNights = (int) ChronoUnit.DAYS.between(
                                checkinDateTime.toLocalDate(),
                                checkoutDateTime.toLocalDate());

                // 쿠폰 할인액 (서버에서 검증/계산)
                int couponDiscount = 0;
                if (requestDto.userCouponId() != null) {
                        UserCoupon userCoupon = userCouponJpaRepository.findById(requestDto.userCouponId())
                                        .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

                        if (!userCoupon.getUserId().equals(userId)) {
                                throw new IllegalArgumentException("본인의 쿠폰만 사용할 수 있습니다.");
                        }

                        if (userCoupon.getStatus() != UserCouponStatus.ISSUED) {
                                throw new IllegalArgumentException("사용할 수 없는 쿠폰입니다.");
                        }

                        if (userCoupon.getExpiredAt() != null
                                        && userCoupon.getExpiredAt().isBefore(LocalDateTime.now())) {
                                throw new IllegalArgumentException("만료된 쿠폰입니다.");
                        }

                        Coupon coupon = couponJpaRepository.findById(userCoupon.getCouponId())
                                        .orElseThrow(() -> new IllegalArgumentException("쿠폰 정보를 찾을 수 없습니다."));

                        Integer minPrice = coupon.getMinPrice();
                        if (minPrice != null && requestDto.totalAmount() < minPrice) {
                                throw new IllegalArgumentException("최소 결제 금액을 충족하지 못했습니다.");
                        }

                        if (coupon.getAccommodation() != null
                                        && !coupon.getAccommodation().getAccommodationsId()
                                                        .equals(requestDto.accommodationsId())) {
                                throw new IllegalArgumentException("해당 숙소 전용 쿠폰입니다.");
                        }

                        couponDiscount = calculateCouponDiscount(coupon, requestDto.totalAmount());
                }

                // 최종 결제 금액 계산
                int finalAmount = Math.max(0, requestDto.totalAmount() - couponDiscount);

                Reservation reservation = Reservation.builder()
                                .accommodationsId(requestDto.accommodationsId())
                                .roomId(requestDto.roomId())
                                .userId(userId)
                                .checkin(checkinDateTime)
                                .checkout(checkoutDateTime)
                                .stayNights(stayNights)
                                .guestCount(requestDto.guestCount())
                                .reservationStatus(0) // 0: 결제 대기
                                .totalAmountBeforeDc(requestDto.totalAmount())
                                .userCouponId(requestDto.userCouponId())
                                .couponDiscountAmount(couponDiscount)
                                .finalPaymentAmount(finalAmount)
                                .paymentStatus(0) // 0: 미결제
                                .reserverName(requestDto.reserverName())
                                .reserverPhone(requestDto.reserverPhone())
                                .build();

                Reservation saved = reservationRepository.save(reservation);

                // 채팅방 자동 생성 (별도 트랜잭션으로 분리)
                Accommodation accommodation = accommodationRepository.findById(requestDto.accommodationsId())
                                .orElseThrow(() -> new IllegalArgumentException("숙소를 찾을 수 없습니다."));
                createChatRoomForReservation(saved, accommodation, userId);

                // Accommodation 정보 조회 (이름/주소 반환을 위해)
                String accName = accommodation.getAccommodationsName();
                String accAddress = accommodation.getCity() + " " + accommodation.getDistrict() + " "
                                + accommodation.getAddressDetail();

                return ReservationResponseDto.from(saved, accName, accAddress);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void createChatRoomForReservation(Reservation reservation, Accommodation accommodation,
                        Long guestUserId) {
                try {
                        String imageUrl = accommodationMapper.selectMainImageUrl(accommodation.getAccommodationsId());

                        RealtimeChatRoom chatRoom = RealtimeChatRoom.builder()
                                        .reservationId(reservation.getId())
                                        .accommodationId(accommodation.getAccommodationsId())
                                        .accommodationName(accommodation.getAccommodationsName())
                                        .accommodationImage(imageUrl)
                                        .hostUserId(accommodation.getUserId())
                                        .guestUserId(guestUserId)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                        realtimeChatRoomRepository.save(chatRoom);
                        log.info("채팅방 생성 완료. reservationId={}, chatRoomId={}", reservation.getId(), chatRoom.getId());

                } catch (Exception e) {
                        log.error("CRITICAL: 채팅방 자동 생성에 실패했습니다. 예약은 정상적으로 완료되었습니다. reservationId={}",
                                        reservation.getId(), e);
                        // 필요시 알림 서비스 호출
                }
        }

        private int calculateCouponDiscount(Coupon coupon, int totalAmount) {
                if (coupon == null || coupon.getDiscountValue() == null) {
                        return 0;
                }

                if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
                        int discount = (int) Math.floor(totalAmount * (coupon.getDiscountValue() / 100.0));
                        Integer maxDiscount = coupon.getMaxDiscount();
                        return maxDiscount != null ? Math.min(discount, maxDiscount) : discount;
                }

                return coupon.getDiscountValue();
        }

        @Override
        public ReservationResponseDto getReservationById(Long reservationId) {
                Reservation reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

                Accommodation accommodation = accommodationRepository
                                .findById(reservation.getAccommodationsId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "숙소를 찾을 수 없습니다: " + reservation.getAccommodationsId()));

                String address = accommodation.getCity() + " " + accommodation.getDistrict() + " "
                                + accommodation.getAddressDetail();

                // Payment에서 paymentMethod 조회
                String paymentMethod = paymentRepository.findByReservationId(reservationId)
                                .map(Payment::getPaymentMethod)
                                .orElse(null);

                return ReservationResponseDto.from(reservation, accommodation.getAccommodationsName(), address, null,
                                false, paymentMethod);
        }

        @Override
        public List<ReservationResponseDto> getReservationsByUserId(Long userId) {
                return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                                .stream()
                                .map(ReservationResponseDto::from)
                                .toList();
        }

        @Override
        public List<ReservationResponseDto> getReservationsByAccommodationId(Long accommodationsId) {
                return reservationRepository.findByAccommodationsId(accommodationsId)
                                .stream()
                                .map(ReservationResponseDto::from)
                                .toList();
        }

        @Override
        public List<ReservationResponseDto> getMyReservations() {
                // JWT 토큰에서 인증된 사용자 정보 추출
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다: " + email));

                Long userId = user.getId();

                // 사용자의 예약 목록 조회 (숙소 정보 + 이미지 + 리뷰 작성 여부 포함)
                // DB 레벨에서 결제 완료된 예약만 조회 (reservationStatus >= 2: 확정 이상)
                return reservationRepository.findCompletedReservationsByUserIdOrderByCreatedAtDesc(userId)
                                .stream()
                                .map(reservation -> {
                                        Accommodation accommodation = accommodationRepository
                                                        .findById(reservation.getAccommodationsId()).orElse(null);

                                        String accName = (accommodation != null) ? accommodation.getAccommodationsName()
                                                        : null;
                                        String accAddress = (accommodation != null)
                                                        ? accommodation.getCity() + " " + accommodation.getDistrict()
                                                                        + " " + accommodation.getAddressDetail()
                                                        : null;

                                        // 숙소 대표 이미지 조회 (sort_order = 0)
                                        String imageUrl = accommodationMapper
                                                        .selectMainImageUrl(reservation.getAccommodationsId());

                                        // 해당 숙소에 리뷰 작성 여부 확인
                                        Boolean hasReview = reviewJpaRepository
                                                        .existsByUserIdAndAccommodationsIdAndIsDeletedFalse(
                                                                        userId, reservation.getAccommodationsId());

                                        return ReservationResponseDto.from(reservation, accName, accAddress, imageUrl,
                                                        hasReview);
                                })
                                .toList();
        }

        @Override
        @Transactional
        public void deletePendingReservation(Long reservationId) {
                int deleted = reservationRepository.deletePendingReservation(reservationId);
                if (deleted == 0) {
                        throw new IllegalArgumentException("대기 상태의 예약을 찾을 수 없습니다: " + reservationId);
                }
        }

        @Override
        @Transactional
        public void deleteCompletedReservation(Long reservationId) {
                // 디버깅을 위해 예약 정보 조회
                Reservation r = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                System.out.println("DEBUG: Deleting reservation " + reservationId);
                System.out.println(" - Status: " + r.getReservationStatus());
                System.out.println(" - Checkout: " + r.getCheckout());
                System.out.println(" - Current Time: " + now);

                // 상태 체크
                boolean statusOk = (r.getReservationStatus() == 2 || r.getReservationStatus() == 0);
                // 시간 체크
                boolean timeOk = r.getCheckout().isBefore(now);

                if (!statusOk) {
                        throw new IllegalArgumentException(
                                        String.format("삭제 실패: 예약 상태가 '확정(2)' 또는 '대기(0)'가 아닙니다. (현재 상태: %d)",
                                                        r.getReservationStatus()));
                }

                if (!timeOk) {
                        throw new IllegalArgumentException(String.format(
                                        "삭제 실패: 체크아웃 시간이 아직 지나지 않았습니다. (체크아웃: %s, 현재: %s)", r.getCheckout(), now));
                }

                // 결제 데이터(Payment, PaymentRefund) 먼저 삭제 (FK 제약조건 해결) -> Soft Delete로 변경되면서 제거
                // (데이터 보존)
                // paymentService.deleteAllPaymentDataByReservationId(reservationId);

                // 현재 시간 기준으로 체크인이 지난 확정 예약만 삭제 가능
                int deleted = reservationRepository.deleteCompletedReservation(reservationId, now);
                if (deleted == 0) {
                        // 위 검증을 통과했는데 여기서 0이면 뭔가 이상함 (동시성 문제 등)
                        throw new IllegalArgumentException("이용 완료된 예약만 삭제할 수 있습니다. (DB 삭제 0건)");
                }
        }

        @Override
        @Transactional
        public void deleteCancelledReservation(Long reservationId) {
                Reservation r = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

                if (r.getReservationStatus() != 9) {
                        throw new IllegalArgumentException("취소된 예약만 삭제할 수 있습니다.");
                }

                int deleted = reservationRepository.deleteCancelledReservation(reservationId);
                if (deleted == 0) {
                        throw new IllegalArgumentException("예약 내역 삭제에 실패했습니다.");
                }
        }

        @Override
        @Transactional
        public int cleanupOldPendingReservations() {
                // 10분 전 시간 계산
                java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusMinutes(10);

                // 삭제 대상 조회
                List<Reservation> toBeDeleted = reservationRepository.findOldPendingReservations(cutoffTime);

                int deletedCount = reservationRepository.deleteOldPendingReservations(cutoffTime);

                // 대기자 알림 발송
                if (deletedCount > 0 && !toBeDeleted.isEmpty()) {
                        for (Reservation r : toBeDeleted) {
                                waitlistService.notifyWaitingUsers(r.getRoomId(), r.getCheckin(), r.getCheckout());
                        }
                }

                return deletedCount;
        }

        // 객실별 예약 조회
        @Override
        public List<ReservationResponseDto> getReservationByUserId(Long roomId) {
                List<Reservation> reservations = reservationRepository.findByRoomId(roomId);
                return reservations.stream()
                                .map(ReservationResponseDto::from)
                                .collect(Collectors.toList());
        }

        // 전체 예약 목록 조회 (관리자용)
        @Override
        public List<ReservationResponseDto> getAllReservations() {
                return reservationRepository.findAll().stream()
                                .map(ReservationResponseDto::from)
                                .collect(Collectors.toList());
        }
}
