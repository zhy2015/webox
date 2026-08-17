package com.webox.repository;

import com.webox.model.Dish;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findAllByOrderByNameAsc();
}
