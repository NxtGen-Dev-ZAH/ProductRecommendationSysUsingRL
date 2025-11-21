package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CartNotFoundException;
import com.datasaz.ecommerce.exceptions.InsufficientStockException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.mappers.AddressMapper;
import com.datasaz.ecommerce.mappers.OrderMapper;
import com.datasaz.ecommerce.models.request.OrderCheckoutRequest;
import com.datasaz.ecommerce.models.response.OrderResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.*;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.datasaz.ecommerce.configs.GroupConfig.CAT1_VAT_RATE;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository; //
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final IBuyerCartService buyerCartService;
    private final CouponService couponService;
    private final IAuditLogService auditLogService;
    private final IEmailService emailService;
    private final IPdfGenerator pdfGenerator;
    private final OrderShippingService orderShippingService;
    //    private final IOrderShippingAddressService orderShippingAddressService;
//    private final IOrderBillingAddressService orderBillingAddressService;
    private final CompanyRepository userCompanyRepository;

    private final AddressMapper orderShippingAddressMapper;
    private final AddressMapper orderBillingAddressMapper;

    private static final BigDecimal VAT_RATE = CAT1_VAT_RATE;

    private final CurrentUserService currentUserService;

    private record OrderTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal totalVAT,
            BigDecimal totalAmount
    ) {
    }



    @Override
    @Transactional
    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponse createOrder(OrderCheckoutRequest orderRequest) {
        User buyer = currentUserService.getCurrentUser();
        log.info("Creating order for user: {} at ", buyer.getEmailAddress());

        Cart cart = cartRepository.findByUserWithItems(buyer)
                .orElseThrow(() -> {
                    log.error("Cart not found for user id: {}", buyer.getId());
                    return CartNotFoundException.builder().message("Cart not found for user : " + buyer.getEmailAddress()).build();
                });

//        Cart cart = cartRepository.findByUserIdOrSessionId(buyer.getId(), request.getCartSessionId())
//                .orElseThrow(() -> new CartNotFoundException("Cart not found for user/session"));

        List<CartItem> itemsToOrder;
        if (orderRequest.getSelectedCartItemIds() != null && !orderRequest.getSelectedCartItemIds().isEmpty()) {
            // Partial checkout: Filter the cart items by the provided IDs
            itemsToOrder = cart.getItems().stream()
                    .filter(item -> orderRequest.getSelectedCartItemIds().contains(item.getId()))
                    .collect(Collectors.toList());

            if (itemsToOrder.isEmpty()) {
                throw BadRequestException.builder().message("No valid items selected for checkout.").build();
            }
        } else {
            // Full checkout: Use all items in the cart
            itemsToOrder = cart.getItems();
        }

        validateAndLockStock(itemsToOrder);
        OrderTotals calculatedTotals = calculateOrderTotals(itemsToOrder, cart.getCoupon());

        Order newOrder = buildOrder(buyer, cart, itemsToOrder, orderRequest, calculatedTotals);
        Order savedOrder = orderRepository.save(newOrder);

        updateStockAndCleanupCart(cart, itemsToOrder);

        auditLogService.logAction(
                buyer.getEmailAddress(),
                "ORDER_CREATED",
                "Order created with ID: " + savedOrder.getId() + " from " + itemsToOrder.size() + " items."
        );


        return orderMapper.toResponse(savedOrder);

        // Validate cart belongs to user
//        if (cart.getUser() != null && !cart.getUser().getId().equals(orderRequest.getBuyerId())) {
//            log.error("Cart {} does not belong to user {}", orderRequest.getCartSessionId(), orderRequest.getBuyerId());
//            throw BadRequestException.builder().message("Cart does not belong to the specified user.").build();
//        }





        // Create order
//        Order order = Order.builder()
//                .orderStatus(OrderStatus.PENDING)
//                .orderDateTime(LocalDateTime.now())
//                .buyer(buyer)
//                .discountAmount(BigDecimal.ZERO)
//                .totalVAT(BigDecimal.ZERO)
//                .totalAmount(BigDecimal.ZERO)
//                .build();

        // Process cart items
//        BigDecimal subtotal = BigDecimal.ZERO;
//        List<OrderItem> orderItems = new ArrayList<>();
//        for (CartItem cartItem : cart.getItems()) {
//            Product product = productRepository.findByIdWithLock(cartItem.getProduct().getId())
//                    .orElseThrow(() -> {
//                        log.error("Product not found for cart item product id: {}", cartItem.getProduct().getId());
//                        return ResourceNotFoundException.builder().message("Product not found.").build();
//                    });
//
//            if (cartItem.getQuantity() > product.getQuantity()) {
//                log.error("Quantity {} is greater than the available quantity {} for product id: {}",
//                        cartItem.getQuantity(), product.getQuantity(), cartItem.getProduct().getId());
//                throw new InsufficientStockException(product.getId(), cartItem.getQuantity(), product.getQuantity());
//            }

//            OrderItem orderItem = OrderItem.builder()
//                    .quantity(cartItem.getQuantity())
//                    .productName(product.getName())
//                    .price(product.getPrice())
//                    .order(order)
//                    .product(product)
//                    .build();
//            orderItems.add(orderItem);
//
//            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
//            productRepository.save(product);
//
//            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
//        }
//
//        // Handle coupon
//        BigDecimal discountAmount = BigDecimal.ZERO;
//        Coupon coupon = null;
//        if (orderRequest.getUsedCouponId() != null) {
//            //TODO: remove coupon specific to company
//            final Coupon finalCoupon = couponService.validateCouponWithLock(
//                    orderRequest.getUsedCouponId(), buyer, subtotal, cart.getItems(), null);
//            coupon = finalCoupon;
//            discountAmount = couponService.calculateDiscount(finalCoupon, cart.getItems(), subtotal);
//
//            Product trackingProduct = null;
//            Category trackingCategory = null;
//            if (finalCoupon.getCategory() == CouponCategory.PRODUCT_SPECIFIC) {
//                trackingProduct = cart.getItems().stream()
//                        .filter(item -> couponService.isItemEligibleForCoupon(finalCoupon, item))
//                        .map(CartItem::getProduct)
//                        .findFirst()
//                        .orElse(null);
//            } else if (finalCoupon.getCategory() == CouponCategory.CATEGORY_SPECIFIC) {
//                trackingCategory = cart.getItems().stream()
//                        .filter(item -> couponService.isItemEligibleForCoupon(finalCoupon, item))
//                        .map(item -> item.getProduct().getCategory())
//                        .filter(Objects::nonNull)
//                        .findFirst()
//                        .orElse(null);
//            }
//
//            couponService.trackCouponUsage(finalCoupon, buyer, order, trackingProduct, trackingCategory);
//        }
//
//        // Calculate totals
//        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
//        BigDecimal totalVAT = taxableAmount.multiply(VAT_RATE);
//        BigDecimal totalAmount = taxableAmount.add(totalVAT);
//
//        // Create invoice
//        Invoice invoice = Invoice.builder()
//                .invoiceNumber("INV-" + System.currentTimeMillis())
//                .issuedAt(LocalDateTime.now())
//                .totalAmount(totalAmount)
//                .order(order)
//                .build();
//        invoiceRepository.save(invoice);
//
//        // Link invoice to order items
//        orderItems.forEach(item -> item.setInvoice(invoice));
//
//        // Set order details
//        order.setUsedCoupon(coupon);
//        order.setDiscountAmount(discountAmount);
//        order.setTotalVAT(totalVAT);
//        order.setTotalAmount(totalAmount);
//        order.setItems(orderItems);
//
//        orderRepository.save(order);
//        orderItemRepository.saveAll(orderItems);

        // TODO:
        // Add shipping and billing details if provided
//        if (orderRequest.getShippingDetails() != null) {
//            orderShippingService.createOrderShipping(order.getId(), orderRequest.getShippingDetails());
//        }
//        if (orderRequest.getShippingAddress() != null) {
//            orderShippingAddressService.createOrderShippingAddress(order.getId(), orderRequest.getShippingAddress());
//        }
//        if (orderRequest.getBillingAddress() != null) {
//            orderBillingAddressService.createOrderBillingAddress(order.getId(), orderRequest.getBillingAddress());
//        }

        // Clear cart
//        cartService.clearCart(orderRequest.getCartSessionId());
//
//        // Audit log
//        auditLogService.logAction(
//                buyer.getEmailAddress(),
//                "ORDER_CREATED",
//                buyer.getEmailAddress(),
//                "Order created with ID: " + order.getId()
//        );
//
//        // Generate and send invoice email
//        try {
//            File invoiceFile = new File("invoice_" + order.getId() + ".pdf");
//            pdfGenerator.generatePdf(invoice);
//            emailService.sendInvoiceEmail(buyer.getEmailAddress(), invoiceFile);
//            log.info("Sent invoice email to {} for order {}", buyer.getEmailAddress(), order.getId());
//        } catch (Exception e) {
//            log.error("Failed to send invoice email to {}: {}", buyer.getEmailAddress(), e.getMessage());
//        }
//
//        return orderMapper.toResponse(order);
    }

    private void validateAndLockStock(List<CartItem> items) {
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (product.getQuantity() < item.getQuantity()) {
                log.error("Insufficient stock for product ID: {}", product.getId());
                throw InsufficientStockException.builder()
                        .message("Insufficient stock for product: " + product.getName())
                        .build();
            }
            // Temporarily decrease stock to prevent race conditions during this transaction
            // A dedicated stock service/table might handle this better in a larger system
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }
    }

    private OrderTotals calculateOrderTotals(List<CartItem> items, Coupon coupon) {
        // Calculate subtotal from only the selected items
        BigDecimal subtotal = items.stream()
                .map(item -> {
                    BigDecimal price = item.getProduct().getOfferPrice() != null
                            ? item.getProduct().getOfferPrice()
                            : item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon != null) {
            // Calculate discount for the *selected* items only
            discount = couponService.calculateDiscount(coupon, items, subtotal);
        }

        BigDecimal taxableAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);
        // Assuming a single VAT rate for simplicity
        BigDecimal totalVAT = taxableAmount.multiply(CAT1_VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(totalVAT).setScale(2, RoundingMode.HALF_UP);

        return new OrderTotals(subtotal, discount, totalVAT, totalAmount);
    }

    private Order buildOrder(User buyer, Cart cart, List<CartItem> itemsToOrder,
                             OrderCheckoutRequest request, OrderTotals totals) {

        // Persist addresses first as they are required foreign keys on the Order entity
//        Address shippingAddress = addressRepository.save(orderShippingAddressMapper.toEntity(request.getShippingAddressId()));
//        Address billingAddress = addressRepository.save(orderBillingAddressMapper.toEntity(request.getBillingAddress()));

        Order newOrder = Order.builder()
                .buyer(buyer)
                .orderStatus(OrderStatus.PENDING_PAYMENT) // Initial status before payment
                .orderDateTime(LocalDateTime.now())
                .usedCoupon(cart.getCoupon()) // Coupon entity from the cart
                .discountAmount(totals.discountAmount())
                // Assuming there is also a subtotal field on Order for completeness
                .totalVAT(totals.totalVAT())
                .totalAmount(totals.totalAmount())
//                .shippingAddress(shippingAddress)
//                .billingAddress(billingAddress)
                .build();

        // Map CartItems to OrderItems
        List<OrderItem> orderItems = itemsToOrder.stream()
                .map(cartItem -> OrderItem.builder()
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .productName(cartItem.getProduct().getName())
                        .price(cartItem.getProduct().getOfferPrice() != null ? cartItem.getProduct().getOfferPrice() : cartItem.getProduct().getPrice()) // Crucial: Price at time of purchase
                        .order(newOrder)
                        // Invoice will be attached later, after successful payment
                        .build())
                .collect(Collectors.toList());

        newOrder.setItems(orderItems);
        // Set OrderShipping (assuming OrderShippingRequest DTO exists and has a toEntity method)
//        if (request.getShippingDetails() != null) {
//            OrderShipping orderShipping = request.getShippingDetails().toEntity(newOrder);
//            newOrder.setOrderShipping(orderShipping);
//        }

        return newOrder;
    }

    private void updateStockAndCleanupCart(Cart cart, List<CartItem> orderedItems) {
        // Remove the ordered items from the original cart
        cart.getItems().removeAll(orderedItems);

        // Delete the removed CartItems from the database
        // (Assuming CartItemRepository exists and allows bulk deletion or cascade removal)
        // If using JPA cascade/orphanRemoval on Cart.items, removal from the collection is enough.

        // Re-calculate cart totals since items (and potentially the coupon eligibility) changed
        buyerCartService.updateCartTotals(cart);
        cartRepository.save(cart);
    }

    // --- Other methods from snippet context (simplified) ---
    // You would include other methods like getOrder, getOrdersByUser, updateOrderStatus, etc., here.
//    @Override
//    public Order getOrderById(Long orderId) {
//        return orderRepository.findById(orderId)
//                .orElseThrow(() -> ResourceNotFoundException.builder().message("Order not found with ID: " + orderId).build());
//    }


   /* private final OrderRepository orderRepository;

    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderMapper orderMapper;
    private final CartService cartService;
    private final CouponService couponService;
    private final IAuditLogService auditLogService;
    private final IEmailService emailService;
    private final IPdfGenerator pdfGenerator;

    private final OrderShippingService orderShippingService;
    private final OrderShippingCredentialService orderShippingCredentialService;
    private final OrderBillingCredentialService orderBillingCredentialService;

    private static final BigDecimal VAT_RATE = CAT1_VAT_RATE;

    private final CompanyRepository userCompanyRepository;


    @Override
    @Transactional
    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Creating order for cartSessionId: {} and buyerId: {}", orderRequest.getCartSessionId(), orderRequest.getBuyerId());

        User user = userRepository.findById(orderRequest.getBuyerId())
                .orElseThrow(() -> {
                    log.error("User not found for user id: {}", orderRequest.getBuyerId());
                    return ResourceNotFoundException.builder().message("User not found.").build();
                });

        // Validate cart and user
        Cart cart = cartRepository.findByUserWithItems(user)
                .orElseThrow(() -> {
                    log.error("Cart not found for cart session id: {}", orderRequest.getCartSessionId());
                    return CartNotFoundException.builder().message("Cart not found.").build();
                });

        // Validate cart belongs to user (if cart is associated with a user)
        if (cart.getUser() != null && !cart.getUser().getId().equals(orderRequest.getBuyerId())) {
            log.error("Cart {} does not belong to user {}", orderRequest.getCartSessionId(), orderRequest.getBuyerId());
            throw BadRequestException.builder().message("Cart does not belong to the specified user.").build();
        }

        // Create order
        Order order = Order.builder()
                .orderStatus(OrderStatus.PENDING)
                .orderDateTime(LocalDateTime.now())
                .buyer(user)
                .discountAmount(BigDecimal.ZERO)
                .totalVAT(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Process cart items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findByIdWithLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> {
                        log.error("Product not found for cart item product id: {}", cartItem.getProduct().getId());
                        return ResourceNotFoundException.builder().message("Product not found.").build();
                    });

            if (cartItem.getQuantity() > product.getQuantity()) {
                log.error("Quantity {} is greater than the available quantity {} for product id: {}",
                        cartItem.getQuantity(), product.getQuantity(), cartItem.getProduct().getId());
                throw new InsufficientStockException(product.getId(), cartItem.getQuantity(), product.getQuantity());
            }

            OrderItem orderItem = OrderItem.builder()
                    .quantity(cartItem.getQuantity())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .order(order)
                    .product(product)
                    .build();
            orderItems.add(orderItem);

            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Handle coupon
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (orderRequest.getUsedCouponId() != null) {
            final Coupon finalCoupon = couponService.validateCouponWithLock(
                    orderRequest.getUsedCouponId(), user, subtotal, cart.getItems(), user.getCompany());
            coupon = finalCoupon;
            discountAmount = couponService.calculateDiscount(finalCoupon, cart.getItems(), subtotal);

            Product trackingProduct = null;
            Category trackingCategory = null;
            if (finalCoupon.getCategory() == CouponCategory.PRODUCT_SPECIFIC) {
                trackingProduct = cart.getItems().stream()
                        .filter(item -> couponService.isItemEligibleForCoupon(finalCoupon, item))
                        .map(CartItem::getProduct)
                        .findFirst()
                        .orElse(null);
            } else if (finalCoupon.getCategory() == CouponCategory.CATEGORY_SPECIFIC) {
                trackingCategory = cart.getItems().stream()
                        .filter(item -> couponService.isItemEligibleForCoupon(finalCoupon, item))
                        .map(item -> item.getProduct().getCategory())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }

            couponService.trackCouponUsage(finalCoupon, user, order, trackingProduct, trackingCategory);
        }

        // Calculate totals
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal totalVAT = taxableAmount.multiply(VAT_RATE);
        BigDecimal totalAmount = taxableAmount.add(totalVAT);

        // Create invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + System.currentTimeMillis())
                .issuedAt(LocalDateTime.now())
                .totalAmount(totalAmount)
                .order(order)
                .build();
        invoiceRepository.save(invoice);

        // Link invoice to order items
        orderItems.forEach(item -> item.setInvoice(invoice));

        // Set order details
        order.setUsedCoupon(coupon);
        order.setDiscountAmount(discountAmount);
        order.setTotalVAT(totalVAT);
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        // Add shipping and billing details if provided
        if (orderRequest.getShippingDetails() != null) {
            orderShippingService.createOrderShipping(order.getId(), orderRequest.getShippingDetails());
        }
        if (orderRequest.getShippingCredential() != null) {
            orderShippingCredentialService.createOrderShippingCredential(order.getId(), orderRequest.getShippingCredential());
        }
        if (orderRequest.getBillingCredential() != null) {
            orderBillingCredentialService.createOrderBillingCredential(order.getId(), orderRequest.getBillingCredential());
        }

        // Clear cart
        cartService.clearCart(orderRequest.getCartSessionId());

        // Audit log
        auditLogService.logAction(
                user.getEmailAddress(),
                "ORDER_CREATED",
                user.getEmailAddress(),
                "Order created with ID: " + order.getId()
        );

        // Generate and send invoice email
        try {
            File invoiceFile = new File("invoice_" + order.getId() + ".pdf");
            pdfGenerator.generatePdf(invoice);
            emailService.sendInvoiceEmail(user.getEmailAddress(), invoiceFile);
            log.info("Sent invoice email to {} for order {}", user.getEmailAddress(), order.getId());
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", user.getEmailAddress(), e.getMessage());
        }

        return orderMapper.toResponse(order);
    }*/


//    //@Override
//    @Transactional
//    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
//    public OrderResponse createOrderV0(OrderRequest orderRequest) {
//        Cart cart = cartRepository.findBySessionId(orderRequest.getCartSessionId())
//                .orElseThrow(() -> {
//                    log.error("Cart not found for cart session id: {}", orderRequest.getCartSessionId());
//                    return CartNotFoundException.builder().message("Cart not found.").build();
//                });
//        User user = userRepository.findById(orderRequest.getBuyerId())
//                .orElseThrow(() -> {
//                    log.error("User not found for user id: {}", orderRequest.getBuyerId());
//                    return ResourceNotFoundException.builder().message("Resource not found.").build();
//                });
//
//
//        Order order = Order.builder()
//                .orderStatus(OrderStatus.PENDING)
//                .orderDateTime(LocalDateTime.now())
//                .buyer(user)
//                .discountAmount(BigDecimal.ZERO)
//                .totalVAT(BigDecimal.ZERO)
//                .totalAmount(BigDecimal.ZERO)
//                .build();
//
//        BigDecimal subtotal = BigDecimal.ZERO;
//        List<OrderItem> orderItems = new ArrayList<>();
//        for (CartItem cartItem : cart.getItems()) {
//            Product product = productRepository.findById(cartItem.getProduct().getId())
//                    .orElseThrow(() ->
//                    {
//                        log.error("Product not found for cart item product id: {}", cartItem.getProduct().getId());
//                        return ResourceNotFoundException.builder().message("Resource not found.").build();
//                    });
//
//            if (cartItem.getQuantity() > product.getQuantity()) {

    /// /                if (product.getQuantity() > 0) {
    /// /                    cartItem.setQuantity(product.getQuantity()); // set to maximum available quantity
    /// /                }
//                log.error("Quantity {} is greater than the available quantity {} for product id: {}", cartItem.getQuantity(), product.getQuantity(), cartItem.getProduct().getId());
//                throw new InsufficientStockException(
//                        product.getId(), cartItem.getQuantity(), product.getQuantity());
//            }
//
//            OrderItem orderItem = OrderItem.builder()
//                    .quantity(cartItem.getQuantity())
//                    .productName(product.getName())
//                    .price(product.getPrice())
//                    .order(order)
//                    .product(product)
//                    .build();
//            orderItems.add(orderItem);
//
//            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
//            productRepository.save(product);
//
//            subtotal = subtotal.add(
//                    product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
//        }
//
//        BigDecimal discountAmount = BigDecimal.ZERO;
//        Coupon coupon = null;
//        if (orderRequest.getUsedCouponIdentifier() != null) {
//            coupon = couponService.validateCoupon(
//                    orderRequest.getUsedCouponIdentifier(), user, subtotal, cart.getItems());
//            discountAmount = couponService.calculateDiscount(coupon, cart.getItems(), subtotal);
//
//            final Coupon finalCoupon = coupon;
//
//            Product trackingProduct = null;
//            Category trackingCategory = null;
//            if (coupon.getCategory() == CouponCategory.PRODUCT_SPECIFIC) {
//                trackingProduct = cart.getItems().stream()
//                        .filter(item -> couponService.isItemEligibleForCoupon(finalCoupon, item))
//                        .map(CartItem::getProduct)
//                        .findFirst()
//                        .orElse(null);
//            } else if (coupon.getCategory() == CouponCategory.CATEGORY_SPECIFIC) {
//                trackingCategory = cart.getItems().stream()
//                        .filter(item -> couponService.isItemEligibleForCoupon(finalCoupon, item))
//                        .map(item -> item.getProduct().getCategory())
//                        .filter(Objects::nonNull)
//                        .findFirst()
//                        .orElse(null);
//            }
//
//            couponService.trackCouponUsage(
//                    coupon, user, order, trackingProduct, trackingCategory);
//        }
//
//        // Create invoice
//        Invoice invoice = Invoice.builder()
//                .invoiceNumber("INV-" + System.currentTimeMillis())
//                .issuedAt(LocalDateTime.now())
//                .totalAmount(subtotal.subtract(discountAmount))
//                .order(order)
//                .build();
//        invoiceRepository.save(invoice);
//
//        // Link invoice to order items
//        orderItems.forEach(item -> item.setInvoice(invoice));
//
//        // Calculate VAT and totals
//        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
//        BigDecimal totalVAT = taxableAmount.multiply(VAT_RATE);
//        BigDecimal totalAmount = taxableAmount.add(totalVAT);
//
//        order.setUsedCoupon(coupon);
//        order.setDiscountAmount(discountAmount);
//        order.setTotalVAT(totalVAT);
//        order.setTotalAmount(totalAmount);
//
//        orderRepository.save(order);
//        orderItemRepository.saveAll(orderItems);
//
//        cartService.clearCart(orderRequest.getCartSessionId());
//
//        Order savedOrder = orderRepository.findById(order.getId())
//                .orElseThrow(() -> {
//                    log.error("Order not found for id: {}", order.getId());
//                    return ResourceNotFoundException.builder().message("Order not found.").build();
//                });
//        savedOrder.setItems(orderItems);
//        return orderMapper.toResponse(savedOrder);
//    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found for id: {}", id);
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });
        order.setItems(orderItemRepository.findByOrderId(id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found for id: {}", id);
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });
        order.setItems(orderItemRepository.findByOrderId(id));
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for user id: {}", userId);
                    return ResourceNotFoundException.builder().message("User not found.").build();
                });
        return orderRepository.findByBuyerId(userId).stream()
                .map(order -> {
                    order.setItems(orderItemRepository.findByOrderId(order.getId()));
                    return orderMapper.toResponse(order);
                })
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findByIdWithLock(id)
                .orElseThrow(() -> {
                    log.error("Order not found for id: {}", id);
                    return ResourceNotFoundException.builder().message("Order not found.").build();
                });
        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setOrderStatus(newStatus);
            orderRepository.save(order);
            order.setItems(orderItemRepository.findByOrderId(id));

            // Audit log
            auditLogService.logAction(
                    order.getBuyer().getEmailAddress(),
                    "ORDER_STATUS_UPDATED",
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    "Order ID: " + id + " updated to status: " + newStatus
            );

            // Send email notification
            try {
                emailService.sendEmail(
                        order.getBuyer().getEmailAddress(),
                        "Order Status Update",
                        String.format(
                                "<h2>Order Status Update</h2>" +
                                        "<p>Your order with ID %d has been updated to status: %s</p>" +
                                        "<p>Thank you for shopping with us!</p>",
                                id, newStatus
                        )
                );
                log.info("Sent status update email to {} for order {}", order.getBuyer().getEmailAddress(), id);
            } catch (Exception e) {
                log.error("Failed to send status update email to {}: {}", order.getBuyer().getEmailAddress(), e.getMessage());
            }

            return orderMapper.toResponse(order);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {} for order id: {}", status, id);
            throw ResourceNotFoundException.builder().message("Invalid order status: " + status).build();
        }
    }

    @Override
    @Transactional
    public Order saveOrder(Order order) {
        log.info("Saving order with ID: {}", order.getId());
        try {
            // Ensure totals are calculated correctly
            BigDecimal subtotal = order.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal taxableAmount = subtotal.subtract(discount);
            BigDecimal totalVAT = taxableAmount.multiply(VAT_RATE);
            BigDecimal totalAmount = taxableAmount.add(totalVAT);

            order.setTotalVAT(totalVAT);
            order.setTotalAmount(totalAmount);
            order.setOrderDateTime(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);
            auditLogService.logAction(
                    savedOrder.getBuyer().getEmailAddress(),
                    "ORDER_SAVED",
                    "Order saved with ID: " + savedOrder.getId()
            );
            return savedOrder;
        } catch (Exception e) {
            log.error("Error while saving order: {}", e.getMessage());
            throw BadRequestException.builder().message("Error while saving order: " + e.getMessage()).build();
        }
    }

}
