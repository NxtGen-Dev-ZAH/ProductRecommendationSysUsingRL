package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.CouponInvalidException;
import com.datasaz.ecommerce.models.request.CouponRequest;
import com.datasaz.ecommerce.models.response.CouponResponse;
import com.datasaz.ecommerce.repositories.CouponRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.ICouponSellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponSellerService implements ICouponSellerService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        User seller = getAuthenticatedSeller();
        validateCouponRequest(request);

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .state(request.getState() != null ? request.getState() : CouponState.INACTIVE)
                .category(request.getCategory())
                .couponScope(request.getCouponScope())
                .couponType(request.getCouponType())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .maxUses(request.getMaxUses())
                .maxUsesPerUser(request.getMaxUsesPerUser())
                .author(seller)
                .startFrom(request.getStartFrom())
                .endAt(request.getEndAt())
                .discountPercentage(request.getDiscountPercentage())
                .discountFixedAmount(request.getDiscountFixedAmount())
                .couponTrackings(new HashSet<>())
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Coupon created: {} by seller: {}", coupon.getCode(), seller.getEmailAddress());
        return mapToCouponResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long couponId, CouponRequest request) {
        User seller = getAuthenticatedSeller();
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> CouponInvalidException.builder().message("Coupon not found: " + couponId).build());

        if (!coupon.getAuthor().getId().equals(seller.getId())) {
            throw CouponInvalidException.builder().message("Only the coupon author can update it").build();
        }

        validateCouponRequest(request);
        coupon.setCode(request.getCode());
        coupon.setDescription(request.getDescription());
        coupon.setState(request.getState() != null ? request.getState() : coupon.getState());
        coupon.setCategory(request.getCategory());
        coupon.setCouponScope(request.getCouponScope());
        coupon.setCouponType(request.getCouponType());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setMaxUsesPerUser(request.getMaxUsesPerUser());
        coupon.setStartFrom(request.getStartFrom());
        coupon.setEndAt(request.getEndAt());
        coupon.setDiscountPercentage(request.getDiscountPercentage());
        coupon.setDiscountFixedAmount(request.getDiscountFixedAmount());

        coupon = couponRepository.save(coupon);
        log.info("Coupon updated: {} by seller: {}", coupon.getCode(), seller.getEmailAddress());
        return mapToCouponResponse(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        User seller = getAuthenticatedSeller();
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> CouponInvalidException.builder().message("Coupon not found: " + couponId).build());

        if (!coupon.getAuthor().getId().equals(seller.getId())) {
            throw CouponInvalidException.builder().message("Only the coupon author can delete it").build();
        }

        coupon.setState(CouponState.DELETED);
        couponRepository.save(coupon);
        log.info("Coupon deleted: {} by seller: {}", coupon.getCode(), seller.getEmailAddress());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getSellerCoupons() {
        User seller = getAuthenticatedSeller();
        List<Coupon> coupons = couponRepository.findByAuthor(seller);
        return coupons.stream().map(this::mapToCouponResponse).collect(Collectors.toList());
    }

    private User getAuthenticatedSeller() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (!user.getUserRoles().stream().anyMatch(role -> role.getRole().equals(RoleTypes.SELLER))) {
            throw new RuntimeException("User is not authorized as a seller");
        }
        return user;
    }

    private void validateCouponRequest(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw CouponInvalidException.builder().message("Coupon code already exists: " + request.getCode()).build();
        }
        if (request.getStartFrom().isAfter(request.getEndAt())) {
            throw CouponInvalidException.builder().message("Start date must be before end date").build();
        }
        if (request.getCouponType() == CouponType.PERCENTAGE && request.getDiscountPercentage() == null) {
            throw CouponInvalidException.builder().message("Discount percentage must be set for PERCENTAGE coupon").build();
        }
        if (request.getCouponType() == CouponType.FIXED && request.getDiscountFixedAmount() == null) {
            throw CouponInvalidException.builder().message("Discount fixed amount must be set for FIXED coupon").build();
        }
    }

    private CouponResponse mapToCouponResponse(Coupon coupon) {
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
                .authorId(coupon.getAuthor().getId())
                .startFrom(coupon.getStartFrom())
                .endAt(coupon.getEndAt())
                .discountPercentage(coupon.getDiscountPercentage())
                .discountFixedAmount(coupon.getDiscountFixedAmount())
                .build();
    }
}