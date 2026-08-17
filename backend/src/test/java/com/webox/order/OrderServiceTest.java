package com.webox.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.api.ApiException;
import com.webox.config.AppProperties;
import com.webox.model.DailyMenuItem;
import com.webox.model.Dish;
import com.webox.model.MealPeriod;
import com.webox.model.OrderEntity;
import com.webox.model.OrderItem;
import com.webox.model.OrderStatus;
import com.webox.model.User;
import com.webox.repository.DailyMenuItemRepository;
import com.webox.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
    private static final String OPTIONS = """
            [{"id":"base","label":"Base","required":true,"multiple":false,"options":[
              {"id":"rice","label":"Brown Rice","extraPrice":0.00},
              {"id":"quinoa","label":"Quinoa","extraPrice":3.00}
            ]}]
            """;

    private OrderRepository orders;
    private DailyMenuItemRepository menus;
    private ObjectMapper objectMapper;
    private User user;
    private Dish dish;
    private DailyMenuItem menuItem;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        menus = mock(DailyMenuItemRepository.class);
        objectMapper = new ObjectMapper();
        user = mock(User.class);
        dish = mock(Dish.class);
        menuItem = mock(DailyMenuItem.class);

        when(user.getId()).thenReturn(77L);
        when(dish.getId()).thenReturn(9L);
        when(dish.getName()).thenReturn("Chicken Bowl");
        when(dish.getPrice()).thenReturn(new BigDecimal("32.00"));
        when(dish.getOptionsJson()).thenReturn(OPTIONS);
        when(dish.isPublished()).thenReturn(true);
        when(menuItem.getDish()).thenReturn(dish);
        when(menuItem.getRemainingStock()).thenReturn(10);
        when(orders.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(orders.findByActiveSlotKey(any())).thenReturn(Optional.empty());
        when(orders.saveAndFlush(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void pricesDistinctConfigurationsOfTheSameDishAndAggregatesTheirTotal() {
        var date = TODAY.plusDays(1);
        when(menus.lockForOrder(date, Set.of(9L))).thenReturn(List.of(menuItem));
        var service = serviceAt(8, 0);

        var response = service.place(user, "checkout-1", request(date,
                new OrderService.LineRequest(9L, 1, List.of("rice")),
                new OrderService.LineRequest(9L, 1, List.of("quinoa"))));

        assertThat(response.totalAmount()).isEqualTo("67.00");
        assertThat(response.items()).extracting(OrderService.OrderLineResponse::unitPrice)
                .containsExactly("32.00", "35.00");
        verify(menuItem, times(2)).deduct(1);
    }

    @Test
    void checksStockAcrossAllConfigurationsBeforeDeducting() {
        var date = TODAY.plusDays(1);
        when(menuItem.getRemainingStock()).thenReturn(1);
        when(menus.lockForOrder(date, Set.of(9L))).thenReturn(List.of(menuItem));

        assertApiError("INSUFFICIENT_STOCK", () -> serviceAt(8, 0).place(user, "checkout-2", request(date,
                new OrderService.LineRequest(9L, 1, List.of("rice")),
                new OrderService.LineRequest(9L, 1, List.of("quinoa")))));
        verify(menuItem, never()).deduct(anyInt());
    }

    @Test
    void rejectsMoreThanFiveItemsBeforeLockingInventory() {
        assertApiError("ORDER_ITEM_LIMIT", () -> serviceAt(8, 0).place(user, "checkout-3", request(TODAY.plusDays(1),
                new OrderService.LineRequest(9L, 3, List.of("rice")),
                new OrderService.LineRequest(9L, 3, List.of("quinoa")))));
        verify(menus, never()).lockForOrder(any(), any());
    }

    @Test
    void rejectsMissingUnknownDuplicateAndMultipleSingleChoiceOptions() {
        var date = TODAY.plusDays(1);
        when(menus.lockForOrder(date, Set.of(9L))).thenReturn(List.of(menuItem));
        var service = serviceAt(8, 0);

        assertApiError("REQUIRED_OPTION_MISSING", () -> service.place(user, "missing", request(date,
                new OrderService.LineRequest(9L, 1, List.of()))));
        assertApiError("UNKNOWN_OPTION", () -> service.place(user, "unknown", request(date,
                new OrderService.LineRequest(9L, 1, List.of("unknown")))));
        assertApiError("DUPLICATE_OPTION", () -> service.place(user, "duplicate", request(date,
                new OrderService.LineRequest(9L, 1, List.of("rice", "rice")))));
        assertApiError("TOO_MANY_OPTIONS", () -> service.place(user, "multiple", request(date,
                new OrderService.LineRequest(9L, 1, List.of("rice", "quinoa")))));
    }

    @Test
    void returnsThePriorOrderForAnIdempotentReplay() {
        var prior = new OrderEntity("WBX-PRIOR", user, TODAY.plusDays(1), MealPeriod.Lunch, "Building A", "same-key");
        prior.addItem(new OrderItem(prior, dish, new BigDecimal("32.00"), 1, "[]"));
        when(orders.findByUserIdAndIdempotencyKey(77L, "same-key")).thenReturn(Optional.of(prior));

        var response = serviceAt(8, 0).place(user, "same-key", request(TODAY.plusDays(1),
                new OrderService.LineRequest(9L, 1, List.of("rice"))));

        assertThat(response.orderNumber()).isEqualTo("WBX-PRIOR");
        verify(menus, never()).lockForOrder(any(), any());
        verify(orders, never()).saveAndFlush(any());
    }

    @Test
    void rejectsASecondActiveOrderForTheSameMealSlot() {
        var existing = mock(OrderEntity.class);
        when(existing.getId()).thenReturn(44L);
        when(orders.findByActiveSlotKey("77|2026-08-18|Lunch")).thenReturn(Optional.of(existing));

        assertApiError("ORDER_SLOT_ALREADY_EXISTS", () -> serviceAt(8, 0).place(user, "another-order",
                request(TODAY.plusDays(1), new OrderService.LineRequest(9L, 1, List.of("rice")))));
        verify(menus, never()).lockForOrder(any(), any());
    }

    @Test
    void resolvesTheActiveOrderUsingTheServerSideCutoffSlot() {
        var existing = new OrderEntity("WBX-ACTIVE", user, TODAY, MealPeriod.Dinner, "Building A", "active-key");
        existing.addItem(new OrderItem(existing, dish, new BigDecimal("32.00"), 1, "[]"));
        when(orders.findByActiveSlotKey("77|2026-08-17|Dinner")).thenReturn(Optional.of(existing));

        var response = serviceAt(10, 30).active(user, TODAY, MealPeriod.Lunch);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().orderNumber()).isEqualTo("WBX-ACTIVE");
    }

    @Test
    void cancellationRestoresInventoryOnlyOnce() {
        var order = new OrderEntity("WBX-CANCEL", user, TODAY.plusDays(1), MealPeriod.Lunch, "Building A", "cancel-key");
        order.addItem(new OrderItem(order, dish, new BigDecimal("35.00"), 2, "[\"Quinoa\"]"));
        when(orders.lockByIdAndUserId(123L, 77L)).thenReturn(Optional.of(order));
        when(menus.lockForRestore(TODAY.plusDays(1), List.of(9L))).thenReturn(List.of(menuItem));
        var service = serviceAt(8, 0);

        assertThat(service.cancel(user, 123L).status()).isEqualTo(OrderStatus.Cancelled);
        verify(menuItem).restore(2);
        assertApiError("ORDER_NOT_CANCELLABLE", () -> service.cancel(user, 123L));
        verify(menuItem).restore(2);
    }

    @Test
    void appliesTheLunchAndDinnerCutoffBoundaries() {
        assertThat(serviceAt(9, 59).effectiveSlot(TODAY, MealPeriod.Lunch))
                .isEqualTo(new OrderService.EffectiveSlot(TODAY, MealPeriod.Lunch));
        assertThat(serviceAt(10, 0).effectiveSlot(TODAY, MealPeriod.Lunch))
                .isEqualTo(new OrderService.EffectiveSlot(TODAY, MealPeriod.Dinner));
        assertThat(serviceAt(14, 59).effectiveSlot(TODAY, MealPeriod.Dinner))
                .isEqualTo(new OrderService.EffectiveSlot(TODAY, MealPeriod.Dinner));
        assertThat(serviceAt(15, 0).effectiveSlot(TODAY, MealPeriod.Dinner))
                .isEqualTo(new OrderService.EffectiveSlot(TODAY.plusDays(1), MealPeriod.Lunch));
        assertApiError("ORDER_DATE_IN_PAST",
                () -> serviceAt(8, 0).effectiveSlot(TODAY.minusDays(1), MealPeriod.Lunch));
    }

    private OrderService serviceAt(int hour, int minute) {
        var instant = ZonedDateTime.of(2026, 8, 17, hour, minute, 0, 0, BUSINESS_ZONE).toInstant();
        return new OrderService(orders, menus, objectMapper,
                new AppProperties(BUSINESS_ZONE.getId(), "http://localhost:5173"),
                Clock.fixed(instant, BUSINESS_ZONE));
    }

    private OrderService.PlaceOrderRequest request(LocalDate date, OrderService.LineRequest... lines) {
        return new OrderService.PlaceOrderRequest(date, MealPeriod.Lunch, "Building A", List.of(lines));
    }

    private void assertApiError(String code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo(code));
    }
}
