package com.datasaz.ecommerce.exceptions;

import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResourceNotFoundException extends EntityNotFoundException {
    private final String message;
}



