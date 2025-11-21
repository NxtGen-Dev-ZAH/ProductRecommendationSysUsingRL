package com.datasaz.ecommerce.services.implementations;


import com.datasaz.ecommerce.exceptions.CouponInvalidException;
import com.datasaz.ecommerce.models.request.CouponRequest;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponSellerServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CouponSellerService couponSellerService;

    private User seller;
    private Coupon coupon;
    private CouponRequest couponRequest;
    private CouponResponse couponResponse;
    private static final String SELLER_EMAIL = "seller@test.com";

    @BeforeEach
    void setUp() {
        // Set up seller with UserRole to match service expectation
        seller = new User();
        seller.setId(1L);
        seller.setEmailAddress(SELLER_EMAIL);
        //seller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        //seller.setUserRoles(Set.of(new Roles(null, RoleTypes.SELLER)));
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        // Note: Requested change was seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        // Using UserRole with "SELLER" to match service's getAuthenticatedSeller check.
        // If service uses Roles and RoleTypes, update to:
        // seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        // Verify role setup
        //assertTrue(seller.getUserRoles().stream().anyMatch(role -> role.getRole().equals("SELLER")),
        //        "Seller must have SELLER role");

        couponRequest = CouponRequest.builder()
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .author(seller)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .couponTrackings(new HashSet<>())
                .build();

        couponResponse = CouponResponse.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .authorId(seller.getId())
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .build();

        // Set up SecurityContextHolder for all tests
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
    }

    @Test
    void createCoupon_validRequest_success() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponSellerService.createCoupon(couponRequest);

        assertNotNull(response);
        assertEquals("SAVE10", response.getCode());
        assertEquals(seller.getId(), response.getAuthorId());
        assertEquals(CouponState.ACTIVE, response.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void createCoupon_duplicateCode_throwsCouponInvalidException() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Coupon code already exists: SAVE10", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_invalidDateRange_throwsCouponInvalidException() {
        couponRequest.setStartFrom(LocalDateTime.now().plusDays(30));
        couponRequest.setEndAt(LocalDateTime.now());

        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Start date must be before end date", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_missingDiscountPercentage_throwsCouponInvalidException() {
        couponRequest.setDiscountPercentage(null);

        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Discount percentage must be set for PERCENTAGE coupon", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_fixedCouponMissingDiscount_throwsCouponInvalidException() {
        couponRequest.setCouponType(CouponType.FIXED);
        couponRequest.setDiscountPercentage(null);
        couponRequest.setDiscountFixedAmount(null);

        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Discount fixed amount must be set for FIXED coupon", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("User not found: " + SELLER_EMAIL, exception.getMessage());
        verify(couponRepository, never()).existsByCode(anyString());
    }

    @Test
    void createCoupon_userNotSeller_throwsRuntimeException() {
        User buyer = new User();
        buyer.setId(1L);
        buyer.setEmailAddress(SELLER_EMAIL);
        //buyer.setUserRoles(Set.of(new UserRole(null, "BUYER")));
        buyer.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.BUYER).build())));

        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(buyer));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("User is not authorized as a seller", exception.getMessage());
        verify(couponRepository, never()).existsByCode(anyString());
    }

    @Test
    void updateCoupon_validRequest_success() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponSellerService.updateCoupon(1L, couponRequest);

        assertNotNull(response);
        assertEquals("SAVE10", response.getCode());
        assertEquals(seller.getId(), response.getAuthorId());
        assertEquals(CouponState.ACTIVE, response.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_couponNotFound_throwsCouponInvalidException() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Coupon not found: 1", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_unauthorizedSeller_throwsCouponInvalidException() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmailAddress("other@seller.com");
        //otherSeller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        otherSeller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        coupon.setAuthor(otherSeller);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Only the coupon author can update it", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_duplicateCode_throwsCouponInvalidException() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Coupon code already exists: SAVE10", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_validRequest_success() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        couponSellerService.deleteCoupon(1L);

        assertEquals(CouponState.DELETED, coupon.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_couponNotFound_throwsCouponInvalidException() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.deleteCoupon(1L));

        assertEquals("Coupon not found: 1", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_unauthorizedSeller_throwsCouponInvalidException() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmailAddress("other@seller.com");
        //otherSeller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        otherSeller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        coupon.setAuthor(otherSeller);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.deleteCoupon(1L));

        assertEquals("Only the coupon author can delete it", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void getSellerCoupons_validRequest_success() {
        when(couponRepository.findByAuthor(seller)).thenReturn(List.of(coupon));

        List<CouponResponse> response = couponSellerService.getSellerCoupons();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("SAVE10", response.get(0).getCode());
        assertEquals(seller.getId(), response.get(0).getAuthorId());
        verify(couponRepository).findByAuthor(seller);
    }

    @Test
    void getSellerCoupons_noCoupons_returnsEmptyList() {
        when(couponRepository.findByAuthor(seller)).thenReturn(List.of());

        List<CouponResponse> response = couponSellerService.getSellerCoupons();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        verify(couponRepository).findByAuthor(seller);
    }

    @Test
    void getSellerCoupons_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.getSellerCoupons());

        assertEquals("User not found: " + SELLER_EMAIL, exception.getMessage());
        verify(couponRepository, never()).findByAuthor(any(User.class));
    }

    @Test
    void getSellerCoupons_userNotSeller_throwsRuntimeException() {
        User buyer = new User();
        buyer.setId(1L);
        buyer.setEmailAddress(SELLER_EMAIL);
        //buyer.setUserRoles(Set.of(new Roles(null, RoleTypes.BUYER)));
        buyer.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.BUYER).build())));

        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(buyer));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.getSellerCoupons());

        assertEquals("User is not authorized as a seller", exception.getMessage());
        verify(couponRepository, never()).findByAuthor(any(User.class));
    }
}

/*
import com.datasaz.ecommerce.exceptions.CouponInvalidException;
import com.datasaz.ecommerce.models.request.CouponRequest;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponSellerServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CouponSellerService couponSellerService;

    private User seller;
    private Coupon coupon;
    private CouponRequest couponRequest;
    private CouponResponse couponResponse;
    private static final String SELLER_EMAIL = "seller@test.com";

    @BeforeEach
    void setUp() {
        // Set up seller with UserRole to match service expectation
        seller = new User();
        seller.setId(1L);
        seller.setEmailAddress(SELLER_EMAIL);
        //seller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        // Debug: Verify role setup
        //assertTrue(seller.getUserRoles().stream().anyMatch(role -> role.getRole().equals("SELLER")),
        //        "Seller role setup failed");
        // Note: If service uses Roles and RoleTypes, update to:
        // seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        couponRequest = CouponRequest.builder()
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .author(seller)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .couponTrackings(new HashSet<>())
                .build();

        couponResponse = CouponResponse.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .authorId(seller.getId())
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .build();

        // Set up SecurityContextHolder for all tests
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
    }

    @Test
    void createCoupon_validRequest_success() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponSellerService.createCoupon(couponRequest);

        assertNotNull(response);
        assertEquals("SAVE10", response.getCode());
        assertEquals(seller.getId(), response.getAuthorId());
        assertEquals(CouponState.ACTIVE, response.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void createCoupon_duplicateCode_throwsCouponInvalidException() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Coupon code already exists: SAVE10", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_invalidDateRange_throwsCouponInvalidException() {
        couponRequest.setStartFrom(LocalDateTime.now().plusDays(30));
        couponRequest.setEndAt(LocalDateTime.now());

        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Start date must be before end date", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_missingDiscountPercentage_throwsCouponInvalidException() {
        couponRequest.setDiscountPercentage(null);

        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Discount percentage must be set for PERCENTAGE coupon", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_fixedCouponMissingDiscount_throwsCouponInvalidException() {
        couponRequest.setCouponType(CouponType.FIXED);
        couponRequest.setDiscountPercentage(null);
        couponRequest.setDiscountFixedAmount(null);

        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Discount fixed amount must be set for FIXED coupon", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("User not found: " + SELLER_EMAIL, exception.getMessage());
        verify(couponRepository, never()).existsByCode(anyString());
    }

    @Test
    void createCoupon_userNotSeller_throwsRuntimeException() {
       // seller.setUserRoles(Set.of(new UserRole(null, "BUYER")));
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.BUYER).build())));

        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("User is not authorized as a seller", exception.getMessage());
        verify(couponRepository, never()).existsByCode(anyString());
    }

    @Test
    void updateCoupon_validRequest_success() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponSellerService.updateCoupon(1L, couponRequest);

        assertNotNull(response);
        assertEquals("SAVE10", response.getCode());
        assertEquals(seller.getId(), response.getAuthorId());
        assertEquals(CouponState.ACTIVE, response.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_couponNotFound_throwsCouponInvalidException() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Coupon not found: 1", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_unauthorizedSeller_throwsCouponInvalidException() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmailAddress("other@seller.com");
        //otherSeller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        otherSeller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        coupon.setAuthor(otherSeller);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Only the coupon author can update it", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_duplicateCode_throwsCouponInvalidException() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Coupon code already exists: SAVE10", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_validRequest_success() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        couponSellerService.deleteCoupon(1L);

        assertEquals(CouponState.DELETED, coupon.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_couponNotFound_throwsCouponInvalidException() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.deleteCoupon(1L));

        assertEquals("Coupon not found: 1", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_unauthorizedSeller_throwsCouponInvalidException() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmailAddress("other@seller.com");
        //otherSeller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        otherSeller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        coupon.setAuthor(otherSeller);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.deleteCoupon(1L));

        assertEquals("Only the coupon author can delete it", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void getSellerCoupons_validRequest_success() {
        when(couponRepository.findByAuthor(seller)).thenReturn(List.of(coupon));

        List<CouponResponse> response = couponSellerService.getSellerCoupons();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("SAVE10", response.get(0).getCode());
        assertEquals(seller.getId(), response.get(0).getAuthorId());
        verify(couponRepository).findByAuthor(seller);
    }

    @Test
    void getSellerCoupons_noCoupons_returnsEmptyList() {
        when(couponRepository.findByAuthor(seller)).thenReturn(List.of());

        List<CouponResponse> response = couponSellerService.getSellerCoupons();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        verify(couponRepository).findByAuthor(seller);
    }

    @Test
    void getSellerCoupons_userNotFound_throwsRuntimeException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.getSellerCoupons());

        assertEquals("User not found: " + SELLER_EMAIL, exception.getMessage());
        verify(couponRepository, never()).findByAuthor(any(User.class));
    }

    @Test
    void getSellerCoupons_userNotSeller_throwsRuntimeException() {
        //seller.setUserRoles(Set.of(new UserRole(null, "BUYER")));
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.BUYER).build())));

        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.getSellerCoupons());

        assertEquals("User is not authorized as a seller", exception.getMessage());
        verify(couponRepository, never()).findByAuthor(any(User.class));
    }
}*/

/*
import com.datasaz.ecommerce.exceptions.CouponInvalidException;
import com.datasaz.ecommerce.models.request.CouponRequest;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CouponSellerServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CouponSellerService couponSellerService;

    private User seller;
    private Coupon coupon;
    private CouponRequest couponRequest;
    private CouponResponse couponResponse;
    private ObjectMapper objectMapper;
    private static final String SELLER_EMAIL = "seller@test.com";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Set up seller with UserRole to match service expectation
        seller = new User();
        seller.setId(1L);
        seller.setEmailAddress(SELLER_EMAIL);
        //seller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        // Note: Requested change was seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        // Using UserRole with "SELLER" to match service's getAuthenticatedSeller check.
        // If service uses Roles and RoleTypes, update to:
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        couponRequest = CouponRequest.builder()
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .author(seller)
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .couponTrackings(new HashSet<>())
                .build();

        couponResponse = CouponResponse.builder()
                .id(1L)
                .code("SAVE10")
                .description("10% off")
                .state(CouponState.ACTIVE)
                .category(CouponCategory.GENERAL)
                .couponScope(CouponScope.ORDER)
                .couponType(CouponType.PERCENTAGE)
                .minimumOrderAmount(new BigDecimal("50.00"))
                .maxUses(100)
                .maxUsesPerUser(1)
                .authorId(seller.getId())
                .startFrom(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(30))
                .discountPercentage(new BigDecimal("10"))
                .discountFixedAmount(null)
                .build();
    }

    @Test
    void createCoupon_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponSellerService.createCoupon(couponRequest);

        assertNotNull(response);
        assertEquals("SAVE10", response.getCode());
        assertEquals(seller.getId(), response.getAuthorId());
        assertEquals(CouponState.ACTIVE, response.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void createCoupon_duplicateCode_throwsCouponInvalidException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Coupon code already exists: SAVE10", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_invalidDateRange_throwsCouponInvalidException() {
        couponRequest.setStartFrom(LocalDateTime.now().plusDays(30));
        couponRequest.setEndAt(LocalDateTime.now());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Start date must be before end date", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_missingDiscountPercentage_throwsCouponInvalidException() {
        couponRequest.setDiscountPercentage(null);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Discount percentage must be set for PERCENTAGE coupon", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_fixedCouponMissingDiscount_throwsCouponInvalidException() {
        couponRequest.setCouponType(CouponType.FIXED);
        couponRequest.setDiscountPercentage(null);
        couponRequest.setDiscountFixedAmount(null);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("Discount fixed amount must be set for FIXED coupon", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void createCoupon_userNotFound_throwsRuntimeException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("User not found: " + SELLER_EMAIL, exception.getMessage());
        verify(couponRepository, never()).existsByCode(anyString());
    }

    @Test
    void createCoupon_userNotSeller_throwsRuntimeException() {
        //seller.setUserRoles(Set.of(new UserRole(null, "BUYER")));
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.BUYER).build())));


        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.createCoupon(couponRequest));

        assertEquals("User is not authorized as a seller", exception.getMessage());
        verify(couponRepository, never()).existsByCode(anyString());
    }

    @Test
    void updateCoupon_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        CouponResponse response = couponSellerService.updateCoupon(1L, couponRequest);

        assertNotNull(response);
        assertEquals("SAVE10", response.getCode());
        assertEquals(seller.getId(), response.getAuthorId());
        assertEquals(CouponState.ACTIVE, response.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_couponNotFound_throwsCouponInvalidException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Coupon not found: 1", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_unauthorizedSeller_throwsCouponInvalidException() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmailAddress("other@seller.com");
        otherSeller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        //seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        coupon.setAuthor(otherSeller);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Only the coupon author can update it", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void updateCoupon_duplicateCode_throwsCouponInvalidException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.updateCoupon(1L, couponRequest));

        assertEquals("Coupon code already exists: SAVE10", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        couponSellerService.deleteCoupon(1L);

        assertEquals(CouponState.DELETED, coupon.getState());
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_couponNotFound_throwsCouponInvalidException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.deleteCoupon(1L));

        assertEquals("Coupon not found: 1", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void deleteCoupon_unauthorizedSeller_throwsCouponInvalidException() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmailAddress("other@seller.com");
        //otherSeller.setUserRoles(Set.of(new UserRole(null, "SELLER")));
        otherSeller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));
        coupon.setAuthor(otherSeller);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        CouponInvalidException exception = assertThrows(CouponInvalidException.class,
                () -> couponSellerService.deleteCoupon(1L));

        assertEquals("Only the coupon author can delete it", exception.getMessage());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void getSellerCoupons_validRequest_success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findByAuthor(seller)).thenReturn(List.of(coupon));

        List<CouponResponse> response = couponSellerService.getSellerCoupons();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("SAVE10", response.get(0).getCode());
        assertEquals(seller.getId(), response.get(0).getAuthorId());
        verify(couponRepository).findByAuthor(seller);
    }

    @Test
    void getSellerCoupons_noCoupons_returnsEmptyList() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));
        when(couponRepository.findByAuthor(seller)).thenReturn(List.of());

        List<CouponResponse> response = couponSellerService.getSellerCoupons();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        verify(couponRepository).findByAuthor(seller);
    }

    @Test
    void getSellerCoupons_userNotFound_throwsRuntimeException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.getSellerCoupons());

        assertEquals("User not found: " + SELLER_EMAIL, exception.getMessage());
        verify(couponRepository, never()).findByAuthor(any(User.class));
    }

    @Test
    void getSellerCoupons_userNotSeller_throwsRuntimeException() {
        //seller.setUserRoles(Set.of(new UserRole(null, "BUYER")));
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.BUYER).build())));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(SELLER_EMAIL);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmailAddressAndDeletedFalse(SELLER_EMAIL)).thenReturn(Optional.of(seller));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponSellerService.getSellerCoupons());

        assertEquals("User is not authorized as a seller", exception.getMessage());
        verify(couponRepository, never()).findByAuthor(any(User.class));
    }
}*/

