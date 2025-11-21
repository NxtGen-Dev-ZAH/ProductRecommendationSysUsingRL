package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.UserFollowersCountResponse;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import org.springframework.data.domain.Page;

public interface IUserProfileVisitService {

    Page<UserSummaryResponse> getFollowers(String emailAddress, int page, int size, String viewerEmail);

    Page<UserSummaryResponse> getFollowings(String emailAddress, int page, int size, String viewerEmail);

    UserFollowersCountResponse getFollowerCount(String emailAddress);

    UserFollowersCountResponse getFollowingCount(String emailAddress);

    //UserDto getProfile(String emailAddress);
    UserProfileResponse getProfile(String email);

    UserPrivacySettings getPrivacySettings();

    Page<ProductResponse> getFavoriteProducts(String emailAddress, int page, int size);

}
