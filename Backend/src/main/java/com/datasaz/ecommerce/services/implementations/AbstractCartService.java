package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CartItemNotFoundException;
import com.datasaz.ecommerce.exceptions.InsufficientStockException;
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartItemRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCartService implements ICartService {

    protected final CartRepository cartRepository;
    protected final ProductRepository productRepository;
    protected final CartMapper cartMapper;
    protected final ICouponService couponService;

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse addToCart(String identifier, CartItemRequest cartItemRequest) {
        log.info("Adding item to cart for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);
        return addItemToCart(cart, cartItemRequest);
    }

    @Transactional
    protected CartResponse addItemToCart(Cart cart, CartItemRequest cartItemRequest) {
        if (cartItemRequest.getProductId() == null || cartItemRequest.getQuantity() <= 0) {
            log.error("Invalid cart request: productId: {}, quantity: {}", cartItemRequest.getProductId(), cartItemRequest.getQuantity());
            throw BadRequestException.builder().message("Invalid product ID or quantity").build();
        }

        Product product = productRepository.findById(cartItemRequest.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {}", cartItemRequest.getProductId());
                    return ProductNotFoundException.builder()
                            .message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartItemRequest.getProductId())
                            .build();
                });

        int requestedQuantity = cartItemRequest.getQuantity();
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(cartItemRequest.getProductId()))
                .findFirst();

        int totalQuantity = existingItem.map(item -> item.getQuantity() + requestedQuantity)
                .orElse(requestedQuantity);

        if (totalQuantity > product.getQuantity()) {
            log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
            throw InsufficientStockException.builder().message(product.getId() + "-" + totalQuantity + "-" + product.getQuantity()).build();
        }

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(totalQuantity);
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(requestedQuantity)
                    .build();
            cart.getItems().add(cartItem);
        }

        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse updateCartItem(String identifier, Long cartItemId, int quantity) {
        log.info("Updating cart item {} with quantity {} for identifier: {}", cartItemId, quantity, identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);
        return updateCartItemInternal(cart, cartItemId, quantity);
    }

    protected CartResponse updateCartItemInternal(Cart cart, Long cartItemId, int quantity) {
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for item id: {}, cart id: {}", cartItemId, cart.getId());
                    return CartItemNotFoundException.builder()
                            .message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId)
                            .build();
                });

        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {}", cartItem.getProduct().getId());
                    return ProductNotFoundException.builder()
                            .message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartItem.getProduct().getId())
                            .build();
                });

        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
        } else {
            if (quantity > product.getQuantity()) {
                log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), quantity, product.getQuantity());
                throw InsufficientStockException.builder().message(product.getId() + "-" + quantity + "-" + product.getQuantity()).build();

            }
            cartItem.setQuantity(quantity);
        }

        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeFromCart(String identifier, Long cartItemId) {
        log.info("Removing cart item {} for identifier: {}", cartItemId, identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);
        return removeItemFromCartInternal(cart, cartItemId);
    }

    protected CartResponse removeItemFromCartInternal(Cart cart, Long cartItemId) {
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for cart id: {}, item id: {}", cart.getId(), cartItemId);
                    return CartItemNotFoundException.builder()
                            .message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId)
                            .build();
                });

        cart.getItems().remove(cartItem);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse clearCart(String identifier) {
        log.info("Clearing cart for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);
        cart.getItems().clear();
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public AppliedCouponResponse applyCoupon(String identifier, String couponIdentifier) {
        log.info("Applying coupon {} to cart for identifier: {}", couponIdentifier, identifier);

        return applyCouponInternal(identifier, couponIdentifier);
    }

    protected AppliedCouponResponse applyCouponInternal(String identifier, String couponIdentifier) {

        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);

        BigDecimal subtotal = calculateSubtotalPrice(identifier);
        User user = cart.getUser();
        Coupon coupon = couponService.validateCoupon(couponIdentifier, user, subtotal, cart.getItems());
        cart.setCoupon(coupon);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return AppliedCouponResponse.builder()
                .code(coupon.getCode())
                .discount(cart.getTotalDiscount())
                .cartResponse(cartMapper.toResponse(cart))
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeCoupon(String identifier) {
        log.info("Removing coupon from cart for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);
        cart.setCoupon(null);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String identifier, Pageable pageable) {
        log.info("Retrieving cart for identifier: {}, pageable: {}", identifier, pageable);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);
        return cartMapper.toResponse(cart, pageable);
    }

    public BigDecimal calculateSubtotalPrice(String identifier) {

        log.info("calculateSubtotalPrice for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);

        return subtotalPriceOf(cart);
    }

    protected BigDecimal subtotalPriceOf(Cart cart) {
        return cart.getItems().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getProduct() != null)
                .map(item -> {
                    BigDecimal price = item.getProduct().getOfferPrice() != null
                            ? item.getProduct().getOfferPrice()
                            : item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalShippingCost(String identifier) {

        log.info("calculateTotalShippingCost for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);

        return totalShippingCostOf(cart);
    }

    protected BigDecimal totalShippingCostOf(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            return BigDecimal.ZERO;
        }
        return cart.getItems().stream()
                .filter(Objects::nonNull)
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

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String identifier) {
        log.info("Calculating discount for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);

        BigDecimal subtotal = subtotalPriceOf(cart);
        BigDecimal discount = cart.getCoupon() != null
                ? couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal)
                : BigDecimal.ZERO;

        return discount;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmount(String identifier) {
        log.info("Calculating total amount for identifier: {}", identifier);
        validateIdentifier(identifier);
        Cart cart = getOrCreateCart(identifier);

        BigDecimal subtotal = subtotalPriceOf(cart);
        BigDecimal shippingCost = totalShippingCostOf(cart);

        BigDecimal discount = cart.getCoupon() != null
                ? couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal)
                : BigDecimal.ZERO;

//        cart.setSubtotalPrice(subtotal);
//        cart.setTotalShippingCost(shippingCost);
//        cart.setTotalDiscount(discount);
//        cart.setTotalAmount(totalAmount.subtract(discount).max(BigDecimal.ZERO));

        BigDecimal totalAmount = subtotal.add(shippingCost).subtract(discount).max(BigDecimal.ZERO);

        return totalAmount;
    }

    public void updateCartTotals(Cart cart) {
        if (cart == null || cart.getItems() == null) {
            log.warn("Empty or null Cart");
            return;
        }

        BigDecimal subtotal = subtotalPriceOf(cart);
        BigDecimal shippingCost = totalShippingCostOf(cart);

        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getCoupon() != null) {
            try {
                discount = couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal);
            } catch (Exception e) {
                log.warn("Failed to calculate discount for coupon {}: {}", cart.getCoupon().getCode(), e.getMessage());
                discount = BigDecimal.ZERO;
            }
        }

        BigDecimal totalAmount = subtotal
                .add(shippingCost != null ? shippingCost : BigDecimal.ZERO)
                .subtract(discount != null ? discount : BigDecimal.ZERO)
                .max(BigDecimal.ZERO);

        cart.setSubtotalPrice(subtotal);
        cart.setTotalShippingCost(shippingCost != null ? shippingCost : BigDecimal.ZERO);
        cart.setTotalDiscount(discount != null ? discount : BigDecimal.ZERO);
        cart.setTotalAmount(totalAmount);
    }


//    public void updateCartTotals(Cart cart) {
//        BigDecimal subtotal = calculateSubtotalPrice(cart); // reuse
//        BigDecimal shipping = calculateTotalShippingCost(cart);
//        BigDecimal discount = cart.getCoupon() != null
//                ? couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal)
//                : BigDecimal.ZERO;
//
//        cart.setSubtotalPrice(subtotal);
//        cart.setTotalShippingCost(shipping);
//        cart.setTotalDiscount(discount);
//        cart.setTotalAmount(subtotal.add(shipping).subtract(discount).max(BigDecimal.ZERO));
//    }

//    public void updateCartTotals(Cart cart) {
//        BigDecimal subtotal = cart.getItems().stream()
//                .map(item -> {
//                    BigDecimal price = item.getProduct().getOfferPrice() != null
//                            ? item.getProduct().getOfferPrice()
//                            : item.getProduct().getPrice();
//                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
//                })
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal shippingCost = cart.getItems().stream()
//                .filter(item -> item != null && item.getProduct() != null && item.getProduct().getShippingCost() != null)
//                .map(item -> {
//                    BigDecimal baseShippingCost = item.getProduct().getShippingCost();
//                    BigDecimal additionalShippingCost = item.getProduct().getEachAdditionalItemShippingCost();
//                    int quantity = item.getQuantity();
//
//                    if (quantity <= 1) {
//                        return baseShippingCost;
//                    } else {
//                        BigDecimal additionalItemsCost = additionalShippingCost != null
//                                ? additionalShippingCost.multiply(BigDecimal.valueOf(quantity - 1))
//                                : baseShippingCost.multiply(BigDecimal.valueOf(quantity - 1));
//
//                        return baseShippingCost.add(additionalItemsCost);
//                    }
//                })
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal discount = cart.getCoupon() != null
//                ? couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal)
//                : BigDecimal.ZERO;
//
//        BigDecimal totalAmount = subtotal.add(shippingCost).subtract(discount).max(BigDecimal.ZERO);
//
//        cart.setSubtotalPrice(subtotal);
//        cart.setTotalShippingCost(shippingCost);
//        cart.setTotalDiscount(discount);
//        cart.setTotalAmount(totalAmount);
//    }

    protected abstract void validateIdentifier(String identifier);

    protected abstract Cart getOrCreateCart(String identifier);
}