package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.entities.Category;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        log.info("mapToCategoryResponse: convert category to category response");
        if (category == null) {
            return null;
        }

        List<CategoryResponse> subResponses = category.getSubcategories() != null
                ? category.getSubcategories().stream()
                .map(this::toResponse)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .imageContent(category.getImageContent() != null ? Base64.getEncoder().encodeToString(category.getImageContent()) : null)
                .imageContentType(category.getImageContentType())
                .createdDate(category.getCreatedAt())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subcategories(subResponses)
                .build();
    }

    public Category toEntity(CategoryRequest request, Category parentCategory) {
        log.info("mapToCategory: convert category request to entity");
        if (request == null) {
            return null;
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parent(parentCategory)
                .subcategories(new ArrayList<>())
                .build();

        if (request.getImageContent() != null && !request.getImageContent().isEmpty()) {
            try {
                category.setImageContent(Base64.getDecoder().decode(request.getImageContent()));
                category.setImageContentType(request.getImageContentType());
                category.setImageFileExtension(getFileExtension(request.getImageContentType()));
            } catch (IllegalArgumentException e) {
                log.error("Invalid Base64 image content for category: {}", request.getName());
                throw new IllegalArgumentException("Invalid Base64 image content");
            }
        }

        return category;
    }

    private String getFileExtension(String contentType) {
        if (contentType == null) return "jpg";
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/bmp":
                return "bmp";
            case "image/tiff":
                return "tiff";
            case "image/vnd.wap.wbmp":
                return "wbmp";
            case "image/webp":
                return "webp";
            default:
                return "jpg";
        }
    }
}

//@Slf4j
//@Component
//public class CategoryMapper {
//
//    public CategoryResponse toResponse(Category category) {
//        log.info("mapToCategoryResponse: convert category to category response");
//        if (category == null) {
//            return null;
//        }
//
//        List<CategoryResponse> subResponses = category.getSubcategories().stream()
//                .map(this::toResponse)
//                .collect(Collectors.toList());
//
//        return CategoryResponse.builder()
//                .id(category.getId())
//                .name(category.getName())
//                .description(category.getDescription())
//                .parentId(category.getParent() != null ? category.getParent().getId() : null)
//                .subcategories(subResponses != null ? subResponses : List.of())
//                .build();
//    }
//
//    public Category toEntity(CategoryRequest request, Category parentCategory) {
//        log.info("mapToCategory: convert category request to entity");
//        return Category.builder()
//                .name(request.getName())
//                .description(request.getDescription())
//                .parent(parentCategory)
//                .build();
//    }
//}
