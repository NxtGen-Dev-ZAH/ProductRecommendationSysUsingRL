package com.datasaz.ecommerce.services;

import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.repositories.OrderRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.services.implementations.ProductService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductMapper productMapper;


    @InjectMocks
    private ProductService productService;

//    @Test
//    @DisplayName("Test save product")
//    public void testFindAllProducts() {
//        List<Product> mockProductList = new ArrayList<>();
//        Product product = Product.builder()
//                .id(1L)
//                .name("Product 1")
//                .price(BigDecimal.valueOf(10.0))
//                .build();
//        mockProductList.add(product);
//        // Mock the behavior of the productRepository
//        when(productMapper.toResponse(product)).thenReturn(ProductResponse.builder()
//                .id(product.getId())
//                .name(product.getName())
//                .price(product.getPrice())
//                .build());
//        when(productRepository.findAll()).thenReturn(mockProductList);
//
//               // Call the method under test
//        List<ProductResponse> result = productService.findAllProducts();
//
//        // Verify the result
//        assertEquals(1, result.size());
//        assertEquals("Product 1", result.get(0).getName());
//    }

//    @Test
//    @DisplayName("Test find product by id")
//    public void testFindProductById() {
//        Long productId = 1L;
//        Product mockProduct = Product.builder()
//                .id(productId)
//                .name("Product 1")
//                .price(BigDecimal.valueOf(10.0))
//                .build();
//        // Mock the behavior of the productRepository
//        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(mockProduct));
//        when(productMapper.toResponse(mockProduct)).thenReturn(ProductResponse.builder()
//                .id(mockProduct.getId())
//                .name(mockProduct.getName())
//                .price(mockProduct.getPrice())
//                .build());
//
//        // Call the method under test
//        ProductResponse result = productService.findProductById(productId);
//
//        // Verify the result
//        assertEquals("Product 1", result.getName());
//    }

//    @Test
//    @DisplayName("Test find product by category id")
//    public void testFindProductsByCategoryIdId() {
//        Long categoryId = 1L;
//        List<Product> mockProductList = new ArrayList<>();
//        Product product = Product.builder()
//                .id(1L)
//                .name("Product 1")
//                .price(BigDecimal.valueOf(10.0))
//                .build();
//        mockProductList.add(product);
//        // Mock the behavior of the productRepository
//        when(productRepository.findByCategoryId(categoryId)).thenReturn(mockProductList);
//        when(productMapper.toResponse(product)).thenReturn(ProductResponse.builder()
//                .id(product.getId())
//                .name(product.getName())
//                .price(product.getPrice())
//                .build());
//
//        // Call the method under test
//        List<ProductResponse> result = productService.findProductsByCategoryId(categoryId);
//
//        // Verify the result
//        assertEquals(1, result.size());
//        assertEquals("Product 1", result.get(0).getName());
//    }

//    @Test
//    @DisplayName("Test update product quantity")
//    public void testUpdateProductQuantity() {
//        Long productId = 1L;
//        int newQuantity = 5;
//        Product mockProduct = Product.builder()
//                .id(productId)
//                .name("Product 1")
//                .price(BigDecimal.valueOf(10.0))
//                .quantity(10)
//                .build();
//        // Mock the behavior of the productRepository
//        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(mockProduct));
//        when(productRepository.save(mockProduct)).thenReturn(mockProduct);
//
//        // Call the method under test
//        ProductResponse result = productService.updateProductQuantity(productId, newQuantity);
//
//        // Verify the result
//        assertEquals(newQuantity, mockProduct.getQuantity());
//    }

//    @Test
//    @DisplayName("Test update product")
//    public void testUpdateProduct() {
//        Long productId = 1L;
//        Product mockProduct = Product.builder()
//                .id(productId)
//                .name("Product 1")
//                .price(BigDecimal.valueOf(10.0))
//                .quantity(10)
//                .build();
//        // Mock the behavior of the productRepository
//        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(mockProduct));
//        when(productRepository.save(mockProduct)).thenReturn(mockProduct);
//        when(productMapper.toResponse(mockProduct)).thenReturn(ProductResponse.builder()
//                .id(mockProduct.getId())
//                .name(mockProduct.getName())
//                .price(mockProduct.getPrice())
//                .build());
//
//        // Call the method under test
//        ProductResponse result = productService.updateProduct(productId, null);
//
//        // Verify the result
//        assertEquals("Product 1", result.getName());
//    }

//    @Test
//    @DisplayName("Test delete product by Id")
//    public void testDeleteProduct() {
//        Long productId = 1L;
//        // Mock the behavior of the productRepository
//        doNothing().when(productRepository).deleteById(productId);
//        // Call the method under test
//        productService.deleteProduct(productId);
//        // Verify that the deleteById method was called with the correct argument
//        // (You can use Mockito.verify if needed)
//        Mockito.verify(productRepository).deleteById(productId);
//    }

//    @Test
//    @DisplayName("Test update product from request")
//    public void testUpdateProductProductFromRequest() {
//        Long productId = 1L;
//        Product mockProduct = Product.builder()
//                .id(productId)
//                .name("Product 1")
//                .price(BigDecimal.valueOf(10.0))
//                .quantity(10)
//                .build();
//        // Mock the behavior of the productRepository
//        when(productRepository.findById(productId)).thenReturn(java.util.Optional.of(mockProduct));
//        when(productRepository.save(mockProduct)).thenReturn(mockProduct);
//        when(productMapper.toResponse(mockProduct)).thenReturn(ProductResponse.builder()
//                .id(mockProduct.getId())
//                .name(mockProduct.getName())
//                .price(mockProduct.getPrice())
//                .build());
//
//        // Call the method under test
//        ProductResponse result = productService.updateProduct(productId, null);
//
//        // Verify the result
//        assertEquals("Product 1", result.getName());
//    }
}
