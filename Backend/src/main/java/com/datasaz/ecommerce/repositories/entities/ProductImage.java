package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;


@Table(name = "product_image")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Column
    private String contentType;

    @Column
    private long fileSize;

    @Column
    private String fileExtension;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private boolean isPrimary;

    @Column
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;


//    @PrePersist
//    public void prePersist() {
//        if (createdAt == null) {
//            createdAt = LocalDateTime.now();
//        }
//    }

//    @PrePersist
//    @PreUpdate
//    public void validatePrimary() {
//        if (isPrimary && product != null) {
//            // Note: Validation of single primary image per product should be handled at service/repository level
//            // or via a unique constraint in the database to avoid race conditions.
//        }
//    }


//    private String fileType;   //contentType
//    private byte[] content;  //file contents / fileUrl


}
