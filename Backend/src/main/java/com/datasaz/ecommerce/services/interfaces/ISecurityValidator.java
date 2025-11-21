package com.datasaz.ecommerce.services.interfaces;

import io.jsonwebtoken.Claims;

public interface ISecurityValidator {

    public Claims validateToken(String token);
}
