package com.datasaz.ecommerce.exceptions;

import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@Getter
public class ProductNotFoundException extends EntityNotFoundException {
    private final String message;
}
