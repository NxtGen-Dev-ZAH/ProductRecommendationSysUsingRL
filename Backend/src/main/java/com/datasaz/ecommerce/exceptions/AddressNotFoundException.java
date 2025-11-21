package com.datasaz.ecommerce.exceptions;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@Getter
public class AddressNotFoundException extends RuntimeException {
    private final String message;
}
