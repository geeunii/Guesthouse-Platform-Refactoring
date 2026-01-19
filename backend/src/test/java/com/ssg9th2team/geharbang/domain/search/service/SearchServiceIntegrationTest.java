package com.ssg9th2team.geharbang.domain.search.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation_theme.entity.AccommodationTheme;
import com.ssg9th2team.geharbang.domain.main.dto.PublicListResponse;
import com.ssg9th2team.geharbang.domain.reservation.entity.Reservation;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import com.ssg9th2team.geharbang.domain.theme.entity.Theme;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SearchServiceImpl.class)
@TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:searchtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.properties.hibernate.type.preferred_boolean_jdbc_type=TINYINT",
                "spring.flyway.enabled=false"
})
@Transactional
class SearchServiceIntegrationTest {

        @Autowired
        private SearchService searchService;

        @Autowired
        private EntityManager entityManager;

        @BeforeEach
        void setUpSchema() {
                entityManager.createNativeQuery(
                                "CREATE TABLE IF NOT EXISTS accommodation_image (" +
                                                "image_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                                                "accommodations_id BIGINT NOT NULL, " +
                                                "image_url VARCHAR(255), " +
                                                "image_type VARCHAR(20), " +
                                                "sort_order INT" +
                                                ")")
                                .executeUpdate();
        }

        @Test
        @DisplayName("Search returns accommodations for Seogwipo keyword")
        void searchBySeogwipoKeywordReturnsAccommodation() {
                Accommodation accommodation = persistAccommodation("\uC11C\uADC0\uD3EC \uD14C\uC2A4\uD2B8 \uC219\uC18C",
                                "\uC11C\uADC0\uD3EC\uC2DC");
                persistRoom(accommodation.getAccommodationsId(), 4);
                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                "\uC11C\uADC0\uD3EC",
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null);

                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).getAccommodationsName())
                                .isEqualTo("\uC11C\uADC0\uD3EC \uD14C\uC2A4\uD2B8 \uC219\uC18C");
        }

        @Test
        @DisplayName("Guest count filter excludes accommodations with insufficient capacity")
        void searchFiltersByGuestCount() {
                Accommodation small = persistAccommodation("\uC11C\uADC0\uD3EC \uC18C\uD615 \uC219\uC18C",
                                "\uC11C\uADC0\uD3EC\uC2DC");
                persistRoom(small.getAccommodationsId(), 2);

                Accommodation large = persistAccommodation("\uC11C\uADC0\uD3EC \uB300\uD615 \uC219\uC18C",
                                "\uC11C\uADC0\uD3EC\uC2DC");
                persistRoom(large.getAccommodationsId(), 6);
                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                "\uC11C\uADC0\uD3EC",
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                4,
                                null,
                                null,
                                false,
                                null);

                List<String> names = response.items().stream()
                                .map(item -> item.getAccommodationsName())
                                .toList();
                assertThat(names).containsExactly("\uC11C\uADC0\uD3EC \uB300\uD615 \uC219\uC18C");
        }

        @Test
        @DisplayName("Date range filter excludes accommodations with fully booked rooms")
        void searchFiltersOutUnavailableRoomsForDateRange() {
                LocalDateTime checkin = LocalDateTime.of(2026, 2, 10, 15, 0);
                LocalDateTime checkout = LocalDateTime.of(2026, 2, 12, 11, 0);

                Accommodation blocked = persistAccommodation("\uC11C\uADC0\uD3EC \uC608\uC57D\uBD88\uAC00",
                                "\uC11C\uADC0\uD3EC\uC2DC");
                Room blockedRoom = persistRoom(blocked.getAccommodationsId(), 4);
                persistReservation(blocked.getAccommodationsId(), blockedRoom.getRoomId(), checkin, checkout, 4);

                Accommodation available = persistAccommodation("\uC11C\uADC0\uD3EC \uC608\uC57D\uAC00\uB2A5",
                                "\uC11C\uADC0\uD3EC\uC2DC");
                persistRoom(available.getAccommodationsId(), 4);
                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                "\uC11C\uADC0\uD3EC",
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                checkin,
                                checkout,
                                2,
                                null,
                                null,
                                false,
                                null);

                List<String> names = response.items().stream()
                                .map(item -> item.getAccommodationsName())
                                .toList();
                assertThat(names).containsExactly("\uC11C\uADC0\uD3EC \uC608\uC57D\uAC00\uB2A5");
        }

        @Test
        @DisplayName("Min price uses available rooms when capacity remains")
        void searchMinPriceUsesAvailableRoomsWhenCapacityRemains() {
                LocalDateTime checkin = LocalDateTime.of(2026, 3, 1, 15, 0);
                LocalDateTime checkout = LocalDateTime.of(2026, 3, 3, 11, 0);

                Accommodation accommodation = persistAccommodation(
                                "\uC11C\uADC0\uD3EC \uCD5C\uC800\uAC00 \uD14C\uC2A4\uD2B8", "\uC11C\uADC0\uD3EC\uC2DC");
                Room cheapRoom = persistRoom(accommodation.getAccommodationsId(), 4, 5000);
                persistRoom(accommodation.getAccommodationsId(), 4, 15000);
                persistReservation(accommodation.getAccommodationsId(), cheapRoom.getRoomId(), checkin, checkout, 1);
                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                "\uC11C\uADC0\uD3EC",
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                checkin,
                                checkout,
                                2,
                                null,
                                null,
                                false,
                                null);

                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).getMinPrice()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("Bounds filter excludes accommodations outside of coordinates when no dates")
        void searchByBoundsFiltersWithoutDates() {
                Accommodation inside = persistAccommodation("Bounds-In", "Seogwipo",
                                BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55));
                persistRoom(inside.getAccommodationsId(), 4);

                Accommodation outside = persistAccommodation("Bounds-Out", "Seogwipo",
                                BigDecimal.valueOf(37.55), BigDecimal.valueOf(126.98));
                persistRoom(outside.getAccommodationsId(), 4);
                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                null,
                                0,
                                10,
                                33.0,
                                33.6,
                                126.0,
                                126.8,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null);

                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).getAccommodationsName()).isEqualTo("Bounds-In");
        }

        @Test
        @DisplayName("Theme filter returns only accommodations mapped to theme when no dates")
        void searchByThemeFiltersWithoutDates() {
                Theme themeA = persistTheme("stay", "theme-a");
                Theme themeB = persistTheme("stay", "theme-b");

                Accommodation themed = persistAccommodation("Theme-In", "Seogwipo");
                persistRoom(themed.getAccommodationsId(), 4);
                linkTheme(themed, themeA);

                Accommodation other = persistAccommodation("Theme-Out", "Seogwipo");
                persistRoom(other.getAccommodationsId(), 4);
                linkTheme(other, themeB);
                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                List.of(themeA.getId()),
                                null,
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null);

                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).getAccommodationsName()).isEqualTo("Theme-In");
        }

        @Test
        @DisplayName("Price filter without dates returns correct count and items")
        void searchByPriceFiltersWithoutDates() {
                Accommodation cheap = persistAccommodation("Cheap-Acc", "Seogwipo",
                                BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55), 5000);
                persistRoom(cheap.getAccommodationsId(), 4, 5000);

                Accommodation expensive = persistAccommodation("Expensive-Acc", "Seogwipo",
                                BigDecimal.valueOf(33.26), BigDecimal.valueOf(126.56), 20000);
                persistRoom(expensive.getAccommodationsId(), 4, 20000);

                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                null,
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                15000,
                                25000,
                                false,
                                null);

                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).getAccommodationsName()).isEqualTo("Expensive-Acc");
                assertThat(response.page().totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Search with dates and price filters returns correct count")
        void searchWithDatesAndPriceFiltersReturnsCorrectCount() {
                LocalDateTime checkin = LocalDateTime.of(2026, 4, 1, 15, 0);
                LocalDateTime checkout = LocalDateTime.of(2026, 4, 3, 11, 0);

                Accommodation cheap = persistAccommodation("Cheap-Dates", "Seogwipo",
                                BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55), 5000);
                persistRoom(cheap.getAccommodationsId(), 4, 5000);

                Accommodation expensive = persistAccommodation("Expensive-Dates", "Seogwipo",
                                BigDecimal.valueOf(33.26), BigDecimal.valueOf(126.56), 20000);
                persistRoom(expensive.getAccommodationsId(), 4, 20000);

                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                Collections.emptyList(),
                                null,
                                0,
                                10,
                                null,
                                null,
                                null,
                                null,
                                checkin,
                                checkout,
                                null,
                                15000,
                                25000,
                                false,
                                null);

                assertThat(response.items()).extracting("accommodationsName")
                                .containsExactly("Expensive-Dates");
                assertThat(response.page().totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Search with themes, bounds, and price filters (no dates) returns correct results")
        void searchPublicListByThemeAndBoundsNoDates_returnsCorrectResults() {
                Theme healing = persistTheme("Mood", "Healing");
                Theme party = persistTheme("Concept", "Party");

                // Acc1: Valid (In Bounds, Has Theme, Valid Price)
                Accommodation validAcc = persistAccommodation("Valid-Acc", "Seogwipo",
                                BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55), 10000);
                linkTheme(validAcc, healing);
                persistRoom(validAcc.getAccommodationsId(), 4, 10000);

                // Acc2: Out of Bounds
                Accommodation outOfBoundsAcc = persistAccommodation("OutOfBounds-Acc", "Jeju",
                                BigDecimal.valueOf(33.50), BigDecimal.valueOf(126.55), 10000);
                linkTheme(outOfBoundsAcc, healing);
                persistRoom(outOfBoundsAcc.getAccommodationsId(), 4, 10000);

                // Acc3: Wrong Theme
                Accommodation wrongThemeAcc = persistAccommodation("WrongTheme-Acc", "Seogwipo",
                                BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55), 10000);
                linkTheme(wrongThemeAcc, party);
                persistRoom(wrongThemeAcc.getAccommodationsId(), 4, 10000);

                // Acc4: Price Too High
                Accommodation expensiveAcc = persistAccommodation("Expensive-Acc", "Seogwipo",
                                BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55), 30000);
                linkTheme(expensiveAcc, healing);
                persistRoom(expensiveAcc.getAccommodationsId(), 4, 30000);

                entityManager.clear();

                PublicListResponse response = searchService.searchPublicList(
                                List.of(healing.getId()),
                                null,
                                0, 10,
                                33.20, 33.30, 126.50, 126.60,
                                null, null,
                                null,
                                5000, 20000, false, null);

                assertThat(response.items()).hasSize(1);
                assertThat(response.items().get(0).getAccommodationsName()).isEqualTo("Valid-Acc");
                assertThat(response.page().totalElements()).isEqualTo(1);
        }

        private Accommodation persistAccommodation(String name, String district) {
                return persistAccommodation(name, district, BigDecimal.valueOf(33.25), BigDecimal.valueOf(126.55));
        }

        private Accommodation persistAccommodation(String name, String district, BigDecimal latitude,
                        BigDecimal longitude) {
                return persistAccommodation(name, district, latitude, longitude, 10000);
        }

        private Accommodation persistAccommodation(String name, String district, BigDecimal latitude,
                        BigDecimal longitude, int minPrice) {
                Accommodation accommodation = Accommodation.builder()
                                .accountNumberId(1L)
                                .userId(1L)
                                .accommodationsName(name)
                                .accommodationsCategory(AccommodationsCategory.GUESTHOUSE)
                                .accommodationsDescription("test description")
                                .shortDescription("test short")
                                .city("\uC81C\uC8FC")
                                .district(district)
                                .township("test-township")
                                .addressDetail("test-address")
                                .latitude(latitude)
                                .longitude(longitude)
                                .transportInfo("test transport")
                                .phone("010-0000-0000")
                                .businessRegistrationNumber("1234567890")
                                .parkingInfo("test parking")
                                .checkInTime("15:00")
                                .checkOutTime("11:00")
                                .minPrice(minPrice)
                                .build();
                entityManager.persist(accommodation);
                entityManager.flush();

                accommodation.updateApprovalStatus(ApprovalStatus.APPROVED, null);
                entityManager.flush();
                return accommodation;
        }

        private Theme persistTheme(String category, String name) {
                Theme theme = Theme.builder()
                                .themeCategory(category)
                                .themeName(name)
                                .build();
                entityManager.persist(theme);
                entityManager.flush();
                return theme;
        }

        private void linkTheme(Accommodation accommodation, Theme theme) {
                entityManager.persist(new AccommodationTheme(accommodation, theme));
                entityManager.flush();
        }

        private Room persistRoom(Long accommodationsId, int maxGuests) {
                return persistRoom(accommodationsId, maxGuests, 10000);
        }

        private Room persistRoom(Long accommodationsId, int maxGuests, int price) {
                Room room = Room.builder()
                                .accommodationsId(accommodationsId)
                                .roomName("test room")
                                .price(price)
                                .minGuests(1)
                                .maxGuests(maxGuests)
                                .roomStatus(1)
                                .build();
                entityManager.persist(room);
                entityManager.flush();
                return room;
        }

        private void persistReservation(Long accommodationsId, Long roomId, LocalDateTime checkin,
                        LocalDateTime checkout) {
                persistReservation(accommodationsId, roomId, checkin, checkout, 2);
        }

        private void persistReservation(Long accommodationsId, Long roomId, LocalDateTime checkin,
                        LocalDateTime checkout, int guestCount) {
                int nights = (int) ChronoUnit.DAYS.between(checkin.toLocalDate(), checkout.toLocalDate());
                Reservation reservation = Reservation.builder()
                                .accommodationsId(accommodationsId)
                                .roomId(roomId)
                                .userId(1L)
                                .checkin(checkin)
                                .checkout(checkout)
                                .stayNights(nights)
                                .guestCount(guestCount)
                                .reservationStatus(2)
                                .totalAmountBeforeDc(10000)
                                .couponDiscountAmount(0)
                                .finalPaymentAmount(10000)
                                .paymentStatus(1)
                                .reserverName("\uD14C\uC2A4\uD130")
                                .reserverPhone("010-0000-0000")
                                .build();
                entityManager.persist(reservation);
                entityManager.flush();
        }
}
