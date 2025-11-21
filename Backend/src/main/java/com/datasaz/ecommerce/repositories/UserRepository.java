package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAddress(String emailAddress);

    //@Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.following WHERE u.emailAddress = :emailAddress AND u.deleted = false")
    //@Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.following LEFT JOIN FETCH u.favoriteProducts WHERE u.emailAddress = :emailAddress AND u.deleted = false")
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles WHERE u.emailAddress = :emailAddress AND u.deleted = false")
    Optional<User> findByEmailAddressAndDeletedFalse(@Param("emailAddress") String emailAddress);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.following WHERE u.emailAddress = :emailAddress AND u.deleted = false")
    Optional<User> findByEmailAddressAndDeletedFalseWithFollowing(@Param("emailAddress") String emailAddress);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.following WHERE u.id = :userId AND u.deleted = false")
    Optional<User> findByIdAndDeletedFalseWithFollowing(@Param("userId") Long userId);

//    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.id = :userId AND u.deleted = false")
//    Optional<User> findByIdAndDeletedFalseWithAddresses(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.followers WHERE u.emailAddress = :emailAddress AND u.deleted = false")
    Optional<User> findByEmailAddressAndDeletedFalseWithFollowers(@Param("emailAddress") String emailAddress);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.followers WHERE u.id = :userId AND u.deleted = false")
    Optional<User> findByIdAndDeletedFalseWithFollowers(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.favoriteProducts WHERE u.emailAddress = :emailAddress AND u.deleted = false")
    Optional<User> findByEmailAddressAndDeletedFalseWithFavoriteProducts(@Param("emailAddress") String emailAddress);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM User u JOIN u.following f WHERE u.emailAddress = :followerEmail AND f.emailAddress = :followingEmail AND u.deleted = false AND f.deleted = false")
    boolean isFollowing(@Param("followerEmail") String followerEmail, @Param("followingEmail") String followingEmail);

    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId AND u.deleted = false")
    Page<User> findFollowersByUserId(Long userId, Pageable pageable);

    @Query("SELECT f FROM User u JOIN u.following f WHERE u.id = :userId AND u.deleted = false")
    Page<User> findFollowingByUserId(Long userId, Pageable pageable);

    Optional<User> findByDeletionToken(String deletionToken);

    Optional<User> findByActivationCode(String activationToken);

    Optional<User> findByResetToken(String resetToken);

    boolean existsByEmailAddressAndDeletedFalse(String emailAddress);

    // Add this method to check if user exists
    boolean existsByIdAndDeletedFalse(Long id);

    // If you still need the old method for backward compatibility
//    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.id = :userId AND u.deleted = false")
//    Optional<User> findByIdAndDeletedFalseWithAddresses(@Param("userId") Long userId);

    // Find Roles for a user
    @Query("SELECT u.userRoles FROM User u WHERE u.id = :userId")
    Set<Roles> findRolesByUserId(@Param("userId") Long userId);

//    @Query("SELECT ur.role FROM User u JOIN u.userRoles ur WHERE u.id = :userId")
//    Set<Roles> findRolesByUserId(@Param("userId") Long userId);


//    // Find Followers for a user
//    @Query("SELECT u.followers FROM User u WHERE u.id = :userId")
//    Set<User> findFollowersByUserId(@Param("userId") Long userId);
//
//    // Find Following for a user
//    @Query("SELECT u.following FROM User u WHERE u.id = :userId")
//    Set<User> findFollowingByUserId(@Param("userId") Long userId);

    @Query("SELECT u.emailAddress, u.firstName, u.lastName, u.profilePictureUrl " +
            "FROM User u WHERE u IN (SELECT f FROM User u2 JOIN u2.followers f WHERE u2.emailAddress = :emailAddress AND u2.deleted = false AND f.deleted = false)")
    Page<Object[]> findFollowersSummaryByEmailAddress(@Param("emailAddress") String emailAddress, Pageable pageable);

    @Query("SELECT u.emailAddress, u.firstName, u.lastName, u.profilePictureUrl " +
            "FROM User u WHERE u IN (SELECT f FROM User u2 JOIN u2.following f WHERE u2.emailAddress = :emailAddress AND u2.deleted = false AND f.deleted = false)")
    Page<Object[]> findFollowingSummaryByEmailAddress(@Param("emailAddress") String emailAddress, Pageable pageable);

    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.emailAddress = :emailAddress AND u.deleted = false AND f.deleted = false")
    Page<User> findFollowersByEmailAddress(@Param("emailAddress") String emailAddress, Pageable pageable);

    @Query("SELECT f FROM User u JOIN u.following f WHERE u.emailAddress = :emailAddress AND u.deleted = false AND f.deleted = false")
    Page<User> findFollowingByEmailAddress(@Param("emailAddress") String emailAddress, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM User u JOIN u.following f WHERE u.emailAddress = :followerEmail AND f.emailAddress = :followingEmail AND u.deleted = false AND f.deleted = false")
    boolean existsFollowRelationship(@Param("followerEmail") String followerEmail, @Param("followingEmail") String followingEmail);

    @Query("SELECT p FROM User u JOIN u.favoriteProducts p WHERE u.emailAddress = :emailAddress AND u.deleted = false AND p.deleted = false")
    Page<Product> findFavoriteProductsByEmailAddress(@Param("emailAddress") String emailAddress, Pageable pageable);

    // Find Favorite Products for a user
    @Query("SELECT u.favoriteProducts FROM User u WHERE u.id = :userId")
    Set<Product> findFavoriteProductsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.emailAddress = :emailAddress AND u.deleted = false AND f.deleted = false")
    Long countFollowersByEmailAddress(@Param("emailAddress") String emailAddress);

    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.emailAddress = :emailAddress AND u.deleted = false AND f.deleted = false")
    Long countFollowingByEmailAddress(@Param("emailAddress") String emailAddress);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId")
    List<User> findByCompanyId(@Param("companyId") Long companyId);

    //    @Query("SELECT COUNT(u) FROM User u JOIN u.userRoles r WHERE r.role = 'ROLE_COMPANY_ADMIN_SELLER' AND u.company.id = :companyId AND u.deleted = false")
//    long countCompanyAdminsByCompanyId(Long companyId);
    @Query("SELECT COUNT(u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE ur.role = 'COMPANY_ADMIN_SELLER' AND u.company.id = :companyId AND u.deleted=false ")
    long countCompanyAdminsByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.favoriteProducts LEFT JOIN FETCH u.followers LEFT JOIN FETCH u.following LEFT JOIN FETCH u.company LEFT JOIN FETCH u.customFields WHERE u.id = :userId")
    Optional<User> findByIdWithAllCollections(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.favoriteProducts LEFT JOIN FETCH u.followers LEFT JOIN FETCH u.following LEFT JOIN FETCH u.company LEFT JOIN FETCH u.customFields WHERE u.emailAddress = :emailAddress")
    Optional<User> findByEmailAddressWithAllCollections(@Param("emailAddress") String emailAddress);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles LEFT JOIN FETCH u.favoriteProducts LEFT JOIN FETCH u.followers LEFT JOIN FETCH u.following LEFT JOIN FETCH u.company LEFT JOIN FETCH u.customFields")
    Page<User> findAllWithAllCollections(Pageable pageable);
}
