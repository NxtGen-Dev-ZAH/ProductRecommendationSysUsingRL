package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.entities.User;

public interface IUserService {

    //User updateUserPassword(RegisterRequest registerRequest, Long id);


    UserDto updateUserPassword(String email, String oldPassword, String newPassword, String jwtToken);

    UserDto updateUser(UserDto userDto, String jwtToken);

    UserDto changeEmail(String oldEmail, String newEmail, String password, String jwtToken);

    void deleteUser(String password, String jwtToken);

    UserDto confirmDeleteUser(String token, String jwtToken);

    void completeUserDeletion(User user, String jwtToken);

    String logout(String username, String refreshToken, String jwtToken);

    //User saveNewUser(RegisterRequest registerRequest);

//    void saveResetPassword(String email, String resetToken);
//
//    Optional<User> findByResetToken(String resetToken);


//    void deleteById(Long id);
//
//    List<User> findAll();
//
//    Optional<User> findById(Long id);
//
//    Optional<User> findByEmail(String email);


//    UserDto restoreUser(String email);




}
