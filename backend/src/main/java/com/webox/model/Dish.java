package com.webox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "dishes")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(nullable = false, length = 100)
    private String protein;
    @Column(name = "allergens_json", nullable = false, columnDefinition = "json")
    private String allergensJson;
    @Column(name = "spice_level", nullable = false, length = 20)
    private String spiceLevel;
    @Column(name = "options_json", nullable = false, columnDefinition = "json")
    private String optionsJson;
    @Column(name = "image_url", nullable = false, length = 300)
    private String imageUrl;
    @Column(nullable = false)
    private boolean published;

    protected Dish() {}

    public Dish(String name, String description, BigDecimal price, String category, String protein,
                String allergensJson, String spiceLevel, String optionsJson, String imageUrl, boolean published) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.protein = protein;
        this.allergensJson = allergensJson;
        this.spiceLevel = spiceLevel;
        this.optionsJson = optionsJson;
        this.imageUrl = imageUrl;
        this.published = published;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public String getProtein() { return protein; }
    public String getAllergensJson() { return allergensJson; }
    public String getSpiceLevel() { return spiceLevel; }
    public String getOptionsJson() { return optionsJson; }
    public String getImageUrl() { return imageUrl; }
    public boolean isPublished() { return published; }
    public void update(String name, String description, BigDecimal price, String category, String protein,
                       String allergensJson, String spiceLevel, String optionsJson, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.protein = protein;
        this.allergensJson = allergensJson;
        this.spiceLevel = spiceLevel;
        this.optionsJson = optionsJson;
        this.imageUrl = imageUrl;
    }
    public void setPublished(boolean published) { this.published = published; }
}
