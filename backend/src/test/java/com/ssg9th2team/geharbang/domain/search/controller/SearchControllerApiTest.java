package com.ssg9th2team.geharbang.domain.search.controller;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.AccommodationsCategory;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.ocr.service.OcrService;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private OcrService ocrService;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                    "CREATE TABLE IF NOT EXISTS accommodation_image (" +
                            "image_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                            "accommodations_id BIGINT NOT NULL, " +
                            "image_url VARCHAR(255), " +
                            "image_type VARCHAR(20), " +
                            "sort_order INT" +
                            ")"
            ).executeUpdate();
            return null;
        });
    }

    @Test
    @DisplayName("Uses accommodation min price when no date and single guest")
    void searchUsesAccommodationMinPriceWhenNoDateAndSingleGuest() throws Exception {
        Long accommodationId = persistAccommodation("API-BASE-001", 20000);
        persistRoom(accommodationId, 5000, 4);
        persistRoom(accommodationId, 15000, 2);

        mockMvc.perform(get("/api/public/search")
                        .param("keyword", "API-BASE-001")
                        .param("guestCount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].minPrice").value(20000));
    }

    @Test
    @DisplayName("Uses available min price when date is provided")
    void searchUsesAvailableMinPriceWhenDateProvided() throws Exception {
        Long accommodationId = persistAccommodation("API-DATE-001", 20000);
        persistRoom(accommodationId, 5000, 4);
        persistRoom(accommodationId, 15000, 2);

        mockMvc.perform(get("/api/public/search")
                        .param("keyword", "API-DATE-001")
                        .param("checkin", LocalDate.of(2026, 3, 1).toString())
                        .param("checkout", LocalDate.of(2026, 3, 3).toString())
                        .param("guestCount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].minPrice").value(5000));
    }

    @Test
    @DisplayName("Uses available min price when guest count is at least two")
    void searchUsesAvailableMinPriceWhenGuestCountAtLeastTwo() throws Exception {
        Long accommodationId = persistAccommodation("API-GUEST-002", 20000);
        persistRoom(accommodationId, 5000, 4);
        persistRoom(accommodationId, 15000, 2);

        mockMvc.perform(get("/api/public/search")
                        .param("keyword", "API-GUEST-002")
                        .param("guestCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].minPrice").value(5000));
    }

    private Long persistAccommodation(String name, int minPrice) {
        return transactionTemplate.execute(status -> {
            Accommodation accommodation = Accommodation.builder()
                    .accountNumberId(1L)
                    .userId(1L)
                    .accommodationsName(name)
                    .accommodationsCategory(AccommodationsCategory.GUESTHOUSE)
                    .accommodationsDescription("test description")
                    .shortDescription("test short")
                    .city("Jeju")
                    .district("Seogwipo")
                    .township("test-township")
                    .addressDetail("test-address")
                    .latitude(BigDecimal.valueOf(33.25))
                    .longitude(BigDecimal.valueOf(126.55))
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
            return accommodation.getAccommodationsId();
        });
    }

    private Long persistRoom(Long accommodationsId, int price, int maxGuests) {
        return transactionTemplate.execute(status -> {
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
            return room.getRoomId();
        });
    }
}
