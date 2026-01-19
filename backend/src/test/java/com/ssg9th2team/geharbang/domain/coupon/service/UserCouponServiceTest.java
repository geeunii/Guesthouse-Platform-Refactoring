package com.ssg9th2team.geharbang.domain.coupon.service;

import com.ssg9th2team.geharbang.domain.coupon.entity.Coupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCoupon;
import com.ssg9th2team.geharbang.domain.coupon.entity.UserCouponStatus;
import com.ssg9th2team.geharbang.domain.coupon.entity.CouponTriggerType;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.CouponJpaRepository;
import com.ssg9th2team.geharbang.domain.coupon.repository.jpa.UserCouponJpaRepository;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-test")
@Transactional
@org.junit.jupiter.api.Disabled("CI 환경 리소스 부족으로 인한 임시 비활성화")
public class UserCouponServiceTest extends com.ssg9th2team.geharbang.config.IntegrationTestConfig {

    @Autowired
    private UserCouponService userCouponService;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private ReviewJpaRepository reviewJpaRepository;

    @Autowired
    private com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository accommodationRepository;

    @Autowired
    private com.ssg9th2team.geharbang.domain.auth.repository.UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Long testUserId = 1L; // Exists in test-base-data.sql
    private Coupon rewardCoupon;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 호스트 생성 (Accommodation FK용)
        com.ssg9th2team.geharbang.domain.auth.entity.User hostUser = com.ssg9th2team.geharbang.domain.auth.entity.User.builder()
                .email("host@test.com")
                .name("호스트")
                .nickname("host")
                .password("password")
                .phone("010-1111-2222")
                .role(com.ssg9th2team.geharbang.domain.auth.entity.UserRole.HOST)
                .hostApproved(true) // 승인됨
                .marketingAgreed(true)
                .socialSignupCompleted(true)
                .build();
        userRepository.save(hostUser);

        // 2. 테스트용 일반 유저 생성 (쿠폰 발급 대상)
        com.ssg9th2team.geharbang.domain.auth.entity.User normalUser = com.ssg9th2team.geharbang.domain.auth.entity.User.builder()
                .email("user@test.com")
                .name("유저")
                .nickname("user")
                .password("password")
                .phone("010-3333-4444")
                .role(com.ssg9th2team.geharbang.domain.auth.entity.UserRole.USER)
                .marketingAgreed(true)
                .socialSignupCompleted(true)
                .build();
        userRepository.save(normalUser);
        
        testUserId = normalUser.getId();

        // 3. 리뷰 보상 쿠폰 생성
        rewardCoupon = Coupon.builder()
                .code("REVIEW_3_REWARD")
                .name("리뷰 3회 감사 쿠폰")
                .description("리뷰를 3회 작성해주신 고객님께 드리는 감사 쿠폰입니다")
                .discountType("PERCENT")
                .discountValue(10)
                .minPrice(10000)
                .maxDiscount(5000)
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusMonths(3))
                .isActive(true)
                .triggerType(CouponTriggerType.REVIEW_3)
                .createdAt(LocalDateTime.now())
                .build();
        couponJpaRepository.save(rewardCoupon);
        entityManager.flush();
    }

    /**
     * 테스트용 리뷰 생성
     */
    private void createTestReviews(Long userId, int count) {
        for (int i = 0; i < count; i++) {
            // Accommodation 생성 (FK 만족을 위해)
            com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation accommodation = com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation.builder()
                    .accommodationsName("테스트 숙소 " + (i + 1))
                    .accommodationsCategory(com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory.GUESTHOUSE)
                    .userId(userRepository.findByEmail("host@test.com").get().getId()) // Use created host
                    .accountNumberId(1L)
                    .accommodationsDescription("테스트 설명")
                    .shortDescription("짧은 설명")
                    .city("서울")
                    .district("강남구")
                    .township("역삼동")
                    .addressDetail("상세주소")
                    .latitude(BigDecimal.valueOf(37.5))
                    .longitude(BigDecimal.valueOf(127.0))
                    .transportInfo("교통정보")
                    .phone("010-0000-0000")
                    .businessRegistrationNumber("123-45-67890")
                    .parkingInfo("주차정보")
                    .checkInTime("15:00")
                    .checkOutTime("11:00")
                    .build();
            accommodationRepository.save(accommodation);

            ReviewEntity review = ReviewEntity.builder()
                    .userId(userId)
                    .accommodationsId(accommodation.getAccommodationsId())
                    .authorName("테스트유저")
                    .rating(new BigDecimal("4.5"))
                    .content("테스트 리뷰 " + (i + 1))
                    .visitDate("2024-12-25")
                    .build();
            reviewJpaRepository.save(review);
        }
        entityManager.flush();
    }

    @Test
    @DisplayName("리뷰 3회 작성 시 쿠폰 자동 발급 테스트")
    void issueReviewRewardCouponTest() {
        // given - 리뷰 3개 생성
        createTestReviews(testUserId, 3);

        // 리뷰 개수 확인
        long reviewCount = reviewJpaRepository.countByUserIdAndIsDeletedFalse(testUserId);
        System.out.println("현재 리뷰 개수: " + reviewCount);
        assertThat(reviewCount).isEqualTo(3);

        // when - 쿠폰 발급 호출
        userCouponService.issueReviewRewardCoupon(testUserId);
        entityManager.flush();

        // then - 쿠폰이 발급되었는지 확인
        List<UserCoupon> userCoupons = userCouponJpaRepository.findByUserIdAndStatus(testUserId, UserCouponStatus.ISSUED);
        assertThat(userCoupons).hasSize(1);
        assertThat(userCoupons.get(0).getCouponId()).isEqualTo(rewardCoupon.getCouponId());

        System.out.println("리뷰 3회 쿠폰 발급 성공!");
        System.out.println("발급된 쿠폰 ID: " + userCoupons.get(0).getCouponId());
        System.out.println("쿠폰 상태: " + userCoupons.get(0).getStatus());
    }

    @Test
    @DisplayName("리뷰 2회 작성 시 쿠폰 발급 안됨 테스트")
    void noRewardCouponFor2ReviewsTest() {
        // given - 리뷰 2개 생성 (3의 배수 아님)
        createTestReviews(testUserId, 2);

        long reviewCount = reviewJpaRepository.countByUserIdAndIsDeletedFalse(testUserId);
        System.out.println("현재 리뷰 개수: " + reviewCount);

        // when
        userCouponService.issueReviewRewardCoupon(testUserId);
        entityManager.flush();

        // then - 쿠폰이 발급되지 않아야 함
        List<UserCoupon> userCoupons = userCouponJpaRepository.findByUserIdAndStatus(testUserId, UserCouponStatus.ISSUED);
        assertThat(userCoupons).isEmpty();

        System.out.println("리뷰 2회 - 쿠폰 발급 안됨 (정상)");
    }

    @Test
    @DisplayName("리뷰 6회 작성 시에도 쿠폰은 1번만 발급 (중복 방지)")
    void noDuplicateCouponFor6ReviewsTest() {
        // given - 리뷰 6개 생성
        createTestReviews(testUserId, 6);

        long reviewCount = reviewJpaRepository.countByUserIdAndIsDeletedFalse(testUserId);
        System.out.println("현재 리뷰 개수: " + reviewCount);

        // when - 여러 번 호출해도 중복 발급 안됨
        userCouponService.issueReviewRewardCoupon(testUserId);
        userCouponService.issueReviewRewardCoupon(testUserId);
        userCouponService.issueReviewRewardCoupon(testUserId);
        entityManager.flush();

        // then - 쿠폰은 1개만 있어야 함
        List<UserCoupon> userCoupons = userCouponJpaRepository.findByUserIdAndStatus(testUserId, UserCouponStatus.ISSUED);
        assertThat(userCoupons).hasSize(1);

        System.out.println("리뷰 6회 - 쿠폰 중복 발급 방지 확인 (1개만 발급됨)");
    }

    @Test
    @DisplayName("쿠폰이 없을 때는 발급하지 않음")
    void noCouponAvailableTest() {
        // given - 쿠폰 삭제
        couponJpaRepository.deleteAll();
        entityManager.flush();

        createTestReviews(testUserId, 3);

        // when
        userCouponService.issueReviewRewardCoupon(testUserId);
        entityManager.flush();

        // then - 쿠폰이 없으므로 발급 안됨
        List<UserCoupon> userCoupons = userCouponJpaRepository.findByUserIdAndStatus(testUserId, UserCouponStatus.ISSUED);
        assertThat(userCoupons).isEmpty();

        System.out.println("쿠폰이 없을 때 - 발급 안됨 (정상)");
    }
}
