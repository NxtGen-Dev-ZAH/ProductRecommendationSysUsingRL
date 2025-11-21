package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.CartMapper;
import com.datasaz.ecommerce.models.response.CartResponse;
import com.datasaz.ecommerce.repositories.CartRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.CartItem;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BuyerCartService extends AbstractCartService implements IBuyerCartService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public BuyerCartService(CartRepository cartRepository, ProductRepository productRepository,
                            UserRepository userRepository, CartMapper cartMapper,
                            ICouponService couponService, CurrentUserService currentUserService) {
        super(cartRepository, productRepository, cartMapper, couponService);
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public CartResponse mergeCartOnLogin(String sessionId, Long userId) {
        log.info("Merging cart for sessionId: {}, userId: {}", sessionId, userId);
        if (sessionId == null || !Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matcher(sessionId).matches()) {
            log.error("Invalid sessionId format: {}", sessionId);
            throw BadRequestException.builder().message("Invalid session ID format").build();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", userId);
                    return UserNotFoundException.builder().message("User not found: " + userId).build();
                });

        validateIdentifier(String.valueOf(userId));

        Cart userCart = getOrCreateCart(String.valueOf(user.getId()));
        Optional<Cart> anonymousCartOpt = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId);

        if (anonymousCartOpt.isPresent()) {
            Cart anonymousCart = anonymousCartOpt.get();
            mergeItems(userCart, anonymousCart);
            mergeCoupon(userCart, anonymousCart);
            cartRepository.delete(anonymousCart);
        }

//        if (anonymousCartOpt.isPresent()) {
//            Cart anonymousCart = anonymousCartOpt.get();
//            for (CartItem anonymousItem : anonymousCart.getItems()) {
//                Product product = productRepository.findById(anonymousItem.getProduct().getId())
//                        .orElse(null);
//                if (product == null) {
//                    log.warn("Skipping product id {} due to not found", anonymousItem.getProduct().getId());
//                    continue;
//                }
//
//                int totalQuantity = anonymousItem.getQuantity();
//                Optional<CartItem> existingItem = userCart.getItems().stream()
//                        .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
//                        .findFirst();
//
//                if (existingItem.isPresent()) {
//                    totalQuantity += existingItem.get().getQuantity();
//                    if (totalQuantity > product.getQuantity()) {
//                        log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
//                        continue;
//                    }
//                    existingItem.get().setQuantity(totalQuantity);
//                    existingItem.get().setProduct(product);
//                } else {
//                    if (totalQuantity > product.getQuantity()) {
//                        log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
//                        continue;
//                    }
//                    CartItem newItem = CartItem.builder()
//                            .cart(userCart)
//                            .product(anonymousItem.getProduct())
//                            .quantity(anonymousItem.getQuantity())
//                            .build();
//                    userCart.getItems().add(newItem);
//                }
//            }
//            if (anonymousCart.getCoupon() != null) {
//                try {
//                    BigDecimal subtotal = userCart.getItems().stream()
//                            .map(item -> {
//                                BigDecimal price = item.getProduct().getOfferPrice() != null
//                                        ? item.getProduct().getOfferPrice()
//                                        : item.getProduct().getPrice();
//                                return price.multiply(BigDecimal.valueOf(item.getQuantity()));
//                            })
//                            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//                    couponService.validateCoupon(anonymousCart.getCoupon().getCode(), userCart.getUser(), subtotal, userCart.getItems());
//                    userCart.setCoupon(anonymousCart.getCoupon());
//                } catch (Exception e) {
//                    log.warn("Failed to apply coupon {} from anonymous cart: {}", anonymousCart.getCoupon().getCode(), e.getMessage());
//                }
//            }
//            cartRepository.delete(anonymousCart);
//        }


        updateCartTotals(userCart);
        cartRepository.save(userCart);
        return cartMapper.toResponse(userCart);
    }

    private void mergeItems(Cart userCart, Cart anonymousCart) {
        for (CartItem anonItem : anonymousCart.getItems()) {
            Product product = productRepository.findById(anonItem.getProduct().getId()).orElse(null);
            if (product == null) continue;

            int totalQty = anonItem.getQuantity();
            Optional<CartItem> existing = userCart.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(product.getId()))
                    .findFirst();

            if (existing.isPresent()) {
                totalQty += existing.get().getQuantity();
                if (totalQty > product.getQuantity()) continue;
                existing.get().setQuantity(totalQty);
                existing.get().setProduct(product);
            } else {
                if (totalQty > product.getQuantity()) continue;
                userCart.getItems().add(CartItem.builder()
                        .cart(userCart)
                        .product(product)
                        .quantity(anonItem.getQuantity())
                        .build());
            }
        }
    }

    private void mergeCoupon(Cart userCart, Cart anonymousCart) {
        if (anonymousCart.getCoupon() == null) return;

        try {
            BigDecimal subtotal = subtotalPriceOf(userCart);

            couponService.validateCoupon(anonymousCart.getCoupon().getCode(), userCart.getUser(), subtotal, userCart.getItems());
            userCart.setCoupon(anonymousCart.getCoupon());
        } catch (Exception e) {
            log.warn("Failed to apply coupon from anonymous cart: {}", e.getMessage());
        }
    }

    @Override
    protected void validateIdentifier(String userId) {
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.error("Invalid userId format: {}", userId);
            throw BadRequestException.builder().message("Invalid user ID format").build();
        }
    }

    @Override
    protected Cart getOrCreateCart(String userId) {
        log.info("Getting or creating cart for userId: {}", userId);
        validateIdentifier(userId);
        Long id = Long.parseLong(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", id);
                    return UserNotFoundException.builder().message("User not found: " + id).build();
                });

        return cartRepository.findByUserWithItems(user)
                .orElseGet(() -> Cart.builder()
                        .user(user)
                        .sessionId(null)
                        .subtotalPrice(BigDecimal.ZERO)
                        .totalShippingCost(BigDecimal.ZERO)
                        .totalDiscount(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.ZERO)
                        .items(new ArrayList<>())
                        .build());
    }
//    @Override
//    protected Cart getOrCreateCart(String userId) {
//        log.info("Getting or creating cart for userId: {}", userId);
//        Long id = Long.parseLong(userId);
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> {
//                    log.error("User not found for userId: {}", id);
//                    return UserNotFoundException.builder().message("User not found: " + id).build();
//                });
//        return cartRepository.findByUserWithItems(user)
//                .orElseGet(() -> {
//                    Cart newCart = Cart.builder()
//                            .user(user)
//                            .sessionId(null)
//                            .subtotalPrice(BigDecimal.ZERO)
//                            .totalShippingCost(BigDecimal.ZERO)
//                            .totalDiscount(BigDecimal.ZERO)
//                            .totalAmount(BigDecimal.ZERO)
//                            .items(new ArrayList<>())
//                            .build();
//                    return newCart;
//                    //return cartRepository.save(newCart);
//                });
//    }
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
import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
import com.datasaz.ecommerce.services.interfaces.ICouponService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyerCartService implements IBuyerCartService {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final ICouponService couponService;
    private final CurrentUserService currentUserService;

//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse addToCart(String sessionId, CartRequest cartRequest) {
//        log.info("Adding item to cart for sessionId: {}", sessionId);
//        Cart cart = getOrCreateCart(sessionId);
//        return addItemToCart(cart, cartRequest);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse addToCartForUser(CartRequest cartRequest) {
        log.info("Adding item to cart for authenticated user");
        User user = currentUserService.getCurrentUser();
        Cart cart = getOrCreateUserCart(user);
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
//    public CartResponse updateCartItem(String sessionId, Long cartItemId, int quantity) {
//        log.info("Updating cart item {} with quantity {} for sessionId: {}", cartItemId, quantity, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        return updateCartItemInternal(cart, cartItemId, quantity);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse updateCartItemForUser(Long cartItemId, int quantity) {
        log.info("Updating cart item {} with quantity {} for authenticated user", cartItemId, quantity);
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
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
//    public CartResponse removeFromCart(String sessionId, Long cartItemId) {
//        log.info("Removing cart item {} for sessionId: {}", cartItemId, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        return removeItemFromCartInternal(cart, cartItemId);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeFromCartForUser(Long cartItemId) {
        log.info("Removing cart item {} for authenticated user", cartItemId);
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
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
//    public CartResponse clearCart(String sessionId) {
//        log.info("Clearing cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        cart.getItems().clear();
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse clearCartForUser() {
        log.info("Clearing cart for authenticated user");
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
                });
        cart.getItems().clear();
        updateCartTotals(cart);
        cartRepository.save(cart);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse mergeCartOnLogin(String sessionId) {
        log.info("Merging cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
        User user = currentUserService.getCurrentUser();
        Cart userCart = getOrCreateUserCart(user);

        Optional<Cart> anonymousCartOpt = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId);
        if (anonymousCartOpt.isPresent()) {
            Cart anonymousCart = anonymousCartOpt.get();
            for (CartItem anonymousItem : anonymousCart.getItems()) {
                Product product = productRepository.findById(anonymousItem.getProduct().getId())
                        .orElse(null);
                if (product == null) {
                    log.warn("Skipping product id {} due to not found", anonymousItem.getProduct().getId());
                    continue;
                }

                int totalQuantity = anonymousItem.getQuantity();
                Optional<CartItem> existingItem = userCart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
                        .findFirst();

                if (existingItem.isPresent()) {
                    totalQuantity += existingItem.get().getQuantity();
                    if (totalQuantity > product.getQuantity()) {
                        log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
                        continue;
                    }
                    existingItem.get().setQuantity(totalQuantity);
                    existingItem.get().setPrice(product.getPrice());
                    existingItem.get().setProductName(product.getName());
                } else {
                    if (totalQuantity > product.getQuantity()) {
                        log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
                        continue;
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
            // Copy coupon if present
            if (anonymousCart.getCoupon() != null) {
                BigDecimal subtotal = calculateSubtotal(userCart);
                try {
                    couponService.validateCoupon(anonymousCart.getCoupon().getCode(), userCart.getUser(), subtotal, userCart.getItems());
                    userCart.setCoupon(anonymousCart.getCoupon());
                } catch (Exception e) {
                    log.warn("Failed to apply coupon {} from anonymous cart: {}", anonymousCart.getCoupon().getCode(), e.getMessage());
                }
            }
            // Delete anonymous cart in a separate transaction
            deleteAnonymousCart(anonymousCart);
        }

        updateCartTotals(userCart);
        cartRepository.save(userCart);
        return cartMapper.toResponse(userCart);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAnonymousCart(Cart anonymousCart) {
        log.info("Deleting anonymous cart with id: {}", anonymousCart.getId());
        cartRepository.delete(anonymousCart);
    }

//    @Override
//    @Transactional(readOnly = true)
//    @Cacheable(value = "carts", key = "#sessionId")
//    public CartResponse getCart(String sessionId, Pageable pageable) {
//        log.info("Retrieving cart for sessionId: {}, pageable: {}", sessionId, pageable);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        return cartMapper.toResponse(cart, pageable);
//    }

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
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", userId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
                });
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "carts", key = "'user-authenticated'")
    public CartResponse getCartForUser(Pageable pageable) {
        log.info("Retrieving cart for authenticated user");
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
                });
        return cartMapper.toResponse(cart, pageable);
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
//        return applyCouponInternal(cart, couponIdentifier);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public AppliedCouponResponse applyCouponForUser(String couponIdentifier) {
        log.info("Applying coupon {} to cart for authenticated user", couponIdentifier);
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
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
//    public CartResponse removeCoupon(String sessionId) {
//        log.info("Removing coupon from cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        cart.setCoupon(null);
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }

    @Override
    @Transactional
    @CacheEvict(value = {"carts", "products"}, allEntries = true)
    public CartResponse removeCouponForUser() {
        log.info("Removing coupon from cart for authenticated user");
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", user.getId());
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
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

        Cart cart = cartRepository.findByUserWithItems(user)
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

        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for userId: {}", userId);
                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
                });

        return cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO;
    }

    private Cart getOrCreateCart(String sessionId) {
        log.info("Getting or creating cart for sessionId: {}", sessionId);
        validateSessionId(sessionId);
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

    private Cart getOrCreateUserCart(User user) {
        log.info("Getting or creating cart for userId: {}", user.getId());
        return cartRepository.findByUserWithItems(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .sessionId(null) // User carts don't use sessionId
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
        if (sessionId != null && !UUID_PATTERN.matcher(sessionId).matches()) {
            log.error("Invalid sessionId format: {}", sessionId);
            throw BadRequestException.builder().message("Invalid session ID format").build();
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupStaleCarts() {
        log.info("Cleaning up stale carts older than 90 days");
        cartRepository.deleteBySessionIdNotNullAndLastModifiedBefore(
                LocalDateTime.now().minusDays(365)
        );
    }
}*/

//import com.datasaz.ecommerce.exceptions.*;
//import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
//import com.datasaz.ecommerce.mappers.CartMapper;
//import com.datasaz.ecommerce.models.request.CartRequest;
//import com.datasaz.ecommerce.models.response.AppliedCouponResponse;
//import com.datasaz.ecommerce.models.response.CartResponse;
//import com.datasaz.ecommerce.repositories.CartRepository;
//import com.datasaz.ecommerce.repositories.ProductRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.*;
//import com.datasaz.ecommerce.services.interfaces.IBuyerCartService;
//import com.datasaz.ecommerce.services.interfaces.ICouponService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.domain.Pageable;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.Optional;
//import java.util.regex.Pattern;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class BuyerCartService implements IBuyerCartService {
//
//    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
//
//    private final CartRepository cartRepository;
//    private final ProductRepository productRepository;
//    private final UserRepository userRepository;
//    private final CartMapper cartMapper;
//    private final ICouponService couponService;
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse addToCart(String sessionId, CartRequest cartRequest) {
//        log.info("Adding item to cart for sessionId: {}", sessionId);
//        Cart cart = getOrCreateCart(sessionId);
//        return addItemToCart(cart, cartRequest);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse addToCartForUser(CartRequest cartRequest) {
//        log.info("Adding item to cart for authenticated user");
//        User user = getAuthenticatedUser();
//        Cart cart = getOrCreateUserCart(user);
//        return addItemToCart(cart, cartRequest);
//    }
//
//    private CartResponse addItemToCart(Cart cart, CartRequest cartRequest) {
//        if (cartRequest.getProductId() == null || cartRequest.getQuantity() <= 0) {
//            log.error("Invalid cart request: productId: {}, quantity: {}", cartRequest.getProductId(), cartRequest.getQuantity());
//            throw BadRequestException.builder().message("Invalid product ID or quantity").build();
//        }
//
//        Product product = productRepository.findById(cartRequest.getProductId())
//                .orElseThrow(() -> {
//                    log.error("Product not found for product id: {}", cartRequest.getProductId());
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
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse updateCartItem(String sessionId, Long cartItemId, int quantity) {
//        log.info("Updating cart item {} with quantity {} for sessionId: {}", cartItemId, quantity, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionId(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        return updateCartItemInternal(cart, cartItemId, quantity);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse updateCartItemForUser(Long cartItemId, int quantity) {
//        log.info("Updating cart item {} with quantity {} for authenticated user", cartItemId, quantity);
//        User user = getAuthenticatedUser();
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", user.getId());
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
//                });
//        return updateCartItemInternal(cart, cartItemId, quantity);
//    }
//
//    private CartResponse updateCartItemInternal(Cart cart, Long cartItemId, int quantity) {
//        CartItem cartItem = cart.getItems().stream()
//                .filter(item -> item.getId().equals(cartItemId))
//                .findFirst()
//                .orElseThrow(() -> {
//                    log.error("Cart item not found for item id: {}, cart id: {}", cartItemId, cart.getId());
//                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
//                });
//
//        Product product = productRepository.findById(cartItem.getProduct().getId())
//                .orElseThrow(() -> {
//                    log.error("Product not found for product id: {}", cartItem.getProduct().getId());
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
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse removeFromCart(String sessionId, Long cartItemId) {
//        log.info("Removing cart item {} for sessionId: {}", cartItemId, sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionId(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        return removeItemFromCartInternal(cart, cartItemId);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse removeFromCartForUser(Long cartItemId) {
//        log.info("Removing cart item {} for authenticated user", cartItemId);
//        User user = getAuthenticatedUser();
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", user.getId());
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
//                });
//        return removeItemFromCartInternal(cart, cartItemId);
//    }
//
//    private CartResponse removeItemFromCartInternal(Cart cart, Long cartItemId) {
//        CartItem cartItem = cart.getItems().stream()
//                .filter(item -> item.getId().equals(cartItemId))
//                .findFirst()
//                .orElseThrow(() -> {
//                    log.error("Cart item not found for cart id: {}, item id: {}", cart.getId(), cartItemId);
//                    return CartItemNotFoundException.builder().message(ExceptionMessages.CART_ITEM_NOT_FOUND + "Cart Item not Found: " + cartItemId).build();
//                });
//
//        cart.getItems().remove(cartItem);
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse clearCart(String sessionId) {
//        log.info("Clearing cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionId(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        cart.getItems().clear();
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse clearCartForUser() {
//        log.info("Clearing cart for authenticated user");
//        User user = getAuthenticatedUser();
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", user.getId());
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
//                });
//        cart.getItems().clear();
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse mergeCartOnLogin(String sessionId) {
//        log.info("Merging cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        User user = getAuthenticatedUser();
//        Cart userCart = getOrCreateUserCart(user);
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
//                            continue; // Skip this item and continue with others
//                        }
//                        existingItem.get().setQuantity(totalQuantity);
//                        existingItem.get().setPrice(product.getPrice());
//                        existingItem.get().setProductName(product.getName());
//                    } else {
//                        if (totalQuantity > product.getQuantity()) {
//                            log.warn("Skipping product id {} due to insufficient stock: requested {}, available {}", product.getId(), totalQuantity, product.getQuantity());
//                            continue; // Skip this item and continue with others
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
//                    continue; // Skip this item and continue with others
//                }
//            }
//            // Copy coupon if present
//            if (anonymousCart.getCoupon() != null) {
//                try {
//                    BigDecimal subtotal = calculateSubtotal(userCart);
//                    couponService.validateCoupon(anonymousCart.getCoupon().getCode(), userCart.getUser(), subtotal, userCart.getItems());
//                    userCart.setCoupon(anonymousCart.getCoupon());
//                } catch (Exception e) {
//                    log.warn("Failed to apply coupon {} from anonymous cart: {}", anonymousCart.getCoupon().getCode(), e.getMessage());
//                    // Skip coupon application but continue
//                }
//            }
//            cartRepository.delete(anonymousCart);
//        }
//
//        updateCartTotals(userCart);
//        cartRepository.save(userCart);
//        return cartMapper.toResponse(userCart);
//    }
//
//    /*
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse mergeCartOnLogin(String sessionId) {
//        log.info("Merging cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        User user = getAuthenticatedUser();
//        Cart userCart = getOrCreateUserCart(user);
//
//        Optional<Cart> anonymousCartOpt = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId);
//        if (anonymousCartOpt.isPresent()) {
//            Cart anonymousCart = anonymousCartOpt.get();
//            for (CartItem anonymousItem : anonymousCart.getItems()) {
//                Product product = productRepository.findById(anonymousItem.getProduct().getId())
//                        .orElseThrow(() -> {
//                            log.error("Product not found for product id: {}, sessionId: {}", anonymousItem.getProduct().getId(), sessionId);
//                            return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + anonymousItem.getProduct().getId()).build();
//                        });
//
//                int totalQuantity = anonymousItem.getQuantity();
//                Optional<CartItem> existingItem = userCart.getItems().stream()
//                        .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
//                        .findFirst();
//
//                if (existingItem.isPresent()) {
//                    totalQuantity += existingItem.get().getQuantity();
//                    if (totalQuantity > product.getQuantity()) {
//                        log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
//                        throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
//                    }
//                    existingItem.get().setQuantity(totalQuantity);
//                    existingItem.get().setPrice(product.getPrice());
//                    existingItem.get().setProductName(product.getName());
//                } else {
//                    if (totalQuantity > product.getQuantity()) {
//                        log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
//                        throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
//                    }
//                    CartItem newItem = CartItem.builder()
//                            .cart(userCart)
//                            .product(anonymousItem.getProduct())
//                            .productName(product.getName())
//                            .price(product.getPrice())
//                            .quantity(anonymousItem.getQuantity())
//                            .build();
//                    userCart.getItems().add(newItem);
//                }
//            }
//            // Copy coupon if present
//            if (anonymousCart.getCoupon() != null) {
//                userCart.setCoupon(anonymousCart.getCoupon());
//            }
//            cartRepository.delete(anonymousCart);
//        }
//
//        updateCartTotals(userCart);
//        cartRepository.save(userCart);
//        return cartMapper.toResponse(userCart);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse mergeCartOnLogin(String sessionId) {
//        log.info("Merging cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        User user = getAuthenticatedUser();
//        Cart userCart = getOrCreateUserCart(user);
//
//        Optional<Cart> anonymousCartOpt = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId);
//        if (anonymousCartOpt.isPresent()) {
//            Cart anonymousCart = anonymousCartOpt.get();
//            for (CartItem anonymousItem : anonymousCart.getItems()) {
//                Product product = productRepository.findById(anonymousItem.getProduct().getId())
//                        .orElseThrow(() -> {
//                            log.error("Product not found for product id: {}, sessionId: {}", anonymousItem.getProduct().getId(), sessionId);
//                            return ProductNotFoundException.builder().message(ExceptionMessages.PRODUCT_NOT_FOUND + "Product not Found: " + anonymousItem.getProduct().getId()).build();
//                        });
//
//                int totalQuantity = anonymousItem.getQuantity();
//                Optional<CartItem> existingItem = userCart.getItems().stream()
//                        .filter(item -> item.getProduct().getId().equals(anonymousItem.getProduct().getId()))
//                        .findFirst();
//
//                if (existingItem.isPresent()) {
//                    totalQuantity += existingItem.get().getQuantity();
//                    if (totalQuantity > product.getQuantity()) {
//                        log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
//                        throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
//                    }
//                    existingItem.get().setQuantity(totalQuantity);
//                    existingItem.get().setPrice(product.getPrice());
//                    existingItem.get().setProductName(product.getName());
//                } else {
//                    if (totalQuantity > product.getQuantity()) {
//                        log.error("Insufficient stock for product id: {}, requested: {}, available: {}", product.getId(), totalQuantity, product.getQuantity());
//                        throw new InsufficientStockException(product.getId(), totalQuantity, product.getQuantity());
//                    }
//                    CartItem newItem = CartItem.builder()
//                            .cart(userCart)
//                            .product(anonymousItem.getProduct())
//                            .productName(product.getName())
//                            .price(product.getPrice())
//                            .quantity(anonymousItem.getQuantity())
//                            .build();
//                    userCart.getItems().add(newItem);
//                }
//            }
//            cartRepository.delete(anonymousCart);
//        }
//
//        updateCartTotals(userCart);
//        cartRepository.save(userCart);
//        return cartMapper.toResponse(userCart);
//    }*/
//
//    @Override
//    @Transactional(readOnly = true)
//    @Cacheable(value = "carts", key = "#sessionId")
//    public CartResponse getCart(String sessionId, Pageable pageable) {
//        log.info("Retrieving cart for sessionId: {}, pageable: {}", sessionId, pageable);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        return cartMapper.toResponse(cart, pageable);
//    }
//
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
//
//    @Override
//    @Transactional(readOnly = true)
//    @Cacheable(value = "carts", key = "'user-authenticated'")
//    public CartResponse getCartForUser(Pageable pageable) {
//        log.info("Retrieving cart for authenticated user");
//        User user = getAuthenticatedUser();
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", user.getId());
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
//                });
//        return cartMapper.toResponse(cart, pageable);
//    }
//
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
//        return applyCouponInternal(cart, couponIdentifier);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public AppliedCouponResponse applyCouponForUser(String couponIdentifier) {
//        log.info("Applying coupon {} to cart for authenticated user", couponIdentifier);
//        User user = getAuthenticatedUser();
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", user.getId());
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
//                });
//        return applyCouponInternal(cart, couponIdentifier);
//    }
//
//    private AppliedCouponResponse applyCouponInternal(Cart cart, String couponIdentifier) {
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
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse removeCoupon(String sessionId) {
//        log.info("Removing coupon from cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        Cart cart = cartRepository.findBySessionIdWithItemsAndCoupon(sessionId)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for sessionId: {}", sessionId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not Found: " + sessionId).build();
//                });
//        cart.setCoupon(null);
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = {"carts", "products"}, allEntries = true)
//    public CartResponse removeCouponForUser() {
//        log.info("Removing coupon from cart for authenticated user");
//        User user = getAuthenticatedUser();
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", user.getId());
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + user.getId()).build();
//                });
//        cart.setCoupon(null);
//        updateCartTotals(cart);
//        cartRepository.save(cart);
//        return cartMapper.toResponse(cart);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public BigDecimal calculateTotalAmount(Long userId) {
//        log.info("Calculating total amount for userId: {}", userId);
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.error("User not found for userId: {}", userId);
//                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
//                });
//
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", userId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
//                });
//
//        return cart.getTotalPrice();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public BigDecimal calculateDiscount(Long userId) {
//        log.info("Calculating discount for userId: {}", userId);
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> {
//                    log.error("User not found for userId: {}", userId);
//                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + userId).build();
//                });
//
//        Cart cart = cartRepository.findByUserWithItems(user)
//                .orElseThrow(() -> {
//                    log.error("Cart not found for userId: {}", userId);
//                    return CartNotFoundException.builder().message(ExceptionMessages.CART_NOT_FOUND + "Cart not found: " + userId).build();
//                });
//
//        return cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO;
//    }
//
//    private Cart getOrCreateCart(String sessionId) {
//        log.info("Getting or creating cart for sessionId: {}", sessionId);
//        validateSessionId(sessionId);
//        return cartRepository.findBySessionIdWithItemsAndCoupons(sessionId)
//                .orElseGet(() -> {
//                    Cart newCart = Cart.builder()
//                            .sessionId(sessionId)
//                            .totalPrice(BigDecimal.ZERO)
//                            .discount(BigDecimal.ZERO)
//                            .items(new ArrayList<>())
//                            .build();
//                    return cartRepository.save(newCart);
//                });
//    }
//
//    private Cart getOrCreateUserCart(User user) {
//        log.info("Getting or creating cart for userId: {}", user.getId());
//        return cartRepository.findByUserWithItems(user)
//                .orElseGet(() -> {
//                    Cart newCart = Cart.builder()
//                            .user(user)
//                            .sessionId(null) // User carts don't use sessionId
//                            .totalPrice(BigDecimal.ZERO)
//                            .discount(BigDecimal.ZERO)
//                            .items(new ArrayList<>())
//                            .build();
//                    return cartRepository.save(newCart);
//                });
//    }
//
//    private BigDecimal calculateSubtotal(Cart cart) {
//        return cart.getItems().stream()
//                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    private void updateCartTotals(Cart cart) {
//        BigDecimal subtotal = calculateSubtotal(cart);
//        BigDecimal discount = BigDecimal.ZERO;
//        if (cart.getCoupon() != null) {
//            discount = couponService.calculateDiscount(cart.getCoupon(), cart.getItems(), subtotal);
//        }
//        cart.setDiscount(discount);
//        cart.setTotalPrice(subtotal.subtract(discount).max(BigDecimal.ZERO));
//    }
//
//    private void validateSessionId(String sessionId) {
//        if (sessionId == null || !UUID_PATTERN.matcher(sessionId).matches()) {
//            log.error("Invalid sessionId format: {}", sessionId);
//            throw BadRequestException.builder().message("Invalid session ID format").build();
//        }
//    }
//
//    private User getAuthenticatedUser() {
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        return userRepository.findByEmailAddressAndDeletedFalse(email)
//                .orElseThrow(() -> {
//                    log.error("User not found for email: {}", email);
//                    return UserNotFoundException.builder().message(ExceptionMessages.USER_NOT_FOUND + "User not found: " + email).build();
//                });
//    }
//
//    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
//    @Transactional
//    public void cleanupStaleCarts() {
//        log.info("Cleaning up stale carts older than 90 days");
//        cartRepository.deleteBySessionIdNotNullAndLastModifiedBefore(
//                LocalDateTime.now().minusDays(90)
//        );
//    }
//}
