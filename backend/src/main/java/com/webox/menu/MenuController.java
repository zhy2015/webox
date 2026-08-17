package com.webox.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.api.ApiException;
import com.webox.repository.DailyMenuItemRepository;
import com.webox.repository.DishRepository;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class MenuController {
    private final DailyMenuItemRepository menus;
    private final DishRepository dishes;
    private final ObjectMapper objectMapper;

    public MenuController(DailyMenuItemRepository menus, DishRepository dishes, ObjectMapper objectMapper) {
        this.menus = menus;
        this.dishes = dishes;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/menus/{date}")
    List<DishView> menu(@PathVariable LocalDate date,
                        @RequestParam(defaultValue = "") @Size(max = 50) String search,
                        @RequestParam(defaultValue = "") String categories) {
        var query = search.trim().toLowerCase(Locale.ROOT);
        var allowedCategories = categories.isBlank() ? List.<String>of() : Arrays.stream(categories.split(",")).toList();
        return menus.findPublishedMenu(date).stream()
                .filter(item -> query.isBlank()
                        || item.getDish().getName().toLowerCase(Locale.ROOT).contains(query)
                        || item.getDish().getDescription().toLowerCase(Locale.ROOT).contains(query))
                .filter(item -> allowedCategories.isEmpty() || allowedCategories.contains(item.getDish().getCategory()))
                .map(item -> DishView.from(item, objectMapper))
                .toList();
    }

    @GetMapping("/dishes/{id}")
    DishView dish(@PathVariable Long id) {
        var dish = dishes.findById(id)
                .filter(com.webox.model.Dish::isPublished)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DISH_NOT_FOUND", "This dish is not available."));
        return DishView.from(dish, null, objectMapper);
    }
}
