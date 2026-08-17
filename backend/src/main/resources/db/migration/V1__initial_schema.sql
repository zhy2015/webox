CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(200) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    allergens_json JSON NOT NULL,
    cuisines_json JSON NOT NULL,
    spice_level VARCHAR(20),
    taste_intensity VARCHAR(20),
    budget_min DECIMAL(12,2),
    budget_max DECIMAL(12,2),
    CONSTRAINT uk_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE dishes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    category VARCHAR(40) NOT NULL,
    protein VARCHAR(100) NOT NULL,
    allergens_json JSON NOT NULL,
    spice_level VARCHAR(20) NOT NULL,
    options_json JSON NOT NULL,
    image_url VARCHAR(300) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_dishes_category_published ON dishes(category, published);

CREATE TABLE daily_menu_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    menu_date DATE NOT NULL,
    dish_id BIGINT NOT NULL,
    initial_stock INT NOT NULL,
    remaining_stock INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_daily_menu_date_dish UNIQUE (menu_date, dish_id),
    CONSTRAINT fk_daily_menu_dish FOREIGN KEY (dish_id) REFERENCES dishes(id),
    CONSTRAINT chk_daily_menu_initial_stock CHECK (initial_stock >= 0),
    CONSTRAINT chk_daily_menu_remaining_stock CHECK (remaining_stock >= 0)
);

CREATE INDEX idx_daily_menu_date ON daily_menu_items(menu_date);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    delivery_date DATE NOT NULL,
    meal_period VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(200) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    active_slot_key VARCHAR(120),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT uk_orders_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT uk_orders_active_slot UNIQUE (active_slot_key),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    dish_name_snapshot VARCHAR(120) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    selected_options_json JSON NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_dish FOREIGN KEY (dish_id) REFERENCES dishes(id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0)
);
