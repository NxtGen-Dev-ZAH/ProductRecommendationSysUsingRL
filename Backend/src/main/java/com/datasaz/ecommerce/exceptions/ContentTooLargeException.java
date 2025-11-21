package com.datasaz.ecommerce.exceptions;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ContentTooLargeException extends RuntimeException {
    private final String message;
}



