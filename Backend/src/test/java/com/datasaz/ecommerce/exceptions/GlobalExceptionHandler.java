// src/test/java/com/datasaz/ecommerce/exceptions/GlobalExceptionHandler.java
package com.datasaz.ecommerce.exceptions;

import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.exceptions.response.ExceptionResponse;
import com.datasaz.ecommerce.models.dto.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Centralized method to build ExceptionResponse
    private ExceptionResponse buildExceptionResponse(String message, HttpStatus status, String error, HttpServletRequest request, Map<String, String> errors) {
        return ExceptionResponse.builder()
                .message(message)
                .status(status.value())
                .error(error)
                .path(request.getRequestURI())
                .timestamp(System.currentTimeMillis())
                .errors(errors)
                .build();
    }

    // Helper method to create ResponseEntity
    private ResponseEntity<Object> buildResponse(String message, HttpStatus status, String error, HttpServletRequest request, Map<String, String> errors) {
        ExceptionResponse response = buildExceptionResponse(message, status, error, request, errors);
        return ResponseEntity.status(status).body(response);
    }

    // Overload for non-validation exceptions (no errors map)
    private ResponseEntity<Object> buildResponse(String message, HttpStatus status, String error, HttpServletRequest request) {
        return buildResponse(message, status, error, request, null);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Object> handleCategoryNotFoundException(CategoryNotFoundException ex, HttpServletRequest request) {
        log.warn("Category not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.CATEGORY_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "CategoryNotFound", request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.USER_ALREADY_EXISTS + ex.getMessage(), HttpStatus.CONFLICT, "UserAlreadyExists", request);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Object> handleRateLimitException(RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildResponse("Too many requests, please try again later.", HttpStatus.TOO_MANY_REQUESTS, "RateLimitExceeded", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.INVALID_CREDENTIALS + ex.getMessage(), HttpStatus.UNAUTHORIZED, "AuthenticationError", request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.UNAUTHORIZED_ACCESS + ex.getMessage(), HttpStatus.UNAUTHORIZED, "Unauthorized", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.UNAUTHORIZED_ACCESS + "Insufficient permissions.", HttpStatus.FORBIDDEN, "AccessDenied", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid request body: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.BAD_REQUEST + "Invalid request body format.", HttpStatus.BAD_REQUEST, "InvalidRequestBody", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing));
        return buildResponse(ExceptionMessages.BAD_REQUEST + "Validation failed for request.", HttpStatus.BAD_REQUEST, "ValidationError", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (existing, replacement) -> existing));
        return buildResponse(ExceptionMessages.BAD_REQUEST + "Validation failed for request parameters.", HttpStatus.BAD_REQUEST, "ConstraintViolation", request, errors);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Object> handleCartItemNotFoundException(CartItemNotFoundException ex, HttpServletRequest request) {
        log.warn("Cart item not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.CART_ITEM_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "CartItemNotFound", request);
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Object> handleCartNotFoundException(CartNotFoundException ex, HttpServletRequest request) {
        log.warn("Cart not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.CART_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "CartNotFound", request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleProductNotFoundException(ProductNotFoundException ex, HttpServletRequest request) {
        log.warn("Product not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.PRODUCT_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "ProductNotFound", request);
    }

    @ExceptionHandler(ConflictFoundException.class)
    public ResponseEntity<Object> handleConflictFoundException(ConflictFoundException ex, HttpServletRequest request) {
        log.warn("Company name conflict found. Admin approval required.");
        return buildResponse(ExceptionMessages.CONFLICT_EXCEPTION + ex.getMessage(), HttpStatus.CONFLICT, "CompanyNameConflictFound", request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Object> handleInsufficientStockException(InsufficientStockException ex, HttpServletRequest request) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.INSUFFICIENT_STOCK + ex.getMessage(), HttpStatus.BAD_REQUEST, "InsufficientStock", request);
    }

    @ExceptionHandler(CouponInvalidException.class)
    public ResponseEntity<Object> handleCouponInvalidException(CouponInvalidException ex, HttpServletRequest request) {
        log.warn("Invalid coupon: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.INVALID_COUPON + ex.getMessage(), HttpStatus.BAD_REQUEST, "CouponInvalid", request);
    }

    @ExceptionHandler(CouponExpiredException.class)
    public ResponseEntity<Object> handleCouponExpiredException(CouponExpiredException ex, HttpServletRequest request) {
        log.warn("Coupon expired: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.COUPON_EXPIRED + ex.getMessage(), HttpStatus.BAD_REQUEST, "CouponExpired", request);
    }

    @ExceptionHandler(CouponLimitExceededException.class)
    public ResponseEntity<Object> handleCouponLimitExceededException(CouponLimitExceededException ex, HttpServletRequest request) {
        log.warn("Coupon limit exceeded: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.COUPON_LIMIT_EXCEEDED + ex.getMessage(), HttpStatus.BAD_REQUEST, "CouponLimitExceeded", request);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Object> handleOptimisticLockException(OptimisticLockException ex, HttpServletRequest request) {
        log.warn("Concurrent update conflict: {}", ex.getMessage());
        return buildResponse("Concurrent update conflict, please try again.", HttpStatus.CONFLICT, "OptimisticLockError", request);
    }

    @ExceptionHandler(PessimisticLockException.class)
    public ResponseEntity<Object> handlePessimisticLockException(PessimisticLockException ex, HttpServletRequest request) {
        log.warn("High demand lock issue: {}", ex.getMessage());
        return buildResponse("Unable to process request due to high demand, please try again later.", HttpStatus.SERVICE_UNAVAILABLE, "PessimisticLockError", request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.BAD_REQUEST + ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                "BadRequest",
                request);
    }

    @ExceptionHandler(ContentTooLargeException.class)
    public ResponseEntity<Object> handleContentTooLargeException(ContentTooLargeException ex, HttpServletRequest request) {
        log.warn("Payload too large: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.CONTENT_TOO_LARGE + ex.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE, "PayloadTooLarge", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.RESOURCE_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "ResourceNotFound", request);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<Object> handleTechnicalException(TechnicalException ex, HttpServletRequest request) {
        log.error("Technical error: {}", ex.getMessage(), ex);
        return buildResponse(ExceptionMessages.TECHNICAL_EXCEPTION + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "TechnicalError", request);
    }

    @ExceptionHandler(IllegalParameterException.class)
    public ResponseEntity<Object> handleIllegalParameterException(IllegalParameterException ex, HttpServletRequest request) {
        log.warn("Illegal parameter: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.BAD_REQUEST + ex.getMessage(), HttpStatus.BAD_REQUEST, "IllegalParameter", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.USER_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "UserNotFound", request);
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<Object> handleCompanyNotFoundException(CompanyNotFoundException ex, HttpServletRequest request) {
        log.warn("Company not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.COMPANY_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "CompanyNotFound", request);
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Object> handleAddressNotFoundException(AddressNotFoundException ex, HttpServletRequest request) {
        log.warn("Address not found: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.ADDRESS_NOT_FOUND + ex.getMessage(), HttpStatus.NOT_FOUND, "AddressNotFound", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        log.warn("Response status exception: {}", ex.getReason());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value()) != null ? HttpStatus.resolve(ex.getStatusCode().value()) : HttpStatus.INTERNAL_SERVER_ERROR;
        return buildResponse(ex.getReason() != null ? ex.getReason() : "An error occurred.", status, "ResponseStatusError", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        return buildResponse(
                ExceptionMessages.BAD_REQUEST + "Unsupported media type: " + ex.getContentType(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "MediaTypeNotSupported",
                request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.BAD_REQUEST + ex.getMessage(), HttpStatus.CONFLICT, "IllegalState", request);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReferenceException(PropertyReferenceException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Invalid sort property: " + ex.getPropertyName() + ".")
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalTransactionStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransactionStateException(IllegalTransactionStateException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .message("Transaction error: Unable to commit due to rollback-only state")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Object> handleInvalidRoleException(InvalidRoleException ex, HttpServletRequest request) {
        log.warn("Invalid role: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.INVALID_ROLE + ex.getMessage(), HttpStatus.BAD_REQUEST, "InvalidRole", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(ExceptionMessages.TECHNICAL_EXCEPTION + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "UnexpectedError", request);
    }
}

/*
@Slf4j
@ControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ExceptionResponse buildExceptionResponse(String message, HttpStatus status, String error, HttpServletRequest request, Map<String, String> errors) {
        return ExceptionResponse.builder()
                .message(message)
                .status(status.value())
                .error(error)
                .path(request.getRequestURI())
                .timestamp(System.currentTimeMillis())
                .errors(errors)
                .build();
    }

    // Helper method to create ResponseEntity
    private ResponseEntity<Object> buildResponse(String message, HttpStatus status, String error, HttpServletRequest request, Map<String, String> errors) {
        ExceptionResponse response = buildExceptionResponse(message, status, error, request, errors);
        return ResponseEntity.status(status).body(response);
    }

    // Overload for non-validation exceptions (no errors map)
    private ResponseEntity<Object> buildResponse(String message, HttpStatus status, String error, HttpServletRequest request) {
        return buildResponse(message, status, error, request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(ExceptionMessages.BAD_REQUEST + ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                "BadRequest",
                request);
    }

//    @ExceptionHandler(BadRequestException.class)
//    public ResponseEntity<ExceptionResponse> handleBadRequestException(
//            BadRequestException ex, HttpServletRequest request) {
//
//        ExceptionResponse response = ExceptionResponse.builder()
//                .message("BAD_REQUEST: " + ex.getMessage())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .error("BadRequest")
//                .path(request.getRequestURI())
//                .timestamp(System.currentTimeMillis())
//                .build();
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }


}*/
