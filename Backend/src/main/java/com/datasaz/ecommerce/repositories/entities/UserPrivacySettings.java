package com.datasaz.ecommerce.repositories.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_privacy_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility")
    private Visibility profileVisibility;// = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "followers_visibility")
    private Visibility followersVisibility;// = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "following_visibility")
    private Visibility followingVisibility;// = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "favorites_visibility")
    private Visibility favoritesVisibility;// = Visibility.PUBLIC;

    public enum Visibility {
        PUBLIC, FOLLOWERS, PRIVATE
    }
}