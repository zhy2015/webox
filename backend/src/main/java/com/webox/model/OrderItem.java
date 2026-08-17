package com.webox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;
    @Column(name = "dish_name_snapshot", nullable = false, length = 120)
    private String dishNameSnapshot;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "selected_options_json", nullable = false, columnDefinition = "json")
    private String selectedOptionsJson;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {}
    public OrderItem(OrderEntity order, Dish dish, BigDecimal unitPrice, int quantity, String selectedOptionsJson) {
        this.order = order;
        this.dish = dish;
        this.dishNameSnapshot = dish.getName();
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.selectedOptionsJson = selectedOptionsJson;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    public Dish getDish() { return dish; }
    public String getDishNameSnapshot() { return dishNameSnapshot; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public String getSelectedOptionsJson() { return selectedOptionsJson; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
