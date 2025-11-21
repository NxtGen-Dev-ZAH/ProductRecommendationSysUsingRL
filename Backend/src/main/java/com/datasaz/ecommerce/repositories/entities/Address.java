package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Table(name = "addresses")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Email
    @Size(max = 255)
    @Column
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String addressLine1;

    @Size(max = 255)
    @Column
    private String addressLine2;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String city;

    @Size(max = 100)
    @Column
    private String state;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String postalCode;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String country;

    @Size(max = 20)
    @Column
    private String phoneNumber;

    @Size(max = 255)
    @Column
    private String reference; // Optional, for delivery instructions or notes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType addressType; // BILLING, SHIPPING, USER, COMPANY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Optional, for user-associated addresses

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company; // Optional, for company-associated addresses (e.g., expeditor)

    @Column(nullable = false)
    private boolean isDefault; // Indicates if this is the default address for the user/company

    @Version
    private Long version;
}