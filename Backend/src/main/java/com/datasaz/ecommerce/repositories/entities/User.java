

package com.datasaz.ecommerce.repositories.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String firstName;
    @Column(length = 100)
    private String lastName;
    @Column
    private LocalDate dateOfBirth;

    @Email
    @Column(nullable = false, unique = true)
    private String emailAddress;
    @Column(length = 20)
    private String phoneNumber;
    @Column(nullable = false)
    private String password;
    @Column
    private Boolean isResetPassword;
    @Column
    private String resetToken;
    @Column
    private String provider;
    @Column
    private LocalDateTime registrationDate;
    @Column
    private String registrationIp;
    @Column
    private String activationCode;
    @Column
    private Boolean isActivated;
    @Column
    private Boolean isBlocked;
    @Column
    private LocalDateTime lastLoginDate;
    @Column
    private LocalDateTime lastPasswordResetDate;

    @Column
    private String location;

    @Column
    private String profilePictureUrl;

    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] imageContent;

    @Column(length = 100)
    private String imageContentType;

    @Column(length = 10)
    private String imageFileExtension;

    @Column(nullable = false)
    private Boolean deleted;// = false;

    @Column
    private String deletionToken; // for OAuth2 users

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> favoriteProducts;

    @ManyToMany
    @JoinTable(
            name = "user_following",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private Set<User> following;// = new HashSet<>();

    @ManyToMany(mappedBy = "following")
    @JsonIgnore
    private Set<User> followers;// = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Roles> userRoles;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "FK_COMPANY_ID")
    private Company company;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserCustomFields> customFields;// = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private UserPrivacySettings privacySettings;

//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Address> addresses;

//    @OneToMany(mappedBy = "author", targetEntity = Product.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<Product> products;

//    @OneToMany(mappedBy = "buyer" , targetEntity = Order.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<Order> orders;

//    @OneToMany(mappedBy = "users", targetEntity = PaymentMethod.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private List<PaymentMethod> paymentMethods;

//    @OneToMany(mappedBy = "reviewer", targetEntity = ProductReview.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private Set<ProductReview> reviewsGiven;

    //@ManyToMany(mappedBy = "user")
    //private Set<CouponTracking> couponTracking = new HashSet<>();
}
