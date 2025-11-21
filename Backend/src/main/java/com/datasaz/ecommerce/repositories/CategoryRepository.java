package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Category> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("""
                SELECT p.category FROM User u
                JOIN u.favoriteProducts p
                GROUP BY p.category
                ORDER BY COUNT(p) DESC
            """)
    Page<Category> findAllSortedByProductFavorites(Pageable pageable);

    @Query("""
                SELECT p.category FROM ProductStatistics s
                JOIN s.product p
                GROUP BY p.category
                ORDER BY SUM(COALESCE(s.announceViews, 0)) DESC
            """)
    Page<Category> findAllSortedByProductViews(Pageable pageable);

    @Query("""
                SELECT p.category FROM Product p
                GROUP BY p.category
                ORDER BY SUM(COALESCE(p.quantity, 0)) DESC
            """)
    Page<Category> findAllSortedByProductQuantity(Pageable pageable);

    @Query("""
                SELECT oi.product.category FROM OrderItem oi
                GROUP BY oi.product.category
                ORDER BY SUM(COALESCE(oi.quantity, 0)) DESC
            """)
    Page<Category> findAllSortedByProductSoldQuantity(Pageable pageable);

    @Query("""
                SELECT p.category FROM Product p
                GROUP BY p.category
                ORDER BY COUNT(p) DESC
            """)
    Page<Category> findAllSortedByProductCount(Pageable pageable);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findByParentIsNull();

    @Query("SELECT c FROM Category c WHERE c.subcategories IS EMPTY")
    List<Category> findBySubcategoriesIsEmpty();
}