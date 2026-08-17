package com.webox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "user_preferences")
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "allergens_json", nullable = false, columnDefinition = "json")
    private String allergensJson = "[]";

    @Column(name = "cuisines_json", nullable = false, columnDefinition = "json")
    private String cuisinesJson = "[]";

    @Column(name = "spice_level", length = 20)
    private String spiceLevel;

    @Column(name = "taste_intensity", length = 20)
    private String tasteIntensity;

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    protected UserPreference() {}

    public UserPreference(User user) { this.user = user; }

    public String getAllergensJson() { return allergensJson; }
    public void setAllergensJson(String allergensJson) { this.allergensJson = allergensJson; }
    public String getCuisinesJson() { return cuisinesJson; }
    public void setCuisinesJson(String cuisinesJson) { this.cuisinesJson = cuisinesJson; }
    public String getSpiceLevel() { return spiceLevel; }
    public void setSpiceLevel(String spiceLevel) { this.spiceLevel = spiceLevel; }
    public String getTasteIntensity() { return tasteIntensity; }
    public void setTasteIntensity(String tasteIntensity) { this.tasteIntensity = tasteIntensity; }
    public BigDecimal getBudgetMin() { return budgetMin; }
    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }
    public BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }
}
