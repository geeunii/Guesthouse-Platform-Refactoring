package com.ssg9th2team.geharbang.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccommodationPriceInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initMinPrices() {
        log.info("Starting accommodation min_price initialization...");

        String sql = """
                    UPDATE accommodation a
                    SET min_price = (
                        SELECT MIN(price)
                        FROM room r
                        WHERE r.accommodations_id = a.accommodations_id
                        AND r.price > 0
                    )
                    WHERE min_price IS NULL OR min_price = 0
                """;

        int updatedRows = jdbcTemplate.update(sql);

        if (updatedRows == 0) {
            log.warn("No min_price updated from rooms. Possible missing room data. Applying fallback random prices...");
        } else {
            log.info("Finished accommodation min_price update from rooms. Updated {} rows.", updatedRows);
        }

        // Fallback: Force set random price for ALL accommodations that still have no
        // valid price
        String fallbackSql = """
                    UPDATE accommodation
                    SET min_price = FLOOR(30000 + (RAND() * 70000))
                    WHERE min_price IS NULL OR min_price <= 0
                """;
        int fallbackRows = jdbcTemplate.update(fallbackSql);
        log.info("Applied fallback random prices to {} accommodations.", fallbackRows);
    }
}
