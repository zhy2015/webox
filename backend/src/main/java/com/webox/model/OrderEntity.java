package com.webox.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    private String orderNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_period", nullable = false, length = 20)
    private MealPeriod mealPeriod;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;
    @Column(name = "delivery_address", nullable = false, length = 200)
    private String deliveryAddress;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
    @Column(name = "active_slot_key", unique = true, length = 120)
    private String activeSlotKey;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected OrderEntity() {}
    public OrderEntity(String orderNumber, User user, LocalDate deliveryDate, MealPeriod mealPeriod,
                       String deliveryAddress, String idempotencyKey) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.deliveryDate = deliveryDate;
        this.mealPeriod = mealPeriod;
        this.status = OrderStatus.Pending;
        this.deliveryAddress = deliveryAddress;
        this.idempotencyKey = idempotencyKey;
        this.activeSlotKey = user.getId() + "|" + deliveryDate + "|" + mealPeriod;
        this.totalAmount = BigDecimal.ZERO;
    }
    public void addItem(OrderItem item) { items.add(item); totalAmount = totalAmount.add(item.getLineTotal()); }
    public void cancel() { status = OrderStatus.Cancelled; activeSlotKey = null; }
    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public User getUser() { return user; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public MealPeriod getMealPeriod() { return mealPeriod; }
    public OrderStatus getStatus() { return status; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
