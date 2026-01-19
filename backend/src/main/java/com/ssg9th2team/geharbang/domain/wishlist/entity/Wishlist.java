package com.ssg9th2team.geharbang.domain.wishlist.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wish_id")
    private Long wishId;

    @Column(name = "accommodations_id")
    private Long accommodationsId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


    // 위시리스트 클릭시 자동으로 생성시간 생성
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
