package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto.UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .emailAddress(user.getEmailAddress())
                .password(user.getPassword())
                .isResetPassword(user.getIsResetPassword())
                .resetToken(user.getResetToken())
                .provider(user.getProvider())
                .registrationDate(user.getRegistrationDate())
                .registrationIp(user.getRegistrationIp())
                .activationCode(user.getActivationCode())
                .isActivated(user.getIsActivated())
                .isBlocked(user.getIsBlocked())
                .lastLoginDate(user.getLastLoginDate())
                .location(user.getLocation())
                .deleted(user.getDeleted())
                .deletionToken(user.getDeletionToken())
                .phoneNumber(user.getPhoneNumber())
                .lastPasswordResetDate(user.getLastPasswordResetDate())
                .followerCount(userRepository.countFollowersByEmailAddress(user.getEmailAddress()))
                .followingCount(userRepository.countFollowingByEmailAddress(user.getEmailAddress()))
                .profilePictureUrl(user.getProfilePictureUrl())
                .imageContent(user.getImageContent() != null ? Base64.getEncoder().encodeToString(user.getImageContent()) : null)
                .imageContentType(user.getImageContentType());

        if (user.getPrivacySettings() != null) {
            builder.profileVisibility(user.getPrivacySettings().getProfileVisibility())
                    .followersVisibility(user.getPrivacySettings().getFollowersVisibility())
                    .followingVisibility(user.getPrivacySettings().getFollowingVisibility())
                    .favoritesVisibility(user.getPrivacySettings().getFavoritesVisibility());
        }

        // Map favoriteProducts to favoriteProductIds
        if (user.getFavoriteProducts() != null) {
            builder.favoriteProductIds(user.getFavoriteProducts().stream()
                    .map(Product::getId)
                    .collect(Collectors.toSet()));
        }

        // Map following to followingIds
        if (user.getFollowing() != null) {
            builder.followingIds(user.getFollowing().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet()));
        }

        // Map followers to followerIds
        if (user.getFollowers() != null) {
            builder.followerIds(user.getFollowers().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet()));
        }

        // Map userRoles to userRoles
        if (user.getUserRoles() != null) {
            builder.userRoles(user.getUserRoles());
        }

        // Map user Company to companyId
        if (user.getCompany() != null) {
            builder.companyId(user.getCompany().getId());
        }

        // Map customFields to CustomFieldDto
        if (user.getCustomFields() != null) {
            builder.customFields(user.getCustomFields().stream()
                    .map(cf -> UserDto.CustomFieldDto.builder()
                            .id(cf.getId())
                            .fieldKey(cf.getFieldKey())
                            .fieldValue(cf.getFieldValue())
                            .description(cf.getDescription())
                            .build())
                    .collect(Collectors.toSet()));
        }

        return builder.build();
    }

    public User toEntity(UserDto userDto) {
        if (userDto == null) {
            return null;
        }

        User.UserBuilder builder = User.builder()
                .id(userDto.getId())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .dateOfBirth(userDto.getDateOfBirth())
                .emailAddress(userDto.getEmailAddress())
                .password(userDto.getPassword())
                .isResetPassword(userDto.getIsResetPassword())
                .resetToken(userDto.getResetToken())
                .provider(userDto.getProvider())
                .registrationDate(userDto.getRegistrationDate())
                .registrationIp(userDto.getRegistrationIp())
                .activationCode(userDto.getActivationCode())
                .isActivated(userDto.getIsActivated())
                .isBlocked(userDto.getIsBlocked())
                .lastLoginDate(userDto.getLastLoginDate())
                .location(userDto.getLocation())
                .deleted(userDto.getDeleted())
                .deletionToken(userDto.getDeletionToken())
                .phoneNumber(userDto.getPhoneNumber())
                .lastPasswordResetDate(userDto.getLastPasswordResetDate())
                .profilePictureUrl(userDto.getProfilePictureUrl())
                .imageContent(userDto.getImageContent() != null ? Base64.getDecoder().decode(userDto.getImageContent()) : null)
                .imageContentType(userDto.getImageContentType());

        // Populate favoriteProducts
        if (userDto.getFavoriteProductIds() != null && !userDto.getFavoriteProductIds().isEmpty()) {
            Set<Product> favoriteProducts = userDto.getFavoriteProductIds().stream()
                    .map(productId -> productRepository.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId)))
                    .collect(Collectors.toSet());
            builder.favoriteProducts(favoriteProducts);
        } else {
            builder.favoriteProducts(new HashSet<>());
        }

        // Populate following
        if (userDto.getFollowingIds() != null && !userDto.getFollowingIds().isEmpty()) {
            Set<User> following = userDto.getFollowingIds().stream()
                    .map(userId -> userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId)))
                    .collect(Collectors.toSet());
            builder.following(following);
        } else {
            builder.following(new HashSet<>());
        }

        // Populate followers
        if (userDto.getFollowerIds() != null && !userDto.getFollowerIds().isEmpty()) {
            Set<User> followers = userDto.getFollowerIds().stream()
                    .map(userId -> userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId)))
                    .collect(Collectors.toSet());
            builder.followers(followers);
        } else {
            builder.followers(new HashSet<>());
        }

        // Populate userRoles
        if (userDto.getUserRoles() != null && !userDto.getUserRoles().isEmpty()) {
            builder.userRoles(userDto.getUserRoles());
        } else {
            builder.userRoles(new HashSet<>());
        }

        // Populate userCompany
        if (userDto.getCompanyId() != null) {
            Company company = companyRepository.findById(userDto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + userDto.getCompanyId()));
            builder.company(company);
        } else {
            builder.company(null);
        }

        return builder.build();
    }

    public UserSummaryResponse toSummaryResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .emailAddress(user.getEmailAddress())
                .displayName(user.getFirstName() + " " + user.getLastName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .imageContent(user.getImageContent() != null ? Base64.getEncoder().encodeToString(user.getImageContent()) : null)
                .imageContentType(user.getImageContentType())
                .build();
    }

    public UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .emailAddress(user.getEmailAddress())
                .phoneNumber(user.getPhoneNumber())
                .location(user.getLocation())
                .profilePictureUrl(user.getProfilePictureUrl())
                .imageContent(user.getImageContent() != null ? Base64.getEncoder().encodeToString(user.getImageContent()) : null)
                .imageContentType(user.getImageContentType())
                .privacySettings(user.getPrivacySettings())
                .userRoles(user.getUserRoles().stream()
                        .map(role -> role.getRole().name())
                        .collect(Collectors.toSet()))
                .favoriteProducts(user.getFavoriteProducts().stream()
                        .map(Product::getId)
                        .collect(Collectors.toSet()))
                .followers(user.getFollowers().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet()))
                .followersCount(user.getFollowers().size())
                .following(user.getFollowing().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet()))
                .followingCount(user.getFollowing().size())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .build();
    }
}


//    public User toEntity(UserDto userDto) {
//        if (userDto == null) {
//            return null;
//        }
//
//
//
//        return User.builder()
//                .id(userDto.getId())
//                .firstName(userDto.getFirstName())
//                .lastName(userDto.getLastName())
//                .emailAddress(userDto.getEmailAddress())
//                .password(userDto.getPassword())
//                .isResetPassword(userDto.getIsResetPassword())
//                .resetToken(userDto.getResetToken())
//                .provider(userDto.getProvider())
//                .registrationDate(userDto.getRegistrationDate())
//                .registrationIp(userDto.getRegistrationIp())
//                .activationCode(userDto.getActivationCode())
//                .isActivated(userDto.getIsActivated())
//                .isBlocked(userDto.getIsBlocked())
//                .lastLoginDate(userDto.getLastLoginDate())
//                .location(userDto.getLocation())
//                .deleted(userDto.getDeleted())
//                .deletionToken(userDto.getDeletionToken())

/// /                .favoriteProducts(new HashSet<>())
/// /                .following(new HashSet<>())
/// /                .followers(new HashSet<>())
/// /                .userRoles(new HashSet<>())
/// /                .userCompany(null)
//                .profilePictureUrl(userDto.getProfilePictureUrl())
//                .phoneNumber(userDto.getPhoneNumber())
//                .lastPasswordResetDate(userDto.getLastPasswordResetDate())
//                .build();
//    }

//@Component
//public class UsersMapper {
//
//    public UserDto toDto(Users users) {
//        if (users == null) {
//            return null;
//        }
//        return UserDto.builder()
//                .id(users.getId())
//                .emailAddress(users.getEmailAddress())
//                .lastName(users.getLastName())
//                .firstName(users.getFirstName())
//                .provider(users.getProvider())
//                .password(users.getPassword())
//                .phoneNumber(users.getPhoneNumber())
//                .resetToken(users.getResetToken())
//                .isResetPassword(users.getIsResetPassword())
//                .isActivated(users.getIsActivated())
//                .isBlocked(users.getIsBlocked())
//                .activationCode(users.getActivationCode())
//                .location(users.getLocation())
//                .registrationIp(users.getRegistrationIp())
//                .registrationDate(users.getRegistrationDate())
//                .lastLoginDate(users.getLastLoginDate())
//                .roles(users.getUserRoles())
//                .lastPasswordResetDate(users.getLastPasswordResetDate())
//                .companyId(users.getUserCompany() != null ? users.getUserCompany().getId() : null)
//                .build();
//
//    }
//
//
//    public Users toEntity(UserDto usersDto) {
//
//        if (usersDto == null) {
//            return null;
//        }
//
//        Users users = new Users();
//        users.setId(usersDto.getId());
//        users.setFirstName(usersDto.getFirstName());
//        users.setLastName(usersDto.getLastName());
//        users.setEmailAddress(usersDto.getEmailAddress());
//        users.setPassword(usersDto.getPassword());
//        users.setProvider(usersDto.getProvider());
//        users.setPhoneNumber(usersDto.getPhoneNumber());
//        users.setResetToken(usersDto.getResetToken());
//        users.setIsResetPassword(usersDto.getIsResetPassword());
//
//        users.setIsActivated(usersDto.getIsActivated());
//        users.setIsBlocked(usersDto.getIsBlocked());
//        users.setActivationCode(usersDto.getActivationCode());
//        users.setLocation(usersDto.getLocation());
//        users.setRegistrationIp(usersDto.getRegistrationIp());
//        users.setRegistrationDate(usersDto.getRegistrationDate());
//        users.setLastLoginDate(usersDto.getLastLoginDate());
//        users.setLastPasswordResetDate(usersDto.getLastPasswordResetDate());
//        users.setUserRoles(usersDto.getRoles());
//
//        //  Provide the code to set User Company - done above
//
/// /        UserCompany userCompany =  userCompanyRepository.findById(usersDto.getCompanyId())
/// /                .orElseThrow(() -> new IllegalArgumentException("mapToProduct: Category not found"));
/// /
/// /        users.setUserCompany(userCompany);
//
//        // Add the favorite products list - done above
//        users.setFavoriteProducts(null); // review/test ..
//        // Add the followers and following lists - done above
//        users.setFollowers(null);
//        users.setFollowing(null);
//
//
//        return users;
//    }
//}
