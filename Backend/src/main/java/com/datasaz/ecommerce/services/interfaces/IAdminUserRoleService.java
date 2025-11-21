package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.entities.Roles;

import java.util.Set;

public interface IAdminUserRoleService {

    UserDto addUserRole(Long userId, String role);

    UserDto removeUserRole(Long userId, String role);

    Set<Roles> getUserRoles(Long userId);

    UserDto assignSellerRole(String email);

    UserDto removeSellerRole(String email);

    UserDto assignRole(String email, String roleName);

    UserDto removeRole(String email, String roleName);

}
