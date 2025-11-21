package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.UserCustomFields;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomFieldsRepository extends JpaRepository<UserCustomFields, Long> {
    List<UserCustomFields> findByUserId(Long userId);

    Optional<UserCustomFields> findByUserIdAndFieldKey(Long userId, String fieldKey);

    Optional<UserCustomFields> findByFieldKey(String fieldKey);

    //List<UserCustomFields> findAll();
    void deleteByUserIdAndFieldKey(Long userId, String fieldKey);

    void deleteByUserId(Long userId);

    void deleteByFieldKey(String fieldKey);

    boolean existsByUserIdAndFieldKey(Long userId, String fieldKey);
}