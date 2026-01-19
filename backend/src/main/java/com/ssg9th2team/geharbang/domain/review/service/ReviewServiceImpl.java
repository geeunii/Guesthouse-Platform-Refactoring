package com.ssg9th2team.geharbang.domain.review.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponService;
import com.ssg9th2team.geharbang.domain.coupon.service.UserCouponServiceImpl;
import com.ssg9th2team.geharbang.domain.profanity.service.ProfanityFilterService;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.reservation.repository.jpa.ReservationJpaRepository;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewCreateDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewResponseDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewTagDto;
import com.ssg9th2team.geharbang.domain.review.dto.ReviewUpdateDto;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewEntity;
import com.ssg9th2team.geharbang.domain.review.entity.ReviewImageEntity;
import com.ssg9th2team.geharbang.domain.review.repository.jpa.ReviewJpaRepository;
import com.ssg9th2team.geharbang.domain.review.repository.mybatis.ReviewMapper;
import com.ssg9th2team.geharbang.global.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewJpaRepository reviewJpaRepository;
    private final ObjectStorageService objectStorageService;
    private final ReservationJpaRepository reservationJpaRepository;
    private final UserRepository userRepository;
    private final UserCouponService userCouponService;
    private final ProfanityFilterService profanityFilterService;


    // 리뷰 등록 (쿠폰 발급 여부 반환)
    @Override
    @Transactional
    public boolean createReview(Long userId, ReviewCreateDto reviewCreateDto) {
       // 해당 숙소 예약 내역 조회 (확정된 예약 + 체크아웃 완료된 것 중 가장 최근 1개)
        Reservation reservation = reservationJpaRepository.findFirstByUserIdAndAccommodationsIdAndReservationStatusAndCheckoutBeforeOrderByCheckoutDesc(
                userId, reviewCreateDto.getAccommodationsId(), 2, LocalDateTime.now())
                .orElseThrow(()-> new IllegalArgumentException("체크아웃이 완료된 예약 내역이 없습니다"));

        // 기한 체크 (체크아웃 후 7일 이내)
        if (reservation.getCheckout().isBefore(LocalDateTime.now().minusDays(7))) {
            throw new IllegalArgumentException("리뷰 작성 기한(체크아웃 후 7일)이 지났습니다.");
        }

        // 이미 리뷰 작성했는지 확인
        if(reviewJpaRepository.existsByUserIdAndAccommodationsIdAndIsDeletedFalse(userId, reviewCreateDto.getAccommodationsId())) {
            throw new IllegalArgumentException("이미 리뷰를 작성했습니다");
        }

        // 금칙어 검사
        profanityFilterService.validateNoProfanity(reviewCreateDto.getContent(), "리뷰 내용");

        // 사용자 닉네임 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 리뷰 엔티티 생성
        ReviewEntity reviewEntity = ReviewEntity.builder()
                .accommodationsId(reviewCreateDto.getAccommodationsId())
                .userId(userId)
                .authorName(user.getNickname())  // 작성 시점 닉네임 저장
                .rating(reviewCreateDto.getRating())
                .content(reviewCreateDto.getContent())
                .visitDate(reviewCreateDto.getVisitDate())
                .build();

        // 이미지 업로드 → ReviewImageEntity 추가
        if (reviewCreateDto.getImageUrls() != null && !reviewCreateDto.getImageUrls().isEmpty()) {
            for (int i = 0; i < reviewCreateDto.getImageUrls().size(); i++) {
                // Base64 이미지면 업로드, URL이면 그대로 사용
                String imageUrl = objectStorageService.uploadBase64Image(
                        reviewCreateDto.getImageUrls().get(i), "reviews");
                reviewEntity.addImage(ReviewImageEntity.of(imageUrl, i));
            }
        }

        // 리뷰 저장 (cascade로 이미지도 같이 저장됨)
        ReviewEntity savedReview = reviewJpaRepository.save(reviewEntity);

        // 태그 저장 (review_tag_map 테이블)
        if (reviewCreateDto.getTagIds() != null && !reviewCreateDto.getTagIds().isEmpty()) {
            reviewMapper.insertReviewTags(savedReview.getReviewId(), reviewCreateDto.getTagIds());
        }

        // 리뷰 등록시 쿠폰 서비스 호출 (쿠폰 발급 여부 반환)
        return userCouponService.issueReviewRewardCoupon(userId);
    }



    // 리뷰 수정
    @Override
    @Transactional
    public void updateReview(Long userId, Long reviewId, ReviewUpdateDto reviewUpdateDto) {
        // 내가 쓴 리뷰 내용 조회
        ReviewEntity reviewEntity = reviewJpaRepository.findByReviewIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 권환 확인 ( 본인인지 )
        // ReviewEntity안에 있는(DB에 리뷰번호, 유저아이디 저장되어있으니까) 유저 아이디와 파라미터로 받은 유저 아이디가 같은지 검사해서 권한 확인
        if(!reviewEntity.getUserId().equals(userId)) {
            throw new IllegalArgumentException("리뷰 수정 권한이 없습니다");
        }

        // 금칙어 검사
        if (reviewUpdateDto.getContent() != null) {
            profanityFilterService.validateNoProfanity(reviewUpdateDto.getContent(), "리뷰 내용");
        }

        // 이미지 리스트 구성
        List<ReviewImageEntity> newImages = new ArrayList<>();
        if(reviewUpdateDto.getImageUrls() != null && !reviewUpdateDto.getImageUrls().isEmpty()) {
            for(int i = 0; i < reviewUpdateDto.getImageUrls().size(); i++) {
                String url = reviewUpdateDto.getImageUrls().get(i);

                String savedUrl = url;
                if (url != null && !url.startsWith("http")) {
                    savedUrl = objectStorageService.uploadBase64Image(url, "reviews");
                }
                if (savedUrl != null) {
                    newImages.add(ReviewImageEntity.of(savedUrl, i));
                }
            }
        }
        // 4. 엔티티 수정
        // ReviewUpdateDto.rating -> BigDecimal 변환
        BigDecimal rating = reviewUpdateDto.getRating() != null ?
                BigDecimal.valueOf(reviewUpdateDto.getRating()) : null;

        reviewEntity.updateReview(reviewUpdateDto.getContent(), rating, newImages);

        // 5. 태그 업데이트 (전체 삭제 후 재등록)
        // dto는 사용자가 입력한 값을 받는 거 -> getTagIds() != null : 태그를 변경 했다면 전체 삭제 후 재등록
        if (reviewUpdateDto.getTagIds() != null) {
            reviewMapper.deleteReviewTags(reviewId);
            if (!reviewUpdateDto.getTagIds().isEmpty()) {
                reviewMapper.insertReviewTags(reviewId, reviewUpdateDto.getTagIds());
            }
        }
    }


    // 리뷰 삭제
    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        ReviewEntity reviewEntity = reviewJpaRepository.findByReviewIdAndIsDeletedFalse(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if(!reviewEntity.getUserId().equals(userId)) {
            throw new IllegalArgumentException("리뷰 삭제 권한이 없습니다");
        }
        reviewEntity.softDelete();
    }



    // 특정 숙소에 달린 '모든 리뷰 목록'을 조회
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByAccommodation(Long userId, Long accommodationsId) {
        return reviewMapper.selectReviewsByAccommodationId(userId, accommodationsId);
    }



    // [User] 내가 작성한 리뷰 조회
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByUserId(Long userId) {
        return reviewMapper.selectReviewsByUserId(userId);
    }





    // 전체 태그 목록 조회
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviewTags")
    public List<ReviewTagDto> getAllReviewTags() {
        return reviewMapper.selectAllReviewTags();
    }
}
