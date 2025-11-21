//package com.datasaz.ecommerce.repositories.entities;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//
/// /this entity may not be used; maintaining OneToMany relationship b/w user and company
//@Entity
//@Table(name = "user_company")
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class UserCompany {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "company_id", nullable = false)
//    private Company company;
//
//    @Column(length = 50)
//    private String role; // e.g., "ADMIN", "EMPLOYEE"
//}
