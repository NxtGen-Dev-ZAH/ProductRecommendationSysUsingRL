package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.UserPrivacySettingsRequest;
import com.datasaz.ecommerce.models.request.UserProfileRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.UserFollowersCountResponse;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface IUserProfileService {

    String toggleFollow(String targetEmail, String followerEmail);
    //UserDto followUser(String emailAddress);
    //UserDto unfollowUser(String emailAddress);

    UserProfileResponse updateProfile(UserProfileRequest request);

    UserProfileResponse updatePrivacySettings(UserPrivacySettingsRequest request);

    //UserProfileResponse updatePrivacySettings(UserPrivacySettingsRequest request, String email);

    Page<ProductResponse> getFavoriteProducts(String emailAddress, int page, int size);

    String toggleFavoriteProduct(Long productId);

    Page<UserSummaryResponse> getFollowers(String email, int page, int size, String viewerEmail);

    Page<UserSummaryResponse> getFollowings(String email, int page, int size, String viewerEmail);

    UserFollowersCountResponse getFollowerCount(String emailAddress);

    UserFollowersCountResponse getFollowingCount(String emailAddress);

    UserProfileResponse getProfile(String email);


    String uploadProfilePicture(UserProfileRequest request, String email);

    String uploadProfilePicture(MultipartFile file, String email);
    // UserDto uploadProfilePicture(String email, MultipartFile file);

}
