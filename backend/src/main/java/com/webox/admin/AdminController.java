package com.webox.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.api.ApiException;
import com.webox.menu.DishView;
import com.webox.model.DailyMenuItem;
import com.webox.model.Dish;
import com.webox.repository.DailyMenuItemRepository;
import com.webox.repository.DishRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console")
public class AdminController {
    private final DishRepository dishes;
    private final DailyMenuItemRepository menus;
    private final ObjectMapper objectMapper;

    public record DishRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank @Size(max = 40) String category,
            @NotBlank @Size(max = 100) String protein,
            List<String> allergens,
            @NotBlank String spiceLevel,
            List<DishView.OptionGroup> optionGroups,
            @NotBlank @Size(max = 300) String imageUrl) {}
    public record StatusRequest(boolean published) {}
    public record MenuItemRequest(@NotNull Long dishId, @Min(0) int stock) {}
    public record MenuItemResponse(Long dishId, String dishName, int initialStock, int remainingStock) {}

    public AdminController(DishRepository dishes, DailyMenuItemRepository menus, ObjectMapper objectMapper) {
        this.dishes = dishes;
        this.menus = menus;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dishes")
    List<DishView> dishes(@RequestParam(defaultValue = "") String search) {
        var query = search.trim().toLowerCase();
        return dishes.findAllByOrderByNameAsc().stream()
                .filter(dish -> query.isBlank() || dish.getName().toLowerCase().contains(query))
                .map(dish -> DishView.from(dish, null, objectMapper)).toList();
    }

    @PostMapping("/dishes")
    @Transactional
    DishView create(@Valid @RequestBody DishRequest request) {
        return DishView.from(dishes.save(toDish(request)), null, objectMapper);
    }

    @PutMapping("/dishes/{id}")
    @Transactional
    DishView update(@PathVariable Long id, @Valid @RequestBody DishRequest request) {
        var dish = requireDish(id);
        try {
            dish.update(request.name().trim(), request.description().trim(), request.price(), request.category(), request.protein(),
                    objectMapper.writeValueAsString(nullToEmpty(request.allergens())), request.spiceLevel(),
                    objectMapper.writeValueAsString(nullToEmpty(request.optionGroups())), request.imageUrl());
            return DishView.from(dish, null, objectMapper);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DISH_DATA", "Dish configuration is invalid.");
        }
    }

    @PatchMapping("/dishes/{id}/status")
    @Transactional
    DishView status(@PathVariable Long id, @RequestBody StatusRequest request) {
        var dish = requireDish(id);
        dish.setPublished(request.published());
        return DishView.from(dish, null, objectMapper);
    }

    @GetMapping("/menus/{date}")
    List<MenuItemResponse> menu(@PathVariable LocalDate date) {
        return menus.findByMenuDateOrderById(date).stream().map(this::toMenuResponse).toList();
    }

    @PutMapping("/menus/{date}")
    @Transactional
    List<MenuItemResponse> configureMenu(@PathVariable LocalDate date,
                                         @Valid @RequestBody List<@Valid MenuItemRequest> requests) {
        if (requests == null) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MENU", "Menu items are required.");
        var requestedDishIds = requests.stream().map(MenuItemRequest::dishId).toList();
        if (new HashSet<>(requestedDishIds).size() != requestedDishIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_MENU_DISH", "A dish can appear only once in a daily menu.");
        }
        for (var existing : menus.findByMenuDateOrderById(date)) {
            if (!requestedDishIds.contains(existing.getDish().getId())) menus.delete(existing);
        }
        for (var request : requests) {
            var dish = requireDish(request.dishId());
            var item = menus.findByMenuDateAndDishId(date, dish.getId())
                    .orElseGet(() -> new DailyMenuItem(date, dish, request.stock()));
            if (item.getId() != null) {
                if (request.stock() < item.getAllocatedStock()) {
                    throw new ApiException(HttpStatus.CONFLICT, "STOCK_BELOW_ALLOCATED",
                            dish.getName() + " already has " + item.getAllocatedStock() + " items allocated.");
                }
                item.configureStock(request.stock());
            }
            menus.save(item);
        }
        return menus.findByMenuDateOrderById(date).stream().map(this::toMenuResponse).toList();
    }

    private Dish toDish(DishRequest request) {
        try {
            return new Dish(request.name().trim(), request.description().trim(), request.price(), request.category(), request.protein(),
                    objectMapper.writeValueAsString(nullToEmpty(request.allergens())), request.spiceLevel(),
                    objectMapper.writeValueAsString(nullToEmpty(request.optionGroups())), request.imageUrl(), true);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DISH_DATA", "Dish configuration is invalid.");
        }
    }

    private Dish requireDish(Long id) {
        return dishes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DISH_NOT_FOUND", "Dish not found."));
    }

    private MenuItemResponse toMenuResponse(DailyMenuItem item) {
        return new MenuItemResponse(item.getDish().getId(), item.getDish().getName(), item.getInitialStock(), item.getRemainingStock());
    }

    private <T> List<T> nullToEmpty(List<T> value) { return value == null ? List.of() : value; }
}
