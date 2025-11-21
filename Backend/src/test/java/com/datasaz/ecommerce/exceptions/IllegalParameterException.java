package com.datasaz.ecommerce.exceptions;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class IllegalParameterException extends IllegalArgumentException {
    private final String message;
}



