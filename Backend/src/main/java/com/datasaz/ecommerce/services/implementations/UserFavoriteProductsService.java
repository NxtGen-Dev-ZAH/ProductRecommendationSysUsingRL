package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.services.interfaces.IUserFavoriteProductsService;
import com.datasaz.ecommerce.utilities.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserFavoriteProductsService implements IUserFavoriteProductsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Utility utility;

    //private UserDto userDto = new UserDto();
    private final UserMapper userMapper;

    @Override
    public Set<Product> getFavoriteProducts(Long userId) {

        return userRepository.findFavoriteProductsByUserId(userId);
    }
}
