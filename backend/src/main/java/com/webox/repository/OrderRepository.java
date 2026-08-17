package com.webox.repository;

import com.webox.model.OrderEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    @EntityGraph(attributePaths = {"items", "items.dish"})
    Optional<OrderEntity> findByActiveSlotKey(String activeSlotKey);

    @EntityGraph(attributePaths = {"items", "items.dish"})
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.dish"})
    Optional<OrderEntity> findWithItemsByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.dish"})
    @Query("select o from OrderEntity o where o.id = :id and o.user.id = :userId")
    Optional<OrderEntity> lockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
