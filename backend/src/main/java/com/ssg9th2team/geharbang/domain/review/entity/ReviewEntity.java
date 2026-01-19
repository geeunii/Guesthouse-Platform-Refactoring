package com.ssg9th2team.geharbang.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "accommodations_id")
    private Long accommodationsId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "content")
    private String content;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "visit_date", length = 50)
    private String visitDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "is_crawled")
    @Builder.Default
    private Boolean isCrawled = false;


    // @OneToMany = 1개의 리뷰 : 여러 개의 리뷰 이미지, ReviewEntity가 부모 엔티티
    // mappedBy = "review" =  연관관계의 주인은 ReviewImageEntity.review
    // cascade = CascadeType.ALL = 부모(Review) 기준으로 자식(Image)에게 모든 작업 전파
    // orphanRemoval = true = 컬렉션에서 제거된 자식 엔티티는 DB에서도 삭제, 이미지 수정 시 "전체 삭제 후 재등록" 패턴에 필수
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImageEntity> images = new ArrayList<>();



    // 수정 메서드 (텍스트 + 별점 + 이미지) -> 이미지 전체 삭제 후 재등록
    public void updateReview(String content, BigDecimal rating, List<ReviewImageEntity> newImages) {
        if (content != null)
            this.content = content;
        if (rating != null)
            this.rating = rating;
        
        if (newImages != null) {
            this.images.clear(); // orphanRemoval → 기존 이미지 전부 삭제
            newImages.forEach(img -> img.setReview(this));  // 연관관계 주인 설정
            this.images.addAll(newImages);
        }
    }



    // 소프트 삭제
    public void softDelete() {
        this.isDeleted = true;
    }

    // 이미지 추가 헬퍼
    public void addImage(ReviewImageEntity image) {
        images.add(image);
        image.setReview(this);
    }


    // 리뷰 생성시간 자동 생성
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
