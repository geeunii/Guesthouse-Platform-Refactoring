package com.ssg9th2team.geharbang.domain.accommodation_theme.entity; // New package

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accommodation_theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccommodationTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_mapping_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodations_id", nullable = false)
    private Accommodation accommodation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    public AccommodationTheme(Accommodation accommodation, Theme theme) {
        this.accommodation = accommodation;
        this.theme = theme;
    }
}
