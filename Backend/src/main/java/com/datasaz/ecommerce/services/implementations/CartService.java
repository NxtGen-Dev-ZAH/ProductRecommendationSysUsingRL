package com.datasaz.ecommerce.services.implementations;


import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CartService extends AbstractCartService implements ICartService {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
                       CartMapper cartMapper, ICouponService couponService) {
        super(cartRepository, productRepository, cartMapper, couponService);
    }

    @Override
    protected void validateIdentifier(String sessionId) {
        if (sessionId == null || !UUID_PATTERN.matcher(sessionId).matches()) {
            log.error("Invalid sessionId format: {}", sessionId);
            throw BadRequestException.builder().message("Invalid session ID format").build();
        }
    }

    @Override
    protected Cart getOrCreateCart(String sessionId) {
        log.info("Getting or creating cart for sessionId: {}", sessionId);
        return cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
                .orElseGet(() -> Cart.builder()
                            .sessionId(sessionId)
                        .subtotalPrice(BigDecimal.ZERO)
                        .totalShippingCost(BigDecimal.ZERO)
                        .totalDiscount(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.ZERO)
                            .items(new ArrayList<>())
                        .build());
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupStaleCarts() {
        log.info("Cleaning up stale carts older than 90 days");
        cartRepository.deleteBySessionIdNotNullAndUserIdNullAndLastModifiedBefore(
                LocalDateTime.now().minusDays(90)
        );
    }
}

/*
import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService implements ICartService {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final ICouponService couponService;

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse addToCart(String sessionId, CartRequest cartRequest) {
        log.info("Adding item to cart for sessionId: {}", sessionId);
        Cart cart = getOrCreateCart(sessionId);
        return addItemToCart(cart, cartRequest);
    }

    private CartResponse addItemToCart(Cart cart, CartRequest cartRequest) {
        if (cartRequest.getProductId() == null || cartRequest.getQuantity() <= 0) {
            log.error("Invalid cart request: productId: {}, quantity: {}", cartRequest.getProductId(), cartRequest.getQuantity());
            throw BadRequestException.builder().message("Invalid product ID or quantity").build();
        }

        Product product = productRepository.findById(cartRequest.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {}", cartRequest.getProductId());
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartRequest.getProductId()).build();
                });

        int requestedQuantity = cartRequest.getQuantity();
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(cartRequest.getProductId()))
                .findFirst();

        int totalQuantity = existingItem.map(item -> item.getQuantity() + requestedQuantity)
                .orElse(requestedQuantity);

        if (totalQuantity > product.getQuantity()) {
            log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
            throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
        }

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(totalQuantity);
            existingItem.get().setPrice(product.getPrice());
            existingItem.get().setProductName(product.getName());
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(requestedQuantity)
                    .build();
            cart.getItems().add(cartItem);
        }

        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }


//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse addToCart(String sessionId, CartRequest cartRequest) {
//        log.info("Adding item to cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        Cart cart = getOrCreateCart(sessionId);
//        Product product = productRepository.findById(cartRequest.getProductId())
//                .orElseThrow(() -> {
//                    log.error("Product not found for product id: {}, sessionId: {}", cartRequest.getProductId(), sessionId);
//                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartRequest.getProductId()).build();
//                });
//
//        int requestedQuantity = cartRequest.getQuantity();
//        Optional<CartItem> existingItem = cart.getItems().stream()
//                .filter(item -> item.getProduct().getId().equals(cartRequest.getProductId()))
//                .findFirst();
//
//        int totalQuantity = existingItem.map(item -> item.getQuantity() + requestedQuantity)
//                .orElse(requestedQuantity);
//
//        if (totalQuantity > product.getQuantity()) {
//            log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
//            throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
//        }
//
//        if (existingItem.isPresent()) {
//            existingItem.get().setQuantity(totalQuantity);
//            existingItem.get().setPrice(product.getPrice());
//            existingItem.get().setProductName(product.getName());
//        } else {
//            CartItem cartItem = CartItem.builder()
//                    .cart(cart)
//                    .product(product)
//                    .productName(product.getName())
//                    .price(product.getPrice())
//                    .quantity(requestedQuantity)
//                    .build();
//            cart.getItems().add(cartItem);
//        }
//
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse updateCartItem(String sessionId, Long cartItemId, int quantity) {
        log.info("Updating cart item {} with quantity {} for sessionId: {}", cartItemId, quantity, sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        return updateCartItemInternal(cart, cartItemId, quantity);
    }

    private CartResponse updateCartItemInternal(Cart cart, Long cartItemId, int quantity) {
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for item id: {}, cart id: {}", cartItemId, cart.getId());
                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
                });

        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {}", cartItem.getProduct().getId());
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartItem.getProduct().getId()).build();
                });

        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
        } else {
            if (quantity > product.getQuantity()) {
                log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), quantity, product.getQuantity());
                throw new InsufficientStockException(product.getId(), quantity, product.getQuantity());
            }
            cartItem.setQuantity(quantity);
            cartItem.setPrice(product.getPrice());
            cartItem.setProductName(product.getName());
        }

        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse updateCartItem(String sessionId, Long cartItemId, int quantity) {
//        log.info("Updating cart item {} with quantity {} for sessionId: {}", cartItemId, quantity, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//
//        CartItem cartItem = cart.getItems().stream()
//                .filter(item -> item.getId().equals(cartItemId))
//                .findFirst()
//                .orElseThrow(() -> {
//                    log.error("Cart item not found for item id: {}, cart id: {}, sessionId: {}", cartItemId, cart.getId(), sessionId);
//                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
//                });
//
//        Product product = productRepository.findById(cartItem.getProduct().getId())
//                .orElseThrow(() -> {
//                    log.error("Product not found for product id: {}, sessionId: {}", cartItem.getProduct().getId(), sessionId);
//                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartItem.getProduct().getId()).build();
//                });
//
//        if (quantity <= 0) {
//            cart.getItems().remove(cartItem);
//        } else {
//            if (quantity > product.getQuantity()) {
//                log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), quantity, product.getQuantity());
//                throw new InsufficientStockException(product.getId(), quantity, product.getQuantity());
//            }
//            cartItem.setQuantity(quantity);
//            cartItem.setPrice(product.getPrice());
//            cartItem.setProductName(product.getName());
//        }
//
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeFromCart(String sessionId, Long cartItemId) {
        log.info("Removing cart item {} for sessionId: {}", cartItemId, sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        return removeItemFromCartInternal(cart, cartItemId);
    }

    private CartResponse removeItemFromCartInternal(Cart cart, Long cartItemId) {
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for cart id: {}, item id: {}", cart.getId(), cartItemId);
                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
                });

        cart.getItems().remove(cartItem);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse removeFromCart(String sessionId, Long cartItemId) {
//        log.info("Removing cart item {} for sessionId: {}", cartItemId, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//
//        CartItem cartItem = cart.getItems().stream()
//                .filter(item -> item.getId().equals(cartItemId))
//                .findFirst()
//                .orElseThrow(() -> {
//                    log.error("Cart item not found for cart id: {}, item id: {}, sessionId: {}", cart.getId(), cartItemId, sessionId);
//                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
//                });
//
//        cart.getItems().remove(cartItem);
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }

//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse mergeCartOnLogin(String sessionId, Long userId) {
//        log.info("Merging cart for sessionId: {}, userId: {}", sessionId, userId);
//        validateSessionId(sessionId);
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.error("User not found for user id: {}, sessionId: {}", userId, sessionId);
//                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
//                });
//
//        Cart userCart = cartRepository.findByUserWithItems(user)
//                .orElseGet(() -> {
//                    Cart newCart = Cart.builder()
//                            .user(user)
//                            .sessionId(null)
//                            .totalPrice(BigDecimal.ZERO)
//                            .discount(BigDecimal.ZERO)
//                            .items(new ArrayList<>())
//                            .build();
//                    return cartRepository.save(newCart);
//                });
//
//        Optional<Cart> anonymousCartOpt = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId);
//        if (anonymousCartOpt.isPresent()) {
//            Cart anonymousCart = anonymousCartOpt.get();
//            for (CartItem anonymousItem : anonymousCart.getItems()) {
//                try {
//                    Product product = productRepository.findById(anonymousItem.getProduct().getId())
//                            .orElseThrow(() -> {
//                                log.error("Product not found for product id: {}, sessionId: {}", anonymousItem.getProduct().getId(), sessionId);
//                                return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + anonymousItem.getProduct().getId()).build();
//                            });
//
//                    int totalQuantity = anonymousItem.getQuantity();
//                    Optional<CartItem> existingItem = userCart.getItems().stream()
//                            .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
//                            .findFirst();
//
//                    if (existingItem.isPresent()) {
//                        totalQuantity += existingItem.get().getQuantity();
//                        if (totalQuantity > product.getQuantity()) {
//                            log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
//                            continue;
//                        }
//                        existingItem.get().setQuantity(totalQuantity);
//                        existingItem.get().setPrice(product.getPrice());
//                        existingItem.get().setProductName(product.getName());
//                    } else {
//                        if (totalQuantity > product.getQuantity()) {
//                            log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
//                            continue;
//                        }
//                        CartItem newItem = CartItem.builder()
//                                .cart(userCart)
//                                .product(anonymousItem.getProduct())
//                                .productName(product.getName())
//                                .price(product.getPrice())
//                                .quantity(anonymousItem.getQuantity())
//                                .build();
//                        userCart.getItems().add(newItem);
//                    }
//                } catch (ProductNotFoundException e) {
//                    log.warn("Skipping product id {} due to not found: {}", anonymousItem.getProduct().getId(), e.getMessage());
//                    continue;
//                }
//            }
//            if (anonymousCart.getCoupon() != null) {
//                try {
//                    BigDecimal subtotal = calculateSubtotal(userCart);
//                    couponService.validateCoupon(anonymousCart.getCoupon().getCode(), userCart.getUser(), subtotal, userCart.getItems());
//                    userCart.setCoupon(anonymousCart.getCoupon());
//                } catch (Exception e) {
//                    log.warn("Failed to apply coupon {} from anonymous cart: {}", anonymousCart.getCoupon().getCode(), e.getMessage());
//                }
//            }
//            cartRepository.delete(anonymousCart);
//        }
//
//        updateCartTotals(userCart);
//        cartRepository.save(userCart);
//        return cartMapper.toResponse(userCart);
//    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#sessionId")
    public CartResponse getCart(String sessionId, Pageable pageable) {
        log.info("Retrieving cart for sessionId: {}, pageable: {}", sessionId, pageable);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        return cartMapper.toResponse(cart, pageable);
    }

//    @Override
//    @Transactional(readOnly = true)
//    @Cacheable(value = "carts", key = "'user-' + #userId")
//    public CartResponse getCartByUsersId(Long userId) {
//        log.info("Retrieving cart for userId: {}", userId);
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.error("User not found for userId: {}", userId);
//                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
//                });
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", userId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
//                });
//        return cartMapper.toResponse(cart);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse clearCart(String sessionId) {
        log.info("Clearing cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        cart.getItems().clear();
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public AppliedCouponResponse applyCoupon(String sessionId, String couponIdentifier) {
        log.info("Applying coupon {} to cart for sessionId: {}", couponIdentifier, sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        return applyCouponInternal(cart, couponIdentifier);
    }

    private AppliedCouponResponse applyCouponInternal(Cart cart, String couponIdentifier) {
        BigDecimal subtotal = calculateSubtotal(cart);
        User user = cart.getUser();
        Coupon coupon = couponService.validateCoupon(couponIdentifier, user, subtotal, cart.getItems());
        cart.setCoupon(coupon);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return AppliedCouponResponse.builder()
                .discount(cart.getDiscount())
                .cartResponse(cartMapper.toResponse(cart))
                .build();
    }

//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public AppliedCouponResponse applyCoupon(String sessionId, String couponIdentifier) {
//        log.info("Applying coupon {} to cart for sessionId: {}", couponIdentifier, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//
//        BigDecimal subtotal = calculateSubtotal(cart);
//        User user = cart.getUser();
//        Coupon coupon = couponService.validateCoupon(couponIdentifier, user, subtotal, cart.getItems());
//        cart.setCoupon(coupon);
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return AppliedCouponResponse.builder()
//                .discount(cart.getDiscount())
//                .cartResponse(cartMapper.toResponse(cart))
//                .build();
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeCoupon(String sessionId) {
        log.info("Removing coupon from cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        cart.setCoupon(null);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmount(String sessionId) {
        log.info("Calculating total amount for cart sessionId: {}", sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });

        return cart.getTotalPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String sessionId) {
        log.info("Calculating discount for sessionId: {}", sessionId);
        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });

        return cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO;
    }

    private Cart getOrCreateCart(String sessionId) {
        log.info("Getting or creating cart for sessionId: {}", sessionId);
        return cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .sessionId(sessionId)
                            .totalPrice(BigDecimal.ZERO)
                            .discount(BigDecimal.ZERO)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getCoupon() != null) {
            discount = couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal);
        }
        cart.setDiscount(discount);
        cart.setTotalPrice(subtotal.subtract(discount).max(BigDecimal.ZERO));
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || !UUID_PATTERN.matcher(sessionId).matches()) {
            log.error("Invalid sessionId format: {}", sessionId);
            throw BadRequestException.builder().message("Invalid session ID format").build();
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupStaleCarts() {
        log.info("Cleaning up stale carts older than 90 days");
        cartRepository.deleteBySessionIdNotNullAndLastModifiedBefore(
                LocalDateTime.now().minusDays(90)
        );
    }
}*/

/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService implements ICartService {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final ICouponService couponService;

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse addToCart(String sessionId, CartRequest cartRequest) {
        log.info("Adding item to cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
        if (cartRequest.getProductId() == null || cartRequest.getQuantity() <= 0) {
            log.error("Invalid cart request: productId: {}, quantity: {}", cartRequest.getProductId(), cartRequest.getQuantity());
            throw BadRequestException.builder().message("Invalid product ID or quantity").build();
        }

        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(cartRequest.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {}, sessionId: {}", cartRequest.getProductId(), sessionId);
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartRequest.getProductId()).build();
                });

        int requestedQuantity = cartRequest.getQuantity();
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(cartRequest.getProductId()))
                .findFirst();

        int totalQuantity = existingItem.map(item -> item.getQuantity() + requestedQuantity)
                .orElse(requestedQuantity);

        if (totalQuantity > product.getQuantity()) {
            log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
            throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
        }

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(totalQuantity);
            existingItem.get().setPrice(product.getPrice());
            existingItem.get().setProductName(product.getName());
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .productName(product.getName())
                    .price(product.getPrice())
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
    public CartResponse updateCart(String sessionId, Long cartItemId, int quantity) {
        log.info("Updating cart item {} with quantity {} for sessionId: {}", cartItemId, quantity, sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for item id: {}, cart id: {}, sessionId: {}", cartItemId, cart.getId(), sessionId);
                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
                });

        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {}, sessionId: {}", cartItem.getProduct().getId(), sessionId);
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartItem.getProduct().getId()).build();
                });

        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
        } else {
            if (quantity > product.getQuantity()) {
                log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), quantity, product.getQuantity());
                throw new InsufficientStockException(product.getId(), quantity, product.getQuantity());
            }
            cartItem.setQuantity(quantity);
            cartItem.setPrice(product.getPrice());
            cartItem.setProductName(product.getName());
        }

        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeFromCart(String sessionId, Long cartItemId) {
        log.info("Removing cart item {} for sessionId: {}", cartItemId, sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for cart id: {}, item id: {}, sessionId: {}", cart.getId(), cartItemId, sessionId);
                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
                });

        cart.getItems().remove(cartItem);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse mergeCartOnLogin(String sessionId, Long userId) {
        log.info("Merging cart for sessionId: {}, userId: {}", sessionId, userId);
        validateSessionId(sessionId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for user id: {}, sessionId: {}", userId, sessionId);
                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
                });

        Cart anonymousCart = cartRepository.findBySessionId(sessionId)
                .orElse(null);
        Cart userCart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .sessionId(null)
                            .totalPrice(BigDecimal.ZERO)
                            .discount(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(newCart);
                });

        if (anonymousCart != null) {
            for (CartItem anonymousItem : anonymousCart.getItems()) {
                Product product = productRepository.findById(anonymousItem.getProduct().getId())
                        .orElseThrow(() -> {
                            log.error("Product not found for product id: {}, sessionId: {}", anonymousItem.getProduct().getId(), sessionId);
                            return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + anonymousItem.getProduct().getId()).build();
                        });

                int totalQuantity = anonymousItem.getQuantity();
                CartItem existingItem = userCart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
                        .findFirst()
                        .orElse(null);

                if (existingItem != null) {
                    totalQuantity += existingItem.getQuantity();
                    if (totalQuantity > product.getQuantity()) {
                        log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
                        throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
                    }
                    existingItem.setQuantity(totalQuantity);
                    existingItem.setPrice(product.getPrice());
                    existingItem.setProductName(product.getName());
                } else {
                    if (totalQuantity > product.getQuantity()) {
                        log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
                        throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
                    }
                    CartItem newItem = CartItem.builder()
                            .cart(userCart)
                            .product(anonymousItem.getProduct())
                            .productName(product.getName())
                            .price(product.getPrice())
                            .quantity(anonymousItem.getQuantity())
                            .build();
                    userCart.getItems().add(newItem);
                }
            }
            cartRepository.delete(anonymousCart);
        }

        updateCartTotals(userCart);
        cartRepository.save(userCart);
        return cartMapper.toResponse(userCart);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "#sessionId")
    public CartResponse getCart(String sessionId, Pageable pageable) {
        log.info("Retrieving cart for sessionId: {}, pageable: {}", sessionId, pageable);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        return cartMapper.toResponse(cart, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "'user-' + #userId")
    public CartResponse getCartByUsersId(Long userId) {
        log.info("Retrieving cart for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", userId);
                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
                });
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", userId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + userId).build();
                });
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public void clearCart(String sessionId) {
        log.info("Clearing cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        cart.getItems().clear();
        updateCartTotals(cart);
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public AppliedCouponResponse applyCoupon(String sessionId, String couponIdentifier) {
        log.info("Applying coupon {} to cart for sessionId: {}", couponIdentifier, sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });

        BigDecimal subtotal = calculateSubtotal(cart);
        User user = cart.getUser();
        Coupon coupon = couponService.validateCoupon(couponIdentifier, user, subtotal, cart.getItems());
        cart.setCoupon(coupon);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return AppliedCouponResponse.builder()
                .discount(cart.getDiscount())
                .cartResponse(cartMapper.toResponse(cart))
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeCoupon(String sessionId) {
        log.info("Removing coupon from cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        cart.setCoupon(null);
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmount(Long userId) {
        log.info("Calculating total amount for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", userId);
                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
                });

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", userId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
                });

        return cart.getTotalPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Long userId) {
        log.info("Calculating discount for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", userId);
                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
                });

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", userId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
                });

        return cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO;
    }

    private Cart getOrCreateCart(String sessionId) {
        log.info("Getting or creating cart for sessionId: {}", sessionId);
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .sessionId(sessionId)
                            .totalPrice(BigDecimal.ZERO)
                            .discount(BigDecimal.ZERO)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getCoupon() != null) {
            discount = couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal);
        }
        cart.setDiscount(discount);
        cart.setTotalPrice(subtotal.subtract(discount).max(BigDecimal.ZERO));
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || !UUID_PATTERN.matcher(sessionId).matches()) {
            log.error("Invalid sessionId format: {}", sessionId);
            throw BadRequestException.builder().message("Invalid session ID format").build();
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupStaleCarts() {
        log.info("Cleaning up stale carts older than 90 days");
        cartRepository.deleteBySessionIdNotNullAndLastModifiedBefore(
                LocalDateTime.now().minusDays(90)
        );
    }
}*/

/*
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.request.CartRequest;
import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService implements ICartService {

    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CartMapper cartMapper;
    private final ICouponService couponService;

    @Override
    @Transactional
    public CartResponse addToCart(String sessionId, CartRequest cartRequest) {
        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(cartRequest.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {} and sessionId: {}", cartRequest.getProductId(), sessionId);
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found : ").build();
                });

        int requestedQuantity = cartRequest.getQuantity();
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(cartRequest.getProductId()))
                .findFirst();

        int totalQuantity = existingItem.map(item -> item.getQuantity() + requestedQuantity)
                .orElse(requestedQuantity);

        if (totalQuantity > product.getQuantity()) {
            if (product.getQuantity() > 0) {
                existingItem.get().setQuantity(product.getQuantity());
            }
            throw new InsufficientStockException(
                    product.getId(), totalQuantity, product.getQuantity());
        }

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(totalQuantity);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setProductName(product.getName());
            cartItem.setPrice(product.getPrice());
            if (requestedQuantity > product.getQuantity()) {
//                if (product.getQuantity() > 0) {
//                    cartItem.setQuantity(product.getQuantity()); // set to maximum available quantity
//                }
                throw new InsufficientStockException(
                        product.getId(), requestedQuantity, product.getQuantity());
            } else
                cartItem.setQuantity(requestedQuantity);

            cart.getItems().add(cartItem);
        }

        // TODO: cart.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity))); //verify and correct for all cartItems

        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCart(String sessionId, Long cartItemId, int quantity) {
        log.info("Update the cart item in the cart with sessionId : {}", sessionId);

        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
                });
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart Item not found for item id: {} cart id: {} and sessionId: {}", cartItemId, cart.getId(), sessionId);
                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
                });
        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {} and sessionId: {}", cartItem.getProduct().getId(), sessionId);
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: ").build();
                });


        if (quantity <= 0)
            cart.getItems().remove(cartItem);
        else {
            if (quantity > product.getQuantity())
                if (product.getQuantity() > 0) {
                    // Set to maximum available quantity
                    cartItem.setQuantity(product.getQuantity());
                    log.error("Quantity {} is greater than the available quantity {} for product id: {} and sessionId: {}", quantity, product.getQuantity(), cartItem.getProduct().getId(), sessionId);
                    throw new InsufficientStockException(
                            product.getId(), quantity, product.getQuantity());
                } else
                    cartItem.setQuantity(quantity);
        }

        // TODO: cart.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity))); //verify and correct for all cartItems

        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }


    @Override
    @Transactional
    public CartResponse removeFromCart(String sessionId, Long cartItemId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
                });
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Cart item not found for cart id:{}, item id: {} and sessionId: {}", cart.getId(), cartItemId, sessionId);
                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
                });
        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("Product not found for product id: {} and sessionId: {}", cartItem.getProduct().getId(), sessionId);
                    return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + cartItem.getProduct().getId()).build();
                });

        cart.getItems().remove(cartItem);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse mergeCartOnLogin(String sessionId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for user id: {} and sessionId: {}", userId, sessionId);
                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "Login user not found.").build();
                });

        Cart anonymousCart = cartRepository.findBySessionId(sessionId)
                .orElse(null);
        Cart userCart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setSessionId(null);
                    return newCart;
                });

        if (anonymousCart != null) {
            for (CartItem anonymousItem : anonymousCart.getItems()) {
                CartItem existingItem = userCart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
                        .findFirst()
                        .orElse(null);
                if (existingItem != null) {
                    existingItem.setQuantity(existingItem.getQuantity() + anonymousItem.getQuantity());
                } else {
                    CartItem newItem = new CartItem();
                    newItem.setCart(userCart);
                    newItem.setProduct(anonymousItem.getProduct());
                    newItem.setQuantity(anonymousItem.getQuantity());
                    userCart.getItems().add(newItem);
                }
            }
            cartRepository.delete(anonymousCart);
        }

        cartRepository.save(userCart);
        return cartMapper.toResponse(userCart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String sessionId) {
        log.info("Retreive Cart for the client session: {}", sessionId);
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found of sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
                });

        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse getCartByUsersId(Long userId) {
        log.info("Retreive Cart for the client id: {}", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Cart not found of userId: {}", userId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
                });

        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse getCartByUsers(User user) {
        log.info("Retreive Cart for the client ID: {}", user.getId());
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found of userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
                });

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String sessionId) {
        log.info("Empty the cart for the session ID : {}", sessionId);

        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
                });

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
    }

    @Override
    @Transactional
    public AppliedCouponResponse applyCoupon(String sessionId, String couponIdentifier) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message("Cart not found").build();
                });

        BigDecimal subtotal = calculateSubtotal(cart);
        User user = cart.getUser();
        Coupon coupon = couponService.validateCoupon(couponIdentifier, user, subtotal, cart.getItems());
        cart.setCoupon(coupon);
        BigDecimal discount = couponService.calculateDiscount(coupon, cart.getItems(), subtotal);
        cartRepository.save(cart);
        return AppliedCouponResponse.builder()
                .discount(discount)
                .cartResponse(cartMapper.toResponse(cart)).build();
    }


//    @Override
//    @Transactional
//    public CartResponse applyCouponV1(String sessionId, String couponIdentifier) {
//        Cart cart = cartRepository.findBySessionId(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
//                });
//
//        BigDecimal subtotal = calculateSubtotal(cart);
//        User user = cart.getUser();
//        Coupon coupon = couponService.validateCoupon(
//                couponIdentifier, user, subtotal, cart.getItems());
//        cart.setCoupon(coupon);
//
//        cartRepository.save(cart);
//        return buildCartResponse(cart);
//    }

    @Override
    @Transactional
    public CartResponse removeCoupon(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.error("Cart not found for sessionId: {}", sessionId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found.").build();
                });
        cart.setCoupon(null);
        cartRepository.save(cart);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for user id: {}", userId);
                    return UserNotFoundException.builder().message("User not found").build();
                });

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for user id: {}", userId);
                    return CartNotFoundException.builder().message("Cart not found").build();
                });

        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = calculateDiscount(userId);
        return subtotal.subtract(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for user id: {}", userId);
                    return UserNotFoundException.builder().message("User not found").build();
                });

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for user id: {}", userId);
                    return CartNotFoundException.builder().message("Cart not found").build();
                });

        if (cart.getCoupon() == null) {
            return BigDecimal.ZERO;
        }

        return couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), calculateSubtotal(cart));
    }

    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponse buildCartResponse(Cart cart) {
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponIdentifier = null;

        if (cart.getCoupon() != null) {
            discountAmount = couponService.calculateDiscount(
                    cart.getCoupon(), cart.getItems(), subtotal);
            couponIdentifier = cart.getCoupon().getCode();
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount)
                .max(BigDecimal.ZERO);

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(cart.getItems().stream()
                        .map(cartMapper::toItemResponse)
                        .collect(Collectors.toList()))
                .couponIdentifier(couponIdentifier)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .build();
    }
}
*/
