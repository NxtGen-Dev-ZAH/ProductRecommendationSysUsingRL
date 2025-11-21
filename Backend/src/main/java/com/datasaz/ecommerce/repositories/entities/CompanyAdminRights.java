package com.datasaz.ecommerce.repositories.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "company_admin_rights")
public class CompanyAdminRights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    @JsonBackReference
    private Company company;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private Boolean canAddRemoveSellers;// = false;

    @Column
    private Boolean canPromoteDemoteAdmins;// = false;

    @Column
    private Boolean canDelegateAdminRights;// = false;

    private Boolean approved;// = false;
}
