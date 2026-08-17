package com.webox.order;

import com.webox.auth.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDate;
import com.webox.model.MealPeriod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final CurrentUserService currentUsers;
    private final OrderService orders;

    public OrderController(CurrentUserService currentUsers, OrderService orders) {
        this.currentUsers = currentUsers;
        this.orders = orders;
    }

    @PostMapping
    OrderService.OrderResponse place(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @Valid @RequestBody OrderService.PlaceOrderRequest request,
                                     Authentication authentication) {
        return orders.place(currentUsers.require(authentication), idempotencyKey, request);
    }

    @GetMapping
    List<OrderService.OrderResponse> list(Authentication authentication) {
        return orders.list(currentUsers.require(authentication));
    }

    @GetMapping("/active")
    ResponseEntity<OrderService.OrderResponse> active(@RequestParam LocalDate deliveryDate,
                                                       @RequestParam MealPeriod mealPeriod,
                                                       Authentication authentication) {
        return ResponseEntity.of(orders.active(currentUsers.require(authentication), deliveryDate, mealPeriod));
    }

    @GetMapping("/{id}")
    OrderService.OrderResponse get(@PathVariable Long id, Authentication authentication) {
        return orders.get(currentUsers.require(authentication), id);
    }

    @PostMapping("/{id}/cancel")
    OrderService.OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
        return orders.cancel(currentUsers.require(authentication), id);
    }
}
