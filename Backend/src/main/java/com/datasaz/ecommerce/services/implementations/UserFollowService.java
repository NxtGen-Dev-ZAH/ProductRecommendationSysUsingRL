package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IUserFollowService;
import com.datasaz.ecommerce.utilities.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserFollowService implements IUserFollowService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Utility utility;

    //private UserDto userDto = new UserDto();
    private final UserMapper userMapper;


//    public Set<User> getFollowers(Long userId) {
//        return userRepository.findFollowersByUserId(userId);
//    }
//
//    public Set<User> getFollowing(Long userId) {
//
//        return userRepository.findFollowingByUserId(userId);
//    }


    @Transactional
    public UserDto followUser(String followerEmail, Long followedUserId) {
        log.info("UsersFollowService: User {} is attempting to follow user ID {}", followerEmail, followedUserId);

        User follower = userRepository.findByEmailAddressAndDeletedFalseWithFollowing(followerEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("Follower user not found: " + followerEmail).build());
        User followed = userRepository.findById(followedUserId)
                .orElseThrow(() -> UserNotFoundException.builder().message("Followed user not found: ID " + followedUserId).build());

        if (follower.getId().equals(followedUserId)) {
            log.error("User {} cannot follow themselves", followerEmail);
            throw BadRequestException.builder().message("Users cannot follow themselves").build();
        }

        if (follower.getFollowing().contains(followed)) {
            log.warn("User {} already follows user ID {}", followerEmail, followedUserId);
            throw BadRequestException.builder().message("User already follows this user").build();
        }

        follower.getFollowing().add(followed);
        userRepository.save(follower);
        log.info("User {} successfully followed user ID {}", followerEmail, followedUserId);

        return userMapper.toDto(follower);
    }

    @Transactional
    public UserDto unfollowUser(String followerEmail, Long followedUserId) {
        log.info("UsersFollowService: User {} is attempting to unfollow user ID {}", followerEmail, followedUserId);

        User follower = userRepository.findByEmailAddressAndDeletedFalseWithFollowing(followerEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("Follower user not found: " + followerEmail).build());
        User followed = userRepository.findById(followedUserId)
                .orElseThrow(() -> UserNotFoundException.builder().message("Followed user not found: ID " + followedUserId).build());

        if (!follower.getFollowing().contains(followed)) {
            log.warn("User {} does not follow user ID {}", followerEmail, followedUserId);
            throw BadRequestException.builder().message("User does not follow this user").build();
        }

        follower.getFollowing().remove(followed);
        userRepository.save(follower);
        log.info("User {} successfully unfollowed user ID {}", followerEmail, followedUserId);

        return userMapper.toDto(follower);
    }

    public Set<UserDto> getFollowers(Long userId) {
        log.info("UsersFollowService: Retrieving followers for user ID {}", userId);

        User user = userRepository.findByIdAndDeletedFalseWithFollowers(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found: ID " + userId).build());

        return user.getFollowers().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toSet());
    }

    public Set<UserDto> getFollowing(Long userId) {
        log.info("UsersFollowService: Retrieving following for user ID {}", userId);

        User user = userRepository.findByIdAndDeletedFalseWithFollowing(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found: ID " + userId).build());

        return user.getFollowing().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    public long getFollowerCount(Long userId) {
        log.info("UsersFollowService: Retrieving followers count for user ID {}", userId);

        User user = userRepository.findByIdAndDeletedFalseWithFollowers(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found: ID " + userId).build());

        return user.getFollowers().size();
    }

    @Override
    public long getFollowingCount(Long userId) {
        log.info("UsersFollowService: Retrieving following count for user ID {}", userId);

        User user = userRepository.findByIdAndDeletedFalseWithFollowing(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found: ID " + userId).build());

        return user.getFollowing().size();
    }

}
