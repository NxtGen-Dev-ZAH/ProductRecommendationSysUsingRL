package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Long> {
    Optional<Roles> findByRole(RoleTypes roleTypes);

    boolean existsByRole(RoleTypes roleTypes);

    @Query("SELECT r FROM Roles r, User u where u.id = ?1")
    Set<Roles> findByUserId(Long userId);
}
