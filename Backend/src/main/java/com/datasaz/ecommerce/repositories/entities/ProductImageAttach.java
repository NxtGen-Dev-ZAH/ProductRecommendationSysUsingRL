package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Table(name = "product_image_attach")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageAttach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String fileExtension;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] fileContent;

    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] thumbnailContent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private boolean isPrimary;

    @Column
    private Integer displayOrder;
}