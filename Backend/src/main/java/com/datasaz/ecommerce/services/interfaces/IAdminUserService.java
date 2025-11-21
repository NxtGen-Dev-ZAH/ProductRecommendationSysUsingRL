package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.UpdatePasswordRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface IAdminUserService {

    Page<UserDto> findAll(int page, int size);

    Optional<UserDto> findById(Long id);

    Optional<UserDto> findByEmail(String email);
    void deleteById(Long id);

    UserDto updateUserPassword(UpdatePasswordRequest updatePasswordRequest);
    UserDto restoreUser(String email);
    String blockUser(String email, String reason);
    String unblockUser(String email);
}