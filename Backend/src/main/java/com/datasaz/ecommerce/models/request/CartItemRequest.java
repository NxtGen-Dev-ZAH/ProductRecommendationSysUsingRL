package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
    @NotNull
    private Long productId;
    //    @NotNull
//    private Long cartId;
    @Positive
    private int quantity;
}
