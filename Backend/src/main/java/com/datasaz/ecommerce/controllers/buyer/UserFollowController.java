package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.services.interfaces.IUserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping("/buyer/user/follow")
@RequiredArgsConstructor
public class UserFollowController {

    private final IUserFollowService userFollowService;

    @PostMapping("/{followedUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> followUser(
            @AuthenticationPrincipal Principal principal,
            @PathVariable Long followedUserId
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        UserDto updatedUser = userFollowService.followUser(principal.getName(), followedUserId);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{followedUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> unfollowUser(
            @AuthenticationPrincipal Principal principal,
            @PathVariable Long followedUserId
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        UserDto updatedUser = userFollowService.unfollowUser(principal.getName(), followedUserId);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Set<UserDto>> getFollowers(@PathVariable Long userId) {
        Set<UserDto> followers = userFollowService.getFollowers(userId);
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Set<UserDto>> getFollowing(@PathVariable Long userId) {
        Set<UserDto> following = userFollowService.getFollowing(userId);
        return ResponseEntity.ok(following);
    }

    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long userId) {
        long count = userFollowService.getFollowerCount(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{userId}/following/count")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        long count = userFollowService.getFollowingCount(userId);
        return ResponseEntity.ok(count);
    }
}