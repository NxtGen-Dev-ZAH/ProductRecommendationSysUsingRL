package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.Product;

import java.util.Set;

public interface IUserFavoriteProductsService {

    Set<Product> getFavoriteProducts(Long userId);
    //   Set<Long> getFavoriteProducts(User user);


//   void clearFavoriteProducts(User user);
//
//   void addFavoriteProduct(User user, Long productId);
//   void removeFavoriteProduct(User user, Long productId);
//
//   boolean isFavoriteProduct(User user, Long productId);
//
//   void deleteFavoriteProducts(User user);
//   void deleteAllFavoriteProducts();
//   void deleteAllFavoriteProducts(User user);
//   void deleteFavoriteProduct(Long productId);
//   void deleteFavoriteProduct(Long productId, User user);
//   void deleteAllFavoriteProducts(Long userId);
//   void deleteAllFavoriteProducts(Long userId, User user);
//   void deleteFavoriteProducts(Long userId, User user);
//   void deleteFavoriteProducts(Long userId);
//   void deleteFavoriteProducts(Long productId, Long userId);

}
