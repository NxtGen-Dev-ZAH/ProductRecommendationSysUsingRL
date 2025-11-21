package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.product WHERE ci.cart = :cart")
    List<CartItem> findItemsByCart(@Param("cart") Cart cart);

    @Query("SELECT ci FROM CartItem ci LEFT JOIN FETCH ci.product p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.company WHERE ci.cart = :cart")
    List<CartItem> findItemsByCartWithProductDetails(@Param("cart") Cart cart);

}
