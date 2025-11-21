package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.UserDto;

import java.util.Set;

public interface IUserFollowService {

//    public boolean isFollowing(String follower, String followed);
//    public boolean isFollower(String follower, String followed);
//    public void deleteFollower(String follower, String followed);
//    public void deleteFollowed(String follower, String followed);
//    public void deleteAllFollowers(String followed);
//    public void deleteAllFollowed(String follower);

    UserDto followUser(String followerEmail, Long followedUserId);

    UserDto unfollowUser(String followerEmail, Long followedUserId);

    long getFollowingCount(Long userId);

    long getFollowerCount(Long userId);

    Set<UserDto> getFollowers(Long userId);

    Set<UserDto> getFollowing(Long userId);

}
