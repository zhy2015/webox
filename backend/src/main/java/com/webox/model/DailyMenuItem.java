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
import jakarta.persistence.Version;
import java.time.LocalDate;

@Entity
@Table(name = "daily_menu_items")
public class DailyMenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;
    @Column(name = "initial_stock", nullable = false)
    private int initialStock;
    @Column(name = "remaining_stock", nullable = false)
    private int remainingStock;
    @Version
    private long version;

    protected DailyMenuItem() {}
    public DailyMenuItem(LocalDate menuDate, Dish dish, int stock) {
        this.menuDate = menuDate;
        this.dish = dish;
        this.initialStock = stock;
        this.remainingStock = stock;
    }
    public Long getId() { return id; }
    public LocalDate getMenuDate() { return menuDate; }
    public Dish getDish() { return dish; }
    public int getInitialStock() { return initialStock; }
    public int getRemainingStock() { return remainingStock; }
    public int getAllocatedStock() { return initialStock - remainingStock; }
    public void configureStock(int stock) {
        int consumed = getAllocatedStock();
        if (stock < consumed) {
            throw new IllegalArgumentException("Stock cannot be lower than the quantity already allocated.");
        }
        initialStock = stock;
        remainingStock = stock - consumed;
    }
    public void deduct(int quantity) { remainingStock -= quantity; }
    public void restore(int quantity) { remainingStock = Math.min(initialStock, remainingStock + quantity); }
}
