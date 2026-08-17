package com.webox.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.api.ApiException;
import com.webox.config.AppProperties;
import com.webox.menu.DishView;
import com.webox.model.DailyMenuItem;
import com.webox.model.MealPeriod;
import com.webox.model.OrderEntity;
import com.webox.model.OrderItem;
import com.webox.model.OrderStatus;
import com.webox.model.User;
import com.webox.repository.DailyMenuItemRepository;
import com.webox.repository.OrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final DailyMenuItemRepository menus;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final Clock clock;

    public record LineRequest(@NotNull Long dishId, @Min(1) @Max(5) int quantity, List<String> selectedOptionIds) {}
    public record PlaceOrderRequest(
            @NotNull LocalDate deliveryDate,
            @NotNull MealPeriod mealPeriod,
            @NotBlank @Size(max = 200) String deliveryAddress,
            @NotEmpty @Size(max = 5) List<@Valid LineRequest> items) {}
    public record OrderLineResponse(Long dishId, String dishName, String unitPrice, int quantity,
                                    List<String> selectedOptions, String lineTotal) {}
    public record OrderResponse(Long id, String orderNumber, LocalDate deliveryDate, MealPeriod mealPeriod,
                                OrderStatus status, String deliveryAddress, String totalAmount,
                                java.time.Instant createdAt, List<OrderLineResponse> items) {}
    record EffectiveSlot(LocalDate date, MealPeriod mealPeriod) {}
    private record PricedLine(DailyMenuItem menuItem, int quantity, BigDecimal unitPrice, List<String> optionLabels) {}

    public OrderService(OrderRepository orders, DailyMenuItemRepository menus, ObjectMapper objectMapper,
                        AppProperties properties, Clock clock) {
        this.orders = orders;
        this.menus = menus;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse place(User user, String idempotencyKey, PlaceOrderRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "A valid Idempotency-Key header is required.");
        }
        var prior = orders.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);
        if (prior.isPresent()) return toResponse(prior.get());

        int totalQuantity = request.items().stream().mapToInt(LineRequest::quantity).sum();
        if (totalQuantity > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_ITEM_LIMIT", "An order can contain at most 5 items.");
        }
        var slot = effectiveSlot(request.deliveryDate(), request.mealPeriod());
        var activeSlotKey = user.getId() + "|" + slot.date() + "|" + slot.mealPeriod();
        var existingSlot = orders.findByActiveSlotKey(activeSlotKey);
        if (existingSlot.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_SLOT_ALREADY_EXISTS",
                    "You already have an active order for this meal. Existing order: " + existingSlot.get().getId());
        }

        var requestedByDish = new HashMap<Long, List<LineRequest>>();
        for (var item : request.items()) {
            requestedByDish.computeIfAbsent(item.dishId(), ignored -> new ArrayList<>()).add(item);
        }
        var locked = menus.lockForOrder(slot.date(), requestedByDish.keySet());
        if (locked.size() != requestedByDish.size()) {
            throw new ApiException(HttpStatus.CONFLICT, "DISH_NOT_ON_MENU", "One or more dishes are not available for this meal date.");
        }

        var pricedLines = new ArrayList<PricedLine>();
        for (var menuItem : locked) {
            var requestLines = requestedByDish.get(menuItem.getDish().getId());
            if (!menuItem.getDish().isPublished()) {
                throw new ApiException(HttpStatus.CONFLICT, "DISH_UNAVAILABLE", menuItem.getDish().getName() + " is no longer available.");
            }
            int requestedQuantity = requestLines.stream().mapToInt(LineRequest::quantity).sum();
            if (menuItem.getRemainingStock() < requestedQuantity) {
                throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                        menuItem.getDish().getName() + " has only " + menuItem.getRemainingStock() + " remaining.");
            }
            for (var requestLine : requestLines) {
                pricedLines.add(price(menuItem, requestLine));
            }
        }

        var order = new OrderEntity("WBX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                user, slot.date(), slot.mealPeriod(), request.deliveryAddress().trim(), idempotencyKey);
        for (var line : pricedLines) {
            try {
                var item = new OrderItem(order, line.menuItem().getDish(), line.unitPrice(), line.quantity(),
                        objectMapper.writeValueAsString(line.optionLabels()));
                order.addItem(item);
                line.menuItem().deduct(line.quantity());
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not store selected options", exception);
            }
        }
        return toResponse(orders.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(User user) {
        return orders.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> active(User user, LocalDate deliveryDate, MealPeriod mealPeriod) {
        var slot = effectiveSlot(deliveryDate, mealPeriod);
        var activeSlotKey = user.getId() + "|" + slot.date() + "|" + slot.mealPeriod();
        return orders.findByActiveSlotKey(activeSlotKey).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(User user, Long id) {
        return orders.findWithItemsByIdAndUserId(id, user.getId()).map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));
    }

    @Transactional
    public OrderResponse cancel(User user, Long id) {
        var order = orders.lockByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));
        if (order.getStatus() != OrderStatus.Pending) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_CANCELLABLE", "Only pending orders can be cancelled.");
        }
        var dishIds = order.getItems().stream().map(item -> item.getDish().getId()).sorted().toList();
        var menuByDish = new HashMap<Long, DailyMenuItem>();
        for (var item : menus.lockForRestore(order.getDeliveryDate(), dishIds)) {
            menuByDish.put(item.getDish().getId(), item);
        }
        for (var line : order.getItems()) {
            var menu = menuByDish.get(line.getDish().getId());
            if (menu != null) menu.restore(line.getQuantity());
        }
        order.cancel();
        return toResponse(order);
    }

    private PricedLine price(DailyMenuItem menuItem, LineRequest request) {
        try {
            var groups = objectMapper.readValue(menuItem.getDish().getOptionsJson(),
                    new TypeReference<List<DishView.OptionGroup>>() {});
            var selected = request.selectedOptionIds() == null ? List.<String>of() : request.selectedOptionIds();
            if (new HashSet<>(selected).size() != selected.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_OPTION", "A customization option cannot be selected twice.");
            }
            var selectedSet = new HashSet<>(selected);
            var knownIds = groups.stream()
                    .flatMap(group -> group.options().stream())
                    .map(DishView.Option::id)
                    .collect(java.util.stream.Collectors.toSet());
            if (!knownIds.containsAll(selectedSet)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_OPTION", "One or more customization options are invalid.");
            }
            var labels = new ArrayList<String>();
            var unitPrice = menuItem.getDish().getPrice();
            for (var group : groups) {
                var chosenInGroup = group.options().stream().filter(option -> selectedSet.contains(option.id())).toList();
                if (group.required() && chosenInGroup.isEmpty()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "REQUIRED_OPTION_MISSING", "Choose an option for " + group.label() + ".");
                }
                if (!group.multiple() && chosenInGroup.size() > 1) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_OPTIONS", "Choose only one option for " + group.label() + ".");
                }
                for (var option : chosenInGroup) {
                    unitPrice = unitPrice.add(option.extraPrice());
                    labels.add(option.label());
                }
            }
            return new PricedLine(menuItem, request.quantity(), unitPrice, labels);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid dish options", exception);
        }
    }

    EffectiveSlot effectiveSlot(LocalDate requestedDate, MealPeriod requestedMeal) {
        var now = ZonedDateTime.now(clock).withZoneSameInstant(properties.zoneId());
        var today = now.toLocalDate();
        if (requestedDate.isBefore(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_DATE_IN_PAST", "Choose today or a future date.");
        }
        if (requestedDate.isAfter(today)) return new EffectiveSlot(requestedDate, requestedMeal);
        var time = now.toLocalTime();
        if (requestedMeal == MealPeriod.Lunch && !time.isBefore(LocalTime.of(10, 0))) {
            if (time.isBefore(LocalTime.of(15, 0))) return new EffectiveSlot(today, MealPeriod.Dinner);
            return new EffectiveSlot(today.plusDays(1), MealPeriod.Lunch);
        }
        if (requestedMeal == MealPeriod.Dinner && !time.isBefore(LocalTime.of(15, 0))) {
            return new EffectiveSlot(today.plusDays(1), MealPeriod.Lunch);
        }
        return new EffectiveSlot(today, requestedMeal);
    }

    private OrderResponse toResponse(OrderEntity order) {
        var lines = order.getItems().stream().map(item -> {
            try {
                return new OrderLineResponse(item.getDish().getId(), item.getDishNameSnapshot(), item.getUnitPrice().toPlainString(),
                        item.getQuantity(), objectMapper.readValue(item.getSelectedOptionsJson(), new TypeReference<List<String>>() {}),
                        item.getLineTotal().toPlainString());
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Invalid order snapshot", exception);
            }
        }).toList();
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getDeliveryDate(), order.getMealPeriod(),
                order.getStatus(), order.getDeliveryAddress(), order.getTotalAmount().toPlainString(), order.getCreatedAt(), lines);
    }
}
