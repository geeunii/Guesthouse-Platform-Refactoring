package com.ssg9th2team.geharbang.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_image_id")
    private Long reviewImageId;

    // 여러 이미지가 하나의 리뷰에 속함
    //연관관계의 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    @Setter
    private ReviewEntity review;

    @Column(name = "review_image_url")
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;


    // 정적 팩토리 메서드
    public static ReviewImageEntity of(String imageUrl, Integer sortOrder) {
        return ReviewImageEntity.builder()
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }
}
