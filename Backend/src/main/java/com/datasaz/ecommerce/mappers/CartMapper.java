package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import com.datasaz.ecommerce.repositories.entities.Coupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        return toResponse(cart, null);
    }

//        public CartResponse toResponse(Cart cart, Pageable pageable) {
//        if (cart == null) {
//            log.debug("toResponse: Cart is null, returning empty response");
//            return CartResponse.builder()
//                    .id(null)
//                    .sessionId(null)
//                    .userId(null)
//                    .items(Collections.emptyList())
//                    .couponResponse(null)
//                    .subtotalPrice(BigDecimal.ZERO)
//                    .totalShippingCost(BigDecimal.ZERO)
//                    .totalDiscount(BigDecimal.ZERO)
//                    .totalAmount(BigDecimal.ZERO)
//                    .build();
//        }
//
//        List<CartItemResponse> itemResponses = cart.getItems() != null
//                ? cart.getItems().stream()
//                .filter(item -> item != null)
//                .map(this::toItemResponse)
//                .filter(item -> item != null)
//                .collect(Collectors.toList())
//                : Collections.emptyList();
//
//        if (pageable != null && !itemResponses.isEmpty()) {
//            int start = (int) pageable.getOffset();
//            int end = Math.min(start + pageable.getPageSize(), itemResponses.size());
//            if (start < itemResponses.size()) {
//                itemResponses = itemResponses.subList(start, end);
//            } else {
//                itemResponses = Collections.emptyList();
//            }
//        }
//
//        BigDecimal subtotalPrice = calculateSubtotalPrice(cart);
//        BigDecimal totalShippingCost = calculateTotalShippingCost(cart);
//        BigDecimal totalDiscount = cart.getTotalDiscount() != null ? cart.getTotalDiscount() : BigDecimal.ZERO;
//        BigDecimal totalAmount = cart.getSubtotalPrice() != null ? cart.getSubtotalPrice() : subtotalPrice.subtract(totalDiscount).max(BigDecimal.ZERO);
//
//        return CartResponse.builder()
//                .id(cart.getId())
//                .sessionId(cart.getSessionId())
//                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
//                .items(itemResponses)
//                .couponResponse(toCouponResponse(cart.getCoupon())) // Map Coupon to CouponResponse
//                .subtotalPrice(subtotalPrice)
//                .totalShippingCost(totalShippingCost)
//                .totalDiscount(totalDiscount)
//                .totalAmount(totalAmount)
//                .build();
//    }

    public CartResponse toResponse(Cart cart, Pageable pageable) {
        if (cart == null) {
            return emptyResponse();
        }

        List<CartItemResponse> itemResponses = Optional.ofNullable(cart.getItems())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .map(this::toItemResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (pageable != null && !itemResponses.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), itemResponses.size());
            itemResponses = start < itemResponses.size() ? itemResponses.subList(start, end) : Collections.emptyList();
        }

        BigDecimal subtotalPrice = subtotalPriceOf(cart);
        BigDecimal totalShippingCost = totalShippingCostOf(cart);
        BigDecimal totalDiscount = Optional.ofNullable(cart.getTotalDiscount()).orElse(BigDecimal.ZERO);

        // FIX: Use calculated subtotal, not DB field
        BigDecimal totalAmount = subtotalPrice
                .add(totalShippingCost)
                .subtract(totalDiscount)
                .max(BigDecimal.ZERO);

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemResponses)
                .couponResponse(toCouponResponse(cart.getCoupon()))
                .subtotalPrice(subtotalPrice)
                .totalShippingCost(totalShippingCost)
                .totalDiscount(totalDiscount)
                .totalAmount(totalAmount)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            log.debug("toItemResponse: CartItem is null");
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct() != null ? cartItem.getProduct().getId() : null)
                .quantity(cartItem.getQuantity())
                .build();
    }

    public CouponResponse toCouponResponse(Coupon coupon) {
        if (coupon == null) {
            log.debug("toCouponResponse: Coupon is null");
            return null;
        }

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .state(coupon.getState())
                .category(coupon.getCategory())
                .couponScope(coupon.getCouponScope())
                .couponType(coupon.getCouponType())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maxUses(coupon.getMaxUses())
                .maxUsesPerUser(coupon.getMaxUsesPerUser())
                .authorId(coupon.getAuthor() != null ? coupon.getAuthor().getId() : null)
                .startFrom(coupon.getStartFrom())
                .endAt(coupon.getEndAt())
                .discountPercentage(coupon.getDiscountPercentage())
                .discountFixedAmount(coupon.getDiscountFixedAmount())
                .couponTrackings(coupon.getCouponTrackings() != null ? coupon.getCouponTrackings() : Collections.emptySet())
                .build();
    }

    public BigDecimal subtotalPriceOf(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }
        return cart.getItems().stream()
                .filter(item -> item != null && item.getProduct() != null)
                .map(item -> {
                    BigDecimal price = item.getProduct().getOfferPrice() != null
                            ? item.getProduct().getOfferPrice()
                            : item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalShippingCostOf(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }
        return cart.getItems().stream()
                .filter(item -> item != null && item.getProduct() != null && item.getProduct().getShippingCost() != null)
                .map(item -> {
                    BigDecimal baseShippingCost = item.getProduct().getShippingCost();
                    BigDecimal additionalShippingCost = item.getProduct().getEachAdditionalItemShippingCost();
                    int quantity = item.getQuantity();

                    if (quantity <= 1) {
                        return baseShippingCost;
                    } else {
                        BigDecimal additionalItemsCost = additionalShippingCost != null
                                ? additionalShippingCost.multiply(BigDecimal.valueOf(quantity - 1))
                                : baseShippingCost.multiply(BigDecimal.valueOf(quantity - 1));

                        return baseShippingCost.add(additionalItemsCost);
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponse emptyResponse() {
        return CartResponse.builder()
                .id(null).sessionId(null).userId(null)
                .items(Collections.emptyList())
                .couponResponse(null)
                .subtotalPrice(BigDecimal.ZERO)
                .totalShippingCost(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
    }
}


/*package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        return toResponse(cart, null);
    }

    public CartResponse toResponse(Cart cart, Pageable pageable) {
        if (cart == null) {
            log.debug("toResponse: Cart is null, returning empty response");
            return CartResponse.builder()
                    .id(null)
                    .sessionId(null)
                    .userId(null)
                    .items(Collections.emptyList())
                    .couponResponse(null)
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        List<CartItemResponse> itemResponses = cart.getItems() != null
                ? cart.getItems().stream()
                .filter(item -> item != null)
                .map(this::toItemResponse)
                .filter(item -> item != null)
                .collect(Collectors.toList())
                : Collections.emptyList();

        if (pageable != null && !itemResponses.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), itemResponses.size());
            if (start < itemResponses.size()) {
                itemResponses = itemResponses.subList(start, end);
            } else {
                itemResponses = Collections.emptyList();
            }
        }

        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO;
        BigDecimal totalAmount = cart.getTotalPrice() != null ? cart.getTotalPrice() : subtotal.subtract(discount).max(BigDecimal.ZERO);

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(itemResponses)
                .couponResponse(cart.getCoupon())
                .subtotal(subtotal)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            log.debug("toItemResponse: CartItem is null");
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct() != null ? cartItem.getProduct().getId() : null)
                .productName(cartItem.getProductName())
                .price(cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO)
                .quantity(cartItem.getQuantity())
                .build();
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }
        return cart.getItems().stream()
                .filter(item -> item != null && item.getPrice() != null)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}*/

/*import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        return toResponse(cart, null);
    }

    public CartResponse toResponse(Cart cart, Pageable pageable) {
        if (cart == null) {
            log.warn("toResponse: Cart is null");
            return null;
        }

        if (!Hibernate.isInitialized(cart.getItems())) {
            log.warn("toResponse: Cart items not initialized for cart ID: {}", cart.getId());
            return CartResponse.builder()
                    .id(cart.getId())
                    .sessionId(cart.getSessionId() != null ? cart.getSessionId() : null)
                    .user(cart.getUser() != null ? cart.getUser() : null)
                    .items(List.of())
                    .coupon(cart.getCoupon() != null ? cart.getCoupon() : null)
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO)
                    .totalAmount(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO)
                    .build();
        }

        List<CartItemResponse> itemResponses;
        if (pageable != null) {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), cart.getItems().size());
            itemResponses = cart.getItems().subList(start, end).stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        } else {
            itemResponses = cart.getItems().stream()
                    .map(this::toItemResponse)
                    .collect(Collectors.toList());
        }

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId() != null ? cart.getSessionId() : null)
                .user(cart.getUser() != null ? cart.getUser() : null)
                .items(itemResponses)
                .coupon(cart.getCoupon() != null ? cart.getCoupon() : null)
                .subtotal(cart.getTotalPrice() != null ? cart.getTotalPrice().add(cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO) : BigDecimal.ZERO)
                .discountAmount(cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO)
                .totalAmount(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            log.warn("toItemResponse: CartItem is null");
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct() != null ? cartItem.getProduct().getId() : null)
                .productName(cartItem.getProductName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .build();
    }
}*/

/*
package com.datasaz.ecommerce.mappers;


import com.datasaz.ecommerce.models.response.CartItemResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CartMapper {

//    public CartResponse mapToCartResponse(Cart cart) {
//        log.info("mapToCartResponse: convert cart to cart response");
//        CartResponse cartResponse = CartResponse.builder()
//                .id(cart.getId())
//                .quantity(cart.getQuantity())
//                .totalPrice(cart.getTotalPrice())
//                .discount(cart.getDiscount())
//                .product(cart.getProduct())
//                .users(cart.getUsers())
//                .build();
//        return cartResponse;
//    }

//    public CartResponse toResponse(Cart cart) {
//        if (cart == null) {
//            return null;
//        }
//
//        CartResponse.CartResponseBuilder builder = CartResponse.builder()
//                .id(cart.getId())
//                .sessionId(cart.getSessionId())
//                .userId(cart.getUsers() != null ? cart.getUsers().getId() : null);
//
//        List<CartItemResponse> itemResponses = cart.getItems() != null
//                ? cart.getItems().stream()
//                .map(this::toItemResponse)
//                .collect(Collectors.toList())
//                : List.of();
//
//        builder.items(itemResponses);
//        return builder.build();
//    }

    public CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(cart.getItems() != null
                        ? cart.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList())
                        : List.of())
                .couponIdentifier(cart.getCoupon() != null ? cart.getCoupon().getCode() : null)
                .subtotal(subtotal != null ? subtotal : BigDecimal.ZERO)
                .discountAmount(cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO) // Updated in service
                .totalAmount(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO) // Updated in service
                .build();
    }

    public CartItemResponse toItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct() != null ? cartItem.getProduct().getId() : null)
                .productName(cartItem.getProduct() != null ? cartItem.getProduct().getName() : null)
                .price(cartItem.getProduct() != null ? cartItem.getProduct().getPrice() : null)
                .quantity(cartItem.getQuantity())
                .build();
    }
}
*/
