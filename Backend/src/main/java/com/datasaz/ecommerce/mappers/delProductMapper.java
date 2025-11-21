//package com.datasaz.ecommerce.mappers;
//
//
//import com.datasaz.ecommerce.models.Request.ProductRequest;
//import com.datasaz.ecommerce.models.Response.ProductResponse;
//import com.datasaz.ecommerce.repositories.entities.Product;
//
//import org.mapstruct.*;
//
//@Mapper(componentModel = "spring")
//public interface ProductMapper {
//
//    Product toEntity(ProductRequest request);
//
/// /    @Mapping(target = "id", source = "id")
/// /    @Mapping(target = "name", source = "name")
/// /    @Mapping(target = "price", source = "price")
/// /    @Mapping(target = "offerPrice", source = "offerPrice")
/// /    @Mapping(target = "quantity", source = "quantity")
/// /    @Mapping(target = "inventoryLocation", source = "inventoryLocation")
/// /    @Mapping(target = "warranty", source = "warranty")
/// /    @Mapping(target = "brand", source = "brand")
/// /    @Mapping(target = "productCode", source = "productCode")
/// /    @Mapping(target = "manufacturingPieceNumber", source = "manufacturingPieceNumber")
/// /    @Mapping(target = "manufacturingDate", source = "manufacturingDate")
/// /    @Mapping(target = "EAN", source = "EAN")
/// /    @Mapping(target = "manufacturingPlace", source = "manufacturingPlace")
/// /    @Mapping(target = "categoryId", source = "categoryId")
/// /    @Mapping(target = "authorId", source = "authorId")
//    ProductResponse toResponse(Product product);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
//}
