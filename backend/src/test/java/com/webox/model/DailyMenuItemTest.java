package com.webox.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyMenuItemTest {
    private final Dish dish = new Dish("Test Dish", "For inventory tests", new BigDecimal("20.00"),
            "Chinese", "Tofu", "[]", "None", "[]", "/images/test.jpg", true);

    @Test
    void preservesAllocatedQuantityWhenSupplyChanges() {
        var item = new DailyMenuItem(LocalDate.of(2026, 8, 18), dish, 10);
        item.deduct(4);

        item.configureStock(8);

        assertThat(item.getAllocatedStock()).isEqualTo(4);
        assertThat(item.getRemainingStock()).isEqualTo(4);
        assertThat(item.getInitialStock()).isEqualTo(8);
    }

    @Test
    void rejectsSupplyBelowTheAlreadyAllocatedQuantity() {
        var item = new DailyMenuItem(LocalDate.of(2026, 8, 18), dish, 10);
        item.deduct(4);

        assertThatThrownBy(() -> item.configureStock(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already allocated");
        assertThat(item.getInitialStock()).isEqualTo(10);
        assertThat(item.getRemainingStock()).isEqualTo(6);
    }
}
