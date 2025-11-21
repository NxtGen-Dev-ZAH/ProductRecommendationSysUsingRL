package com.datasaz.ecommerce.repositories.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "company")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column
    private String logoUrl;

    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] logoContent;

    @Column
    private String logoContentType;

    @Column
    private String logoFileExtension;

    @Column(nullable = false)
    private boolean deleted;// = false;

    @Column
    private Long deletedByUserId;

//    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Product> products = new HashSet<>();

//    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<UserCompany> userCompanies = new HashSet<>();

    private String registrationNumber;
    private String vatNumber;
    //private String address; // del: replaced with the association to address.
    private String contactEmail;

    // verify if not needed in case of multiple admins, which is possible
    @OneToOne
    @JoinColumn(name = "primary_admin_id")
    private User primaryAdmin; // Primary COMPANY_ADMIN_SELLER

    //remove initialization ?  = new HashSet<>();
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<CompanyAdminRights> adminRights;// = new HashSet<>();

//    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<Address> addresses;

    @Version
    private Long version;

}