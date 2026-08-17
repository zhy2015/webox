package com.webox.config;

import com.webox.model.DailyMenuItem;
import com.webox.model.Dish;
import com.webox.model.Role;
import com.webox.model.User;
import com.webox.repository.DailyMenuItemRepository;
import com.webox.repository.DishRepository;
import com.webox.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final DishRepository dishes;
    private final DailyMenuItemRepository menus;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public DemoDataSeeder(UserRepository users, DishRepository dishes, DailyMenuItemRepository menus,
                          PasswordEncoder passwordEncoder, AppProperties properties) {
        this.users = users;
        this.dishes = dishes;
        this.menus = menus;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("employee@webox.local", "Lunch123", Role.EMPLOYEE);
        seedUser("admin@webox.local", "Admin123", Role.ADMIN);
        if (dishes.count() == 0) seedDishes();
        var today = LocalDate.now(properties.zoneId());
        seedMenu(today);
        seedMenu(today.plusDays(1));
    }

    private void seedUser(String email, String password, Role role) {
        if (!users.existsByEmailIgnoreCase(email)) users.save(new User(email, passwordEncoder.encode(password), role));
    }

    private void seedDishes() {
        dishes.saveAll(List.of(
                dish("Kung Pao Chicken", "Sichuan chicken stir-fried with peanuts and dried chili.", "22.00", "Chinese", "Chicken", "[\"Peanuts\"]", "Medium", "[]", 20),
                dish("Caesar Salad", "Crisp romaine, Parmesan and classic Caesar dressing.", "28.50", "Light Meal", "Chicken", "[\"Dairy\",\"Egg\"]", "None",
                        "[{\"id\":\"add-ons\",\"label\":\"Add-ons\",\"required\":false,\"multiple\":true,\"options\":[{\"id\":\"grilled-chicken\",\"label\":\"Grilled Chicken\",\"extraPrice\":6.00},{\"id\":\"bacon\",\"label\":\"Bacon\",\"extraPrice\":5.00},{\"id\":\"avocado\",\"label\":\"Avocado\",\"extraPrice\":4.00}]}]", 12),
                dish("Salmon Sashimi Set", "Fresh salmon sashimi with rice and miso soup.", "45.00", "Japanese", "Fish", "[\"Fish\"]", "None", "[]", 10),
                dish("Tomato Pasta", "Italian tomato pasta finished with basil.", "26.50", "Western", "None", "[\"Gluten\"]", "None",
                        "[{\"id\":\"pasta\",\"label\":\"Pasta type\",\"required\":true,\"multiple\":false,\"options\":[{\"id\":\"spaghetti\",\"label\":\"Spaghetti\",\"extraPrice\":0},{\"id\":\"fusilli\",\"label\":\"Fusilli\",\"extraPrice\":0},{\"id\":\"penne\",\"label\":\"Penne\",\"extraPrice\":0}]},{\"id\":\"add-ons\",\"label\":\"Add-ons\",\"required\":false,\"multiple\":true,\"options\":[{\"id\":\"bacon\",\"label\":\"Bacon\",\"extraPrice\":5},{\"id\":\"cheese\",\"label\":\"Cheese\",\"extraPrice\":3}]}]", 14),
                dish("Tom Yum Soup", "Thai hot and sour shrimp soup with lemongrass and lime leaves.", "32.00", "Southeast Asian", "Shrimp", "[\"Shellfish\"]", "Hot", "[]", 3),
                dish("Chicken Quinoa Bowl", "Grilled chicken, quinoa, avocado and seasonal vegetables.", "35.80", "Light Meal", "Chicken", "[]", "None",
                        "[{\"id\":\"base\",\"label\":\"Base\",\"required\":true,\"multiple\":false,\"options\":[{\"id\":\"quinoa\",\"label\":\"Quinoa\",\"extraPrice\":0},{\"id\":\"brown-rice\",\"label\":\"Brown Rice\",\"extraPrice\":0},{\"id\":\"mixed-grains\",\"label\":\"Mixed Grains\",\"extraPrice\":0}]}]", 6),
                dish("Mapo Tofu", "Silken tofu and minced pork in a fragrant Sichuan sauce.", "18.00", "Chinese", "Tofu, Pork", "[\"Soy\"]", "Medium", "[]", 16),
                dish("Korean Bibimbap", "Stone-pot rice with vegetables, egg and chili sauce.", "30.00", "Korean", "Egg", "[\"Egg\",\"Soy\"]", "Mild",
                        "[{\"id\":\"add-ons\",\"label\":\"Add-ons\",\"required\":false,\"multiple\":true,\"options\":[{\"id\":\"cheese\",\"label\":\"Cheese\",\"extraPrice\":3},{\"id\":\"fried-egg\",\"label\":\"Fried Egg\",\"extraPrice\":2},{\"id\":\"beef\",\"label\":\"Beef Slices\",\"extraPrice\":8}]}]", 18),
                dish("Classic Beef Burger", "Angus beef patty with lettuce, tomato and onion.", "38.00", "Western", "Beef", "[\"Gluten\",\"Dairy\"]", "None",
                        "[{\"id\":\"bun\",\"label\":\"Bun\",\"required\":true,\"multiple\":false,\"options\":[{\"id\":\"plain\",\"label\":\"Plain\",\"extraPrice\":0},{\"id\":\"whole-wheat\",\"label\":\"Whole Wheat\",\"extraPrice\":0}]},{\"id\":\"sauce\",\"label\":\"Sauce\",\"required\":true,\"multiple\":false,\"options\":[{\"id\":\"ketchup\",\"label\":\"Ketchup\",\"extraPrice\":0},{\"id\":\"mustard\",\"label\":\"Mustard\",\"extraPrice\":0},{\"id\":\"mayo\",\"label\":\"Mayo\",\"extraPrice\":0},{\"id\":\"bbq\",\"label\":\"BBQ\",\"extraPrice\":0}]}]", 19)
        ));
    }

    private Dish dish(String name, String description, String price, String category, String protein,
                      String allergens, String spice, String options, int imageNumber) {
        return new Dish(name, description, new BigDecimal(price), category, protein, allergens, spice, options,
                String.format("/images/dish-%02d.jpg", imageNumber), true);
    }

    private void seedMenu(LocalDate date) {
        if (!menus.findByMenuDateOrderById(date).isEmpty()) return;
        int stock = 12;
        for (var dish : dishes.findAllByOrderByNameAsc()) {
            menus.save(new DailyMenuItem(date, dish, stock++ % 8 + 4));
        }
    }
}
