package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.imageAttaches WHERE p.deleted = false")
    Page<Product> findAllWithImageAttaches(Pageable pageable);

    //Page<Product> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.company " +
            "WHERE p.company.id = :companyId AND p.deleted = false")
    Page<Product> findByCompanyIdAndDeletedFalse(@Param("companyId") Long companyId, Pageable pageable);


    List<Product> findByCompanyId(Long companyId);
    //Optional<Company> findCompanyById(Long companyId);

//    @Query("SELECT p FROM Product p WHERE p.author.id = :authorId " +
//            "AND p.company IS NULL AND p.deleted = false")
//    Page<Product> findByAuthorIdAndDeletedFalse(Long authorId, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.company " +
            "WHERE p.author.id = :authorId AND p.company IS NULL AND p.deleted = false")
    Page<Product> findByAuthorIdAndDeletedFalse(@Param("authorId") Long authorId, Pageable pageable);

//    @Query("SELECT p FROM Product p WHERE " +
//            "p.author.emailAddress = :authorEmailAddress AND" +
//            " p.company IS NULL AND p.deleted = false")
//    Page<Product> findByAuthorEmailAddressAndDeletedFalse(String authorEmailAddress, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.company " +
            "WHERE p.author.emailAddress = :authorEmailAddress AND p.company IS NULL AND p.deleted = false")
    Page<Product> findByAuthorEmailAddressAndDeletedFalse(@Param("authorEmailAddress") String authorEmailAddress, Pageable pageable);

//    @Query("SELECT p FROM Product p WHERE p.author.emailAddress = :authorEmailAddress AND p.company IS NULL AND p.deleted = false")
//    List<Product> findByAuthorEmailAddressAndDeletedFalse(String authorEmailAddress);


    // Optimistic locking (default with @Version)
    Optional<Product> findById(Long id);

//    @Query("SELECT p FROM Product p " +
//            "LEFT JOIN FETCH p.imageAttaches " +
//            "WHERE p.id = :id AND p.deleted = false")
//    Optional<Product> findWithImageAttachesByIdAndDeletedFalse(@Param("id") Long id);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.company " +
            "LEFT JOIN FETCH p.imageAttaches " +
            "WHERE p.id = :id AND p.deleted = false")
    Optional<Product> findWithImageAttachesByIdAndDeletedFalse(@Param("id") Long id);

//    @Query("SELECT p FROM Product p WHERE " +
//            "p.id = :id AND p.deleted = false")
//    Optional<Product> findByIdAndDeletedFalse(@Param("id") Long id);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.company " +
            "WHERE p.id = :id AND p.deleted = false")
    Optional<Product> findByIdAndDeletedFalse(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.usersFavourite WHERE p.id = :id AND p.deleted = false")
    Optional<Product> findByIdAndDeletedFalseWithFavouriteUsers(@Param("id") Long id);


    @Query("SELECT p FROM Product p JOIN p.usersFavourite u WHERE u.id = :userId AND p.deleted = false")
    Page<Product> findFavoriteProductsByUserId(Long userId, Pageable pageable);

    Page<Product> findByDeletedFalse(Pageable pageable);

    // Pessimistic locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(Long id);
    List<Product> findByCategoryId(Long categoryId);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) and p.deleted = false and p.author.id = :authorId")
    List<Product> findByNameContainingIgnoreCaseAuthorIdDeletedFalse(@Param("name") String name, @Param("authorId") Long authorId);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.author " +
            "LEFT JOIN FETCH p.company " +
            "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.deleted = false AND p.author.id = :authorId")
    Page<Product> findByNameContainingIgnoreCaseAuthorIdDeletedFalse(
            @Param("name") String name,
            @Param("authorId") Long authorId,
            Pageable pageable);

//    @Query("SELECT p FROM Product p " +
//            "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
//            "and p.deleted = false and p.author.id = :authorId")
//    Page<Product> findByNameContainingIgnoreCaseAuthorIdDeletedFalse(@Param("name") String name, @Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.imageAttaches WHERE lower(p.name) LIKE lower(concat('%', :name, '%')) AND p.deleted = false")
    Page<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);

    @Query("SELECT p FROM Product p, OrderItem oi WHERE p.id = oi.product.id and p.deleted = false ORDER BY COUNT(oi.quantity) DESC")
    Page<Product> findAllSortedByProductSoldQuantity(Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.company = :company WHERE p.author.id = :authorId AND p.company IS NULL AND p.deleted = false")
    void updateCompanyForAuthorProducts(Long authorId, Company company);

    //Optional<Product> findByIdAndDeletedFalse(Long id);

    Page<Product> findAllByDeletedFalse(Pageable pageable);


    // Non-paginated version for backward compatibility (if needed)
//    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.imageAttaches WHERE lower(p.name) LIKE lower(concat('%', :name, '%')) AND p.deleted = false")
//    List<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

    Page<Product> findByCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);

    //List<Product> findByNameContainingIgnoreCaseAuthorIdDeletedFalse(String name, Long authorId);

    //Page<Product> findByNameContainingIgnoreCaseAuthorIdDeletedFalse(String name, Long authorId, Pageable pageable);

    //Page<Product> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

//    @Modifying
//    @Query("UPDATE Product p SET p.company = :company WHERE p.author.id = :authorId AND p.deleted = false")
//    void updateCompanyForAuthorProducts(Long authorId, Company company);

//    @Modifying
//    @Query("UPDATE Product p SET p.author.id = :newAuthorId WHERE p.author.id = :oldAuthorId AND p.deleted = false")
//    void updateAuthorForUserProducts(@Param("oldAuthorId") Long oldAuthorId, @Param("newAuthorId") Long newAuthorId);

    @Query("SELECT p FROM Product p " +
            "WHERE p.offerPrice < p.price AND p.deleted = false")
    Page<Product> findByOfferPriceLessThanPriceAndDeletedFalse(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :catId AND p.id != :excludeId AND p.deleted = false")
    Page<Product> findByCategoryIdAndIdNotAndDeletedFalse(@Param("catId") Long catId, @Param("excludeId") Long excludeId, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.author.id = :newAuthorId WHERE p.author.id = :oldAuthorId AND p.company.id = :companyId AND p.deleted = false")
    void updateAuthorForUserProducts(@Param("oldAuthorId") Long oldAuthorId, @Param("newAuthorId") Long newAuthorId, @Param("companyId") Long companyId);

    @Modifying
    @Query("UPDATE Product p SET p.deleted = true WHERE p.author.id = :authorId AND p.deleted = false")
    void deleteByAuthorId(@Param("authorId") Long authorId);

}