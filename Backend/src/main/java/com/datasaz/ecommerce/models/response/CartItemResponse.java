package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Getter;

//@Data
@Builder
@Getter
public class CartItemResponse {

    private Long id;
    private Long productId;
    private int quantity;
}
