package com.webox.repository;

import com.webox.model.DailyMenuItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyMenuItemRepository extends JpaRepository<DailyMenuItem, Long> {
    @Query("select m from DailyMenuItem m join fetch m.dish d where m.menuDate = :date and d.published = true order by d.name")
    List<DailyMenuItem> findPublishedMenu(@Param("date") LocalDate date);

    @Query("select m from DailyMenuItem m join fetch m.dish where m.menuDate = :date order by m.id")
    List<DailyMenuItem> findByMenuDateOrderById(@Param("date") LocalDate date);

    Optional<DailyMenuItem> findByMenuDateAndDishId(LocalDate date, Long dishId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from DailyMenuItem m join fetch m.dish where m.menuDate = :date and m.dish.id in :dishIds order by m.dish.id")
    List<DailyMenuItem> lockForOrder(@Param("date") LocalDate date, @Param("dishIds") Collection<Long> dishIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from DailyMenuItem m where m.menuDate = :date and m.dish.id in :dishIds order by m.dish.id")
    List<DailyMenuItem> lockForRestore(@Param("date") LocalDate date, @Param("dishIds") Collection<Long> dishIds);
}
