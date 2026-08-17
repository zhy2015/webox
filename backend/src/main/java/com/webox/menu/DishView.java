package com.webox.menu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.model.DailyMenuItem;
import com.webox.model.Dish;
import java.math.BigDecimal;
import java.util.List;

public record DishView(
        Long id,
        String name,
        String description,
        String price,
        String category,
        String protein,
        List<String> allergens,
        String spiceLevel,
        List<OptionGroup> optionGroups,
        String imageUrl,
        boolean published,
        Integer remainingStock) {

    public record OptionGroup(String id, String label, boolean required, boolean multiple, List<Option> options) {}
    public record Option(String id, String label, BigDecimal extraPrice) {}

    public static DishView from(Dish dish, Integer remainingStock, ObjectMapper objectMapper) {
        try {
            var allergens = objectMapper.readValue(dish.getAllergensJson(), new TypeReference<List<String>>() {});
            var groups = objectMapper.readValue(dish.getOptionsJson(), new TypeReference<List<OptionGroup>>() {});
            return new DishView(dish.getId(), dish.getName(), dish.getDescription(), dish.getPrice().toPlainString(),
                    dish.getCategory(), dish.getProtein(), allergens, dish.getSpiceLevel(), groups,
                    dish.getImageUrl(), dish.isPublished(), remainingStock);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid dish configuration", exception);
        }
    }

    public static DishView from(DailyMenuItem item, ObjectMapper objectMapper) {
        return from(item.getDish(), item.getRemainingStock(), objectMapper);
    }
}
