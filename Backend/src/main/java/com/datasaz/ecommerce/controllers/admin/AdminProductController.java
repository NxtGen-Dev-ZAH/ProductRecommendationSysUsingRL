package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.services.interfaces.IAdminProductService;
import com.datasaz.ecommerce.services.interfaces.IProductImageService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('Role_SELLER')")
public class AdminProductController {

    private final IAdminProductService adminProductService;
    private final IProductImageService productImageService;

    //add pagination and verify to retrieve all products including deleted
    @GetMapping("/all")
    public List<ProductResponse> getAllProducts() {

        return adminProductService.findAllProducts();
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        log.info("Received getAllProducts request with pageable: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        return ResponseEntity.ok(adminProductService.findAllProducts(pageable));
    }

    @GetMapping("/search/pages")
    public Page<ProductResponse> searchAllProductsByName(@RequestParam String productName, Pageable pageable) {
        return adminProductService.findProductsByName(productName, pageable);
    }

    // QA UNIT: Tested with Postman and it works fine
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(adminProductService.findProductById(id));
    }

    @DeleteMapping("/{productId}")
    @RateLimiter(name = "productDelete")
    public ResponseEntity<Void> deleteProductByAdmin(
            @PathVariable Long productId,
            Authentication authentication) {
        String email = authentication.getName();
        adminProductService.deleteProductByAdmin(productId, email);
        return ResponseEntity.noContent().build();
    }

}
