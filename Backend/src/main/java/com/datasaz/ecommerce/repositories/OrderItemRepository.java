package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.OrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Optimistic locking (default with @Version)
    Optional<OrderItem> findById(Long id);

    List<OrderItem> findByIdIn(List<Long> ids);

    // Pessimistic locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id")
    Optional<OrderItem> findByIdWithLock(Long id);

    // Find items by order
    List<OrderItem> findByOrderId(Long orderId);

}