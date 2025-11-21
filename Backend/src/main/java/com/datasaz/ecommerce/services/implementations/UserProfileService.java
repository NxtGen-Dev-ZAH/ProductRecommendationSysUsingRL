package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.request.UserPrivacySettingsRequest;
import com.datasaz.ecommerce.models.request.UserProfileRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.UserFollowersCountResponse;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IUserProfileService;
import com.datasaz.ecommerce.services.interfaces.IUserProfileVisitService;
import com.datasaz.ecommerce.utilities.FileStorageService;
import com.datasaz.ecommerce.utilities.Utility;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserProfileService implements IUserProfileService, IUserProfileVisitService {

    private final UserRepository userRepository;
    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final ProductRepository productRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final RolesRepository rolesRepository;
    private final UserCustomFieldsRepository customFieldsRepository;
    private final FileStorageService fileStorageService;
    private final GroupConfig groupConfig;
    private final AuditLogService auditLogService;
    private final IEmailService emailService;
    private final Utility utility;
    private final RestTemplate restTemplate;
    private static final Tika tika = new Tika();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public Page<UserSummaryResponse> getFollowers(String email, int page, int size, String viewerEmail) {
        log.info("getFollowers: Fetching followers for user: {}, page: {}, size: {}, viewer: {}", email, page, size, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getFollowersVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view followers of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view followers").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followers = userRepository.findFollowersByUserId(user.getId(), pageable);

        auditLogService.logAction(email, "VIEW_FOLLOWERS", viewerEmail, "Viewed followers by: " + viewerEmail);

        return followers.map(userMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public Page<UserSummaryResponse> getFollowings(String email, int page, int size, String viewerEmail) {
        log.info("getFollowings: Fetching followings for user: {}, page: {}, size: {}, viewer: {}", email, page, size, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getFollowingVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view followings of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view followings").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followings = userRepository.findFollowingByUserId(user.getId(), pageable);

        auditLogService.logAction(email, "VIEW_FOLLOWINGS", viewerEmail, "Viewed followings by: " + viewerEmail);

        return followings.map(userMapper::toSummaryResponse);
    }

    @Override
    @RateLimiter(name = "toggleFollow")
    @Transactional
    public String toggleFollow(String targetEmail, String followerEmail) {
        log.info("toggleFollow: {} attempting to follow/unfollow {}", followerEmail, targetEmail);
        if (targetEmail.equals(followerEmail)) {
            log.error("User {} cannot follow themselves", followerEmail);
            throw BadRequestException.builder().message("Cannot follow yourself").build();
        }

        User targetUser = userRepository.findByEmailAddressAndDeletedFalseWithFollowers(targetEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("Target user not found with email: " + targetEmail).build());
        User follower = userRepository.findByEmailAddressAndDeletedFalseWithFollowing(followerEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("Follower not found with email: " + followerEmail).build());

        //boolean isFollowing = userRepository.isFollowing(followerEmail, targetEmail);
        boolean isFollowing = follower.getFollowing().contains(targetUser);
        String action = isFollowing ? "UNFOLLOWED_USER" : "FOLLOWED_USER";

        if (isFollowing) {
            follower.getFollowing().remove(targetUser);
            targetUser.getFollowers().remove(follower);
        } else {
            follower.getFollowing().add(targetUser);
            targetUser.getFollowers().add(follower);
        }

        userRepository.save(follower);
        userRepository.save(targetUser);
        log.info("{} {} by {}", targetEmail, action, followerEmail);

        auditLogService.logAction(targetEmail, action, followerEmail, "Target user: " + targetEmail);

        return action;
    }

    @Override
    public UserFollowersCountResponse getFollowerCount(String emailAddress) {
        log.info("getFollowerCount: Fetching follower count for email: {}", emailAddress);
        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
            log.error("Invalid email format: {}", emailAddress);
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!userRepository.findByEmailAddressAndDeletedFalse(emailAddress).isPresent()) {
            log.error("User not found with email: {}", emailAddress);
            throw UserNotFoundException.builder().message("User not found with email: " + emailAddress).build();
        }

        Long count = userRepository.countFollowersByEmailAddress(emailAddress);
        return UserFollowersCountResponse.builder().count(count).build();
    }

    @Override
    public UserFollowersCountResponse getFollowingCount(String emailAddress) {
        log.info("getFollowingCount: Fetching following count for email: {}", emailAddress);
        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
            log.error("Invalid email format: {}", emailAddress);
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!userRepository.findByEmailAddressAndDeletedFalse(emailAddress).isPresent()) {
            log.error("User not found with email: {}", emailAddress);
            throw UserNotFoundException.builder().message("User not found with email: " + emailAddress).build();
        }

        Long count = userRepository.countFollowingByEmailAddress(emailAddress);
        return UserFollowersCountResponse.builder().count(count).build();
    }

    @Override
    @Transactional
    public UserProfileResponse getProfile(String email) {
        String viewerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("getProfile: Fetching profile for user: {} by viewer: {}", email, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getProfileVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view profile of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view this profile").build();
        }

        UserProfileResponse response = userMapper.toProfileResponse(user);
        if (!checkVisibility(privacySettings.getFollowersVisibility(), email, viewerEmail)) {
            response.setFollowersCount(0);
            response.setFollowers(null);
        }
        if (!checkVisibility(privacySettings.getFollowingVisibility(), email, viewerEmail)) {
            response.setFollowingCount(0);
            response.setFollowing(null);
        }
        if (!checkVisibility(privacySettings.getFavoritesVisibility(), email, viewerEmail)) {
            response.setFavoriteProducts(null);
        }

        auditLogService.logAction(email, "VIEW_PROFILE", viewerEmail, "Viewed by: " + viewerEmail);

        return response;
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UserProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("updateProfile: Attempting to update profile for user: {}", email);

        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!email.equals(authenticatedEmail)) {
            log.error("updateProfile: Unauthorized attempt to update profile for {} by {}", email, authenticatedEmail);
            throw BadRequestException.builder().message("Unauthorized: Can only update your own profile").build();
        }

        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        } else {
            log.warn("updateProfile: FirstName not provided or empty for user: {}", email);
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        } else {
            log.warn("updateProfile: LastName not provided or empty for user: {}", email);
        }

        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                log.error("updateProfile: Date of birth cannot be in the future: {}", request.getDateOfBirth());
                throw BadRequestException.builder().message("Date of birth cannot be in the future").build();
            }
            if (Period.between(request.getDateOfBirth(), LocalDate.now()).getYears() < 13) {
                log.error("updateProfile: User must be at least 13 years old: {}", request.getDateOfBirth());
                throw BadRequestException.builder().message("User must be at least 13 years old").build();
            }
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber());
        } else {
            log.warn("updateProfile: PhoneNumber not provided or empty for user: {}", email);
        }

        String location = fetchLocationFromIp();
        if (location != null) {
            user.setLocation(location);
            log.info("updateProfile: Set location to {} for user: {}", location, email);
        } else {
            log.warn("updateProfile: Could not determine location for user: {}", email);
        }

        userRepository.save(user);

        LocalDateTime timestamp = LocalDateTime.now();
        auditLogService.logAction(email, "UPDATE_PROFILE", "User profile updated for: " + email);

        try {
            emailService.sendProfileUpdateNotification(email, LocalDateTime.now());
        } catch (Exception e) {
            log.error("updateProfile: Failed to send profile update notification to {}: {}", email, e.getMessage());
        }

        refreshTokenRepository.deleteByUserEmail(email);
        log.info("updateProfile: Revoked all refresh tokens for user: {}", email);

        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updatePrivacySettings(UserPrivacySettingsRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("updatePrivacySettings: Updating privacy settings for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (request.getProfileVisibility() != null) {
            privacySettings.setProfileVisibility(UserPrivacySettings.Visibility.valueOf(request.getProfileVisibility()));
        }
        if (request.getFollowersVisibility() != null) {
            privacySettings.setFollowersVisibility(UserPrivacySettings.Visibility.valueOf(request.getFollowersVisibility()));
        }
        if (request.getFollowingVisibility() != null) {
            privacySettings.setFollowingVisibility(UserPrivacySettings.Visibility.valueOf(request.getFollowingVisibility()));
        }
        if (request.getFavoritesVisibility() != null) {
            privacySettings.setFavoritesVisibility(UserPrivacySettings.Visibility.valueOf(request.getFavoritesVisibility()));
        }

        userRepository.save(user);
        log.info("Privacy settings updated for user: {}", email);

        auditLogService.logAction(email, "UPDATE_PRIVACY_SETTINGS", "Updated settings: " + request.toString());

        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserPrivacySettings getPrivacySettings() {
        log.info("getPrivacySettings: Fetching privacy settings");
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            privacySettings = UserPrivacySettings.builder()
                    .user(user)
                    .profileVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .followersVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .followingVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .favoritesVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .build();
            privacySettingsRepository.save(privacySettings);
        }

        return privacySettings;
    }

    private boolean checkVisibility(UserPrivacySettings.Visibility visibility, String targetEmail, String viewerEmail) {
        if (visibility == UserPrivacySettings.Visibility.PUBLIC) {
            return true;
        }
        if (visibility == UserPrivacySettings.Visibility.PRIVATE) {
            return targetEmail.equals(viewerEmail);
        }
        if (visibility == UserPrivacySettings.Visibility.FOLLOWERS) {
            User targetUser = userRepository.findByEmailAddressAndDeletedFalse(targetEmail)
                    .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + targetEmail).build());
            Optional<User> viewerOpt = userRepository.findByEmailAddressAndDeletedFalse(viewerEmail);
            if (viewerOpt.isEmpty()) {
                return false;
            }
            User viewer = viewerOpt.get();
            return targetUser.getFollowers().contains(viewer) || targetEmail.equals(viewerEmail);
        }
        return false;
    }

    @Override
    @Transactional
    public Page<ProductResponse> getFavoriteProducts(String email, int page, int size) {
        String viewerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("getFavoriteProducts: Fetching favorite products for user: {}, page: {}, size: {}, viewer: {}", email, page, size, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getFavoritesVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view favorite products of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view favorite products").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> favoriteProducts = productRepository.findFavoriteProductsByUserId(user.getId(), pageable);

        auditLogService.logAction(email, "VIEW_FAVORITE_PRODUCTS", viewerEmail, "Viewed favorite products by: " + viewerEmail);

        return favoriteProducts.map(productMapper::toResponse);
    }

    @Override
    @RateLimiter(name = "toggleFavoriteProduct")
    @Transactional
    public String toggleFavoriteProduct(Long productId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("toggleFavoriteProduct: {} attempting to favorite/unfavorite product {}", email, productId);
        User user = userRepository.findByEmailAddressAndDeletedFalseWithFavoriteProducts(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalseWithFavouriteUsers(productId)
                .orElseThrow(() -> ProductNotFoundException.builder().message("Product not found with id: " + productId).build());

        boolean isFavorited = user.getFavoriteProducts().contains(product);
        String action = isFavorited ? "UNFAVORITED_PRODUCT" : "FAVORITED_PRODUCT";

        if (isFavorited) {
            user.getFavoriteProducts().remove(product);
            product.getUsersFavourite().remove(user);
        } else {
            user.getFavoriteProducts().add(product);
            product.getUsersFavourite().add(user);
        }

        userRepository.save(user);
        productRepository.save(product);
        log.info("{} {} by {}", productId, action, email);

        auditLogService.logAction(email, action, "Product ID: " + productId);

        return action;
    }

    @Override
    @RateLimiter(name = "pictureUpload")
    @Transactional
    public String uploadProfilePicture(MultipartFile image, String email) {
        log.info("uploadProfilePicture: Uploading profile picture for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        validateImage(image);

        String fileName = generateFileName(image.getOriginalFilename());
        String extension = getFileExtension(image.getOriginalFilename());

        try {
            byte[] imageBytes = image.getBytes();
            if (groupConfig.imageStorageMode.equals("database")) {
                byte[] resizedImage = resizeImage(imageBytes, extension, false, groupConfig.resizeWidth, groupConfig.resizeHeight);
                if (resizedImage.length == 0) {
                    log.error("Resized image is empty for file: {}", fileName);
                    throw BadRequestException.builder().message("Failed to resize image: empty result").build();
                }
                if (resizedImage.length > groupConfig.MAX_FILE_SIZE) {
                    log.error("Image size {} bytes exceeds limit {} MB", resizedImage.length, groupConfig.maxFileSizeMb);
                    throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
                }

                user.setImageContent(resizedImage);
                user.setImageContentType(image.getContentType());
                user.setImageFileExtension(extension);
                user.setProfilePictureUrl(null);
                auditLogService.logAction(email, "UPLOAD_PROFILE_PICTURE", "Uploaded image to database for user: " + email);
            } else {
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "profile-pictures");
                if (!fileStorageService.exists(uploadDir)) {
                    fileStorageService.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                try (var inputStream = image.getInputStream()) {
                    fileStorageService.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                String profilePictureUrl = "/Uploads/profile-pictures/" + fileName;
                user.setProfilePictureUrl(profilePictureUrl);
                user.setImageContent(null);
                user.setImageContentType(null);
                user.setImageFileExtension(null);
                auditLogService.logAction(email, "UPLOAD_PROFILE_PICTURE", "Uploaded image to file system for user: " + email + " at: " + filePath);
            }

            if (groupConfig.imageStorageMode.equals("file system") && user.getProfilePictureUrl() != null) {
                deleteOldImage(user.getProfilePictureUrl());
            }

            userRepository.save(user);
            return user.getProfilePictureUrl();
        } catch (IOException e) {
            log.error("Failed to upload profile picture for user {}: {}", email, e.getMessage());
            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
        }
    }

    @Transactional
    public String uploadProfilePicture(UserProfileRequest request, String email) {
        log.info("uploadProfilePicture: Uploading profile picture (Base64) for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        if (request.getProfilePictureBase64() == null || request.getProfilePictureBase64().isEmpty()) {
            log.error("Image content is required for user: {}", email);
            throw BadRequestException.builder().message("Image content is required").build();
        }

        validateImageRequest(request);

        String fileName = generateFileName(user.getEmailAddress() + ".jpg");
        String extension = getFileExtension(user.getEmailAddress() + ".jpg");

        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.getProfilePictureBase64());
            if (groupConfig.imageStorageMode.equals("database")) {
                byte[] resizedImage = resizeImage(imageBytes, extension, false, groupConfig.resizeWidth, groupConfig.resizeHeight);
                if (resizedImage.length == 0) {
                    log.error("Resized image is empty for file: {}", fileName);
                    throw BadRequestException.builder().message("Failed to resize image: empty result").build();
                }
                if (resizedImage.length > groupConfig.MAX_FILE_SIZE) {
                    log.error("Image size {} bytes exceeds limit {} MB", resizedImage.length, groupConfig.maxFileSizeMb);
                    throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
                }

                user.setImageContent(resizedImage);
                user.setImageContentType("image/jpeg");
                user.setImageFileExtension(extension);
                user.setProfilePictureUrl(null);
                auditLogService.logAction(email, "UPLOAD_PROFILE_PICTURE", "Uploaded image to database for user: " + email);
            } else {
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "profile-pictures");
                if (!fileStorageService.exists(uploadDir)) {
                    fileStorageService.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                fileStorageService.write(filePath, imageBytes);
                String profilePictureUrl = "/Uploads/profile-pictures/" + fileName;
                user.setProfilePictureUrl(profilePictureUrl);
                user.setImageContent(null);
                user.setImageContentType(null);
                user.setImageFileExtension(null);
                auditLogService.logAction(email, "UPLOAD_PROFILE_PICTURE", "Uploaded image to file system for user: " + email + " at: " + filePath);
            }

            if (groupConfig.imageStorageMode.equals("file system") && user.getProfilePictureUrl() != null) {
                deleteOldImage(user.getProfilePictureUrl());
            }

            userRepository.save(user);
            return user.getProfilePictureUrl();
        } catch (IOException e) {
            log.error("Failed to upload profile picture for user {}: {}", email, e.getMessage());
            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BadRequestException.builder().message("File is empty or null").build();
        }
        if (file.getSize() > groupConfig.MAX_FILE_SIZE) {
            log.error("File size {} bytes exceeds limit {} MB ({} bytes)", file.getSize(), groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("File size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }
        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream());
            if (!groupConfig.getALLOWED_IMAGE_TYPES().contains(mimeType)) {
                log.error("Unsupported file type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported file type: " + mimeType).build();
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes())) {
                BufferedImage img = ImageIO.read(bais);
                if (img == null) {
                    log.error("Invalid image content: cannot read image for {}", file.getOriginalFilename());
                    throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
                }
                if (img.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_RGB) {
                    log.warn("Image has non-RGB colorspace: {} for {}", img.getColorModel().getColorSpace().getType(), file.getOriginalFilename());
                }
            }
        } catch (IOException e) {
            log.error("Error validating image: {}", e.getMessage());
            throw BadRequestException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private void validateImageRequest(UserProfileRequest request) {
        if (request.getProfilePictureBase64() == null || request.getProfilePictureBase64().isBlank()) {
            throw BadRequestException.builder().message("Image request is not complete").build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(request.getProfilePictureBase64());
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 content for image");
            throw BadRequestException.builder().message("Invalid Base64 content for image").build();
        }

        if (imageBytes.length > groupConfig.MAX_FILE_SIZE) {
            log.error("Image size {} bytes exceeds limit {} MB ({} bytes)", imageBytes.length, groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }

        String mimeType;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            mimeType = tika.detect(bais);
            if (!groupConfig.getALLOWED_IMAGE_TYPES().contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
            bais.reset();
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Invalid image content: cannot read image");
                throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
            }
            if (img.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_RGB) {
                log.warn("Image has non-RGB colorspace: {}", img.getColorModel().getColorSpace().getType());
            }
        } catch (IOException e) {
            log.error("Error detecting image type or validating image: {}", e.getMessage());
            throw BadRequestException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private byte[] resizeImage(byte[] originalImage, String extension, boolean forceJpeg, int targetWidth, int targetHeight) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImage)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Failed to read image: null BufferedImage");
                throw new IOException("Cannot read image: invalid or corrupt image data");
            }
            log.debug("Original image: width={}, height={}", img.getWidth(), img.getHeight());

            double aspectRatio = (double) img.getWidth() / img.getHeight();
            if (aspectRatio > 1) {
                targetHeight = (int) (targetWidth / aspectRatio);
            } else {
                targetWidth = (int) (targetHeight * aspectRatio);
            }
            BufferedImage resized = org.imgscalr.Scalr.resize(img, org.imgscalr.Scalr.Method.ULTRA_QUALITY, org.imgscalr.Scalr.Mode.FIT_EXACT, targetWidth, targetHeight);
            log.debug("Resized image: width={}, height={}", resized.getWidth(), resized.getHeight());

            BufferedImage rgbImage = resized;
            String outputFormat = forceJpeg ? "jpg" : (extension != null && !extension.isEmpty() ? extension.toLowerCase() : "jpg");
            String formatName;
            switch (outputFormat) {
                case "jpg":
                case "jpeg":
                    formatName = "jpeg";
                    rgbImage = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImage.getGraphics().drawImage(resized, 0, 0, null);
                    break;
                case "png":
                    formatName = "png";
                    break;
                case "gif":
                    formatName = "gif";
                    break;
                case "bmp":
                    formatName = "bmp";
                    break;
                case "tiff":
                    formatName = "tiff";
                    break;
                case "wbmp":
                    formatName = "wbmp";
                    break;
                case "webp":
                    formatName = "webp";
                    break;
                default:
                    log.warn("Unsupported output format: {}, defaulting to jpeg", outputFormat);
                    formatName = "jpeg";
                    outputFormat = "jpg";
                    rgbImage = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImage.getGraphics().drawImage(resized, 0, 0, null);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (formatName.equals("png") || formatName.equals("gif") || formatName.equals("bmp") || formatName.equals("tiff") || formatName.equals("wbmp")) {
                    ImageIO.write(rgbImage, formatName, baos);
                } else {
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
                    if (!writers.hasNext()) {
                        log.error("No writer available for format: {}", formatName);
                        throw new IOException("No writer available for format: " + formatName);
                    }
                    ImageWriter writer = writers.next();
                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                        writer.setOutput(ios);
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (formatName.equals("jpeg") || formatName.equals("webp")) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(groupConfig.imageQuality);
                        }
                        writer.write(null, new javax.imageio.IIOImage(rgbImage, null, null), param);
                    } finally {
                        writer.dispose();
                    }
                }
                byte[] result = baos.toByteArray();
                log.debug("Resized image size: {} bytes, format: {}", result.length, formatName);
                if (result.length == 0) {
                    log.error("Resized image is empty after writing to ByteArrayOutputStream");
                    throw new IOException("Resized image is empty");
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Error resizing image: {}", e.getMessage(), e);
            throw new IOException("Failed to resize image: " + e.getMessage(), e);
        }
    }

    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            try {
                fileStorageService.deleteIfExists(oldFilePath);
                log.info("Deleted old profile image: {}", oldImageUrl);
            } catch (IOException e) {
                log.warn("Failed to delete old profile image {}: {}", oldImageUrl, e.getMessage());
            }
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "jpg";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "jpg" : fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    private String fetchLocationFromIp() {
        try {
            String url = "http://ip-api.com/json/?fields=city,region,country";
            ResponseEntity<GeoLocationResponse> response = restTemplate.getForEntity(url, GeoLocationResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GeoLocationResponse geo = response.getBody();
                return String.format("%s, %s, %s", geo.getCity(), geo.getRegion(), geo.getCountry());
            }
        } catch (Exception e) {
            log.error("fetchLocationFromIp: Failed to fetch location: {}", e.getMessage());
        }
        return null;
    }

    private static class GeoLocationResponse {
        private String city;
        private String region;
        private String country;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }
}



/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ProductNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.ProductMapper;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.request.UserPrivacySettingsRequest;
import com.datasaz.ecommerce.models.request.UserProfileRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.UserFollowersCountResponse;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IUserProfileService;
import com.datasaz.ecommerce.services.interfaces.IUserProfileVisitService;
import com.datasaz.ecommerce.utilities.Utility;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserProfileService implements IUserProfileService, IUserProfileVisitService {

    private final UserRepository userRepository;
    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final ProductRepository productRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    private final RolesRepository rolesRepository;
    private final UserCustomFieldsRepository customFieldsRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Utility utility;

    //private UserDto userDto = new UserDto();

    private final GroupConfig groupConfig;

    private final AuditLogService auditLogService;
    private final IEmailService emailService;


    private static final Tika tika = new Tika();
    private final RestTemplate restTemplate;


    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");


    // private static final String DEFAULT_PROFILE_PICTURE = "/uploads/profile-pictures/default-profile-picture.jpg";


//    @Override
//    public Page<UserSummaryResponse> getFollowers(String emailAddress, int page, int size) {
//        log.info("getFollowers: Fetching followers for email: {}, page: {}, size: {}", emailAddress, page, size);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        checkVisibility(user, user.getPrivacySettings().getFollowersVisibility(), requesterEmail, "followers");
//
//        Pageable pageable = PageRequest.of(page, size);
//        return userRepository.findFollowersByEmailAddress(emailAddress, pageable)
//                .map(userMapper::toSummaryResponse);
//    }
//
//    @Override
//    public Page<UserSummaryResponse> getFollowing(String emailAddress, int page, int size) {
//        log.info("getFollowing: Fetching following for email: {}, page: {}, size: {}", emailAddress, page, size);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        checkVisibility(user, user.getPrivacySettings().getFollowingVisibility(), requesterEmail, "following");
//
//        Pageable pageable = PageRequest.of(page, size);
//
//        return userRepository.findFollowingByEmailAddress(emailAddress, pageable)
//                .map(userMapper::toSummaryResponse);
//    }

    @Override
    @Transactional
    public Page<UserSummaryResponse> getFollowers(String email, int page, int size, String viewerEmail) {
        log.info("getFollowers: Fetching followers for user: {}, page: {}, size: {}, viewer: {}", email, page, size, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getFollowersVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view followers of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view followers").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followers = userRepository.findFollowersByUserId(user.getId(), pageable);

        auditLogService.logAction(email, "VIEW_FOLLOWERS", viewerEmail, "Viewed followers by: " + viewerEmail);

        return followers.map(userMapper::toSummaryResponse);
    }


//    @Override
//    public Page<UserSummaryResponse> getFollowers(String emailAddress, int page, int size) {
//        log.info("getFollowers: Fetching followers for email: {}, page: {}, size: {}", emailAddress, page, size);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        // checkVisibility(user, getPrivacySettings(userMapper.toDto(user)).getFollowersVisibility(), requesterEmail, "followers");
//
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Object[]> followersPage = userRepository.findFollowersSummaryByEmailAddress(emailAddress, pageable);
//
//        List<UserSummaryResponse> followers = followersPage.getContent().stream()
//                .map(row -> UserSummaryResponse.builder()
//                        .emailAddress((String) row[0])
//                        .displayName(((String) row[1] + " " + (String) row[2]).trim())
//                        .profilePictureUrl((String) row[3])
//                        .build())
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(followers, pageable, followersPage.getTotalElements());
//    }


    @Override
    @Transactional
    public Page<UserSummaryResponse> getFollowings(String email, int page, int size, String viewerEmail) {
        log.info("getFollowings: Fetching followings for user: {}, page: {}, size: {}, viewer: {}", email, page, size, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getFollowingVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view followings of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view followings").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followings = userRepository.findFollowingByUserId(user.getId(), pageable);

        auditLogService.logAction(email, "VIEW_FOLLOWINGS", viewerEmail, "Viewed followings by: " + viewerEmail);

        return followings.map(userMapper::toSummaryResponse);
    }

//    @Override
//    public Page<UserSummaryResponse> getFollowings(String emailAddress, int page, int size) {
//        log.info("getFollowing: Fetching following for email: {}, page: {}, size: {}", emailAddress, page, size);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        //checkVisibility(user, getPrivacySettings(userMapper.toDto(user)).getFollowingVisibility(), requesterEmail, "following");
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Object[]> followingPage = userRepository.findFollowingSummaryByEmailAddress(emailAddress, pageable);
//
//        List<UserSummaryResponse> following = followingPage.getContent().stream()
//                .map(row -> UserSummaryResponse.builder()
//                        .emailAddress((String) row[0])
//                        .displayName(((String) row[1] + " " + (String) row[2]).trim())
//                        .profilePictureUrl((String) row[3])
//                        .build())
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(following, pageable, followingPage.getTotalElements());
//    }

    @Override
    @RateLimiter(name = "toggleFollow")
    public String toggleFollow(String targetEmail, String followerEmail) {
        log.info("toggleFollow: {} attempting to follow/unfollow {}", followerEmail, targetEmail);
        if (targetEmail.equals(followerEmail)) {
            log.error("User {} cannot follow themselves", followerEmail);
            throw BadRequestException.builder().message("Cannot follow yourself").build();
        }

        User targetUser = userRepository.findByEmailAddressAndDeletedFalse(targetEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("Target user not found with email: " + targetEmail).build());
        User follower = userRepository.findByEmailAddressAndDeletedFalse(followerEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("Follower not found with email: " + followerEmail).build());

        boolean isFollowing = follower.getFollowing().contains(targetUser);
        String action = isFollowing ? "UNFOLLOWED_USER" : "FOLLOWED_USER";

        if (isFollowing) {
            follower.getFollowing().remove(targetUser);
            targetUser.getFollowers().remove(follower);
        } else {
            follower.getFollowing().add(targetUser);
            targetUser.getFollowers().add(follower);
        }

        userRepository.save(follower);
        userRepository.save(targetUser);
        log.info("{} {} by {}", targetEmail, action, followerEmail);

        auditLogService.logAction(targetEmail, action, followerEmail, "Target user: " + targetEmail);

        return action;
    }

//    @Override
//    public UserDto toggleFollow(String emailAddress) {
//        log.info("toggleFollow: Attempting to toggle follow for user with email: {}", emailAddress);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//
//        String followerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (followerEmail.equals(emailAddress)) {
//            log.error("User {} cannot follow themselves", followerEmail);
//            throw new IllegalArgumentException("Cannot follow yourself");
//        }
//
//        User follower = userRepository.findByEmailAddressAndDeletedFalse(followerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder().message("Follower not found with email: " + followerEmail).build());
//        User following = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        boolean isFollowing = userRepository.existsFollowRelationship(followerEmail, emailAddress);
//        String action = isFollowing ? "UNFOLLOW_USER" : "FOLLOW_USER";
//
//        if (isFollowing) {
//            follower.getFollowing().remove(following);
//            following.getFollowers().remove(follower);
//        } else {
//            follower.getFollowing().add(following);
//            following.getFollowers().add(follower);
//        }
//
//        userRepository.save(follower);
//        userRepository.save(following);
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(emailAddress)
//                .action(action)
//                .performedBy(followerEmail)
//                .timestamp(LocalDateTime.now())
//                .build());
//
//        return userMapper.toDto(following);
//    }


//    @Override
//    public UserDto followUser(String emailAddress) {
//        log.info("followUser: Attempting to follow user with email: {}", emailAddress);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//
//        String followerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (followerEmail.equals(emailAddress)) {
//            log.error("User {} cannot follow themselves", followerEmail);
//            throw new IllegalArgumentException("Cannot follow yourself");
//        }
//
//        User follower = userRepository.findByEmailAddressAndDeletedFalse(followerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder().message("Follower not found with email: " + followerEmail).build());
//        User following = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        if (userRepository.existsFollowRelationship(followerEmail, emailAddress)) {
//            log.warn("User {} already follows {}", followerEmail, emailAddress);
//            throw new IllegalStateException("Already following this user");
//        }
//
//        follower.getFollowing().add(following);
//        following.getFollowers().add(follower);
//        userRepository.save(follower);
//        userRepository.save(following);
//
//        // Log audit event
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(emailAddress)
//                .action("FOLLOW_USER")
//                .performedBy(followerEmail)
//                .timestamp(LocalDateTime.now())
//                .build());
//
//        return userMapper.toDto(following);
//    }
//
//    @Override
//    public UserDto unfollowUser(String emailAddress) {
//        log.info("unfollowUser: Attempting to unfollow user with email: {}", emailAddress);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            log.error("Invalid email format: {}", emailAddress);
//            throw new IllegalArgumentException("Invalid email format");
//        }
//
//        String followerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (followerEmail.equals(emailAddress)) {
//            log.error("User {} cannot unfollow themselves", followerEmail);
//            throw new IllegalArgumentException("Cannot unfollow yourself");
//        }
//
//        User follower = userRepository.findByEmailAddressAndDeletedFalse(followerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder().message("Follower not found with email: " + followerEmail).build());
//        User following = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        if (!userRepository.existsFollowRelationship(followerEmail, emailAddress)) {
//            log.warn("User {} does not follow {}", followerEmail, emailAddress);
//            throw new IllegalStateException("Not following this user");
//        }
//
//        follower.getFollowing().remove(following);
//        following.getFollowers().remove(follower);
//        userRepository.save(follower);
//        userRepository.save(following);
//
//        // Log audit event
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(emailAddress)
//                .action("UNFOLLOW_USER")
//                .performedBy(followerEmail)
//                .timestamp(LocalDateTime.now())
//                .build());
//
//        return userMapper.toDto(following);
//    }

    @Override
    public UserFollowersCountResponse getFollowerCount(String emailAddress) {
        log.info("getFollowerCount: Fetching follower count for email: {}", emailAddress);
        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
            log.error("Invalid email format: {}", emailAddress);
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!userRepository.findByEmailAddressAndDeletedFalse(emailAddress).isPresent()) {
            log.error("User not found with email: {}", emailAddress);
            throw UserNotFoundException.builder().message("User not found with email: " + emailAddress).build();
        }

        Long count = userRepository.countFollowersByEmailAddress(emailAddress);
        return UserFollowersCountResponse.builder().count(count).build();
    }

    @Override
    public UserFollowersCountResponse getFollowingCount(String emailAddress) {
        log.info("getFollowingCount: Fetching following count for email: {}", emailAddress);
        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
            log.error("Invalid email format: {}", emailAddress);
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!userRepository.findByEmailAddressAndDeletedFalse(emailAddress).isPresent()) {
            log.error("User not found with email: {}", emailAddress);
            throw UserNotFoundException.builder().message("User not found with email: " + emailAddress).build();
        }

        Long count = userRepository.countFollowingByEmailAddress(emailAddress);
        return UserFollowersCountResponse.builder().count(count).build();
    }

//    @Override
//    public UserDto getProfile(String emailAddress) {
//        log.info("getProfile: Fetching profile for email: {}", emailAddress);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            throw new IllegalArgumentException("Invalid email format");
//        }
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        checkVisibility(user, getPrivacySettings(userMapper.toDto(user)).getProfileVisibility(), requesterEmail, "profile");
//
//        return userMapper.toDto(user);
//    }

    @Override
    @Transactional
    public UserProfileResponse getProfile(String email) {
        String viewerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("getProfile: Fetching profile for user: {} by viewer: {}", email, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        // Check visibility permissions
        if (!checkVisibility(privacySettings.getProfileVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view profile of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view this profile").build();
        }

        UserProfileResponse response = userMapper.toProfileResponse(user);
        if (!checkVisibility(privacySettings.getFollowersVisibility(), email, viewerEmail)) {
            response.setFollowersCount(0);
            response.setFollowers(null);
        }
        if (!checkVisibility(privacySettings.getFollowingVisibility(), email, viewerEmail)) {
            response.setFollowingCount(0);
            response.setFollowing(null);
        }
        if (!checkVisibility(privacySettings.getFavoritesVisibility(), email, viewerEmail)) {
            response.setFavoriteProducts(null);
        }

        auditLogService.logAction(email, "VIEW_PROFILE", viewerEmail, "Viewed by: " + viewerEmail);

        return response;
    }

//    @Override
//    public UserProfileResponse updateProfile(UserProfileRequest request) {
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//
//        log.info("updateProfile: Updating profile for user: {}", email);
//
//        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
//
//        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
//        if (request.getLastName() != null) user.setLastName(request.getLastName());
//        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
//        if (request.getLocation() != null) user.setLocation(request.getLocation());
//
//        userRepository.save(user);
//        log.info("Profile updated for user: {}", email);
//
//        auditLogService.logAction(email, "UPDATE_PROFILE", "Updated fields: " + request.toString());
//
//        return userMapper.toProfileResponse(user);
//    }


    @Override
    @Transactional
    public UserProfileResponse updateProfile(UserProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("updateProfile: Attempting to update profile for user: {}", email);

        // Verify authenticated user
        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!email.equals(authenticatedEmail)) {
            log.error("updateProfile: Unauthorized attempt to update profile for {} by {}", email, authenticatedEmail);
            throw BadRequestException.builder().message("Unauthorized: Can only update your own profile").build();
        }

        // Find user
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        // Update fields if provided
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        } else {
            log.warn("updateProfile: FirstName not provided or empty for user: {}", email);
        }
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        } else {
            log.warn("updateProfile: LastName not provided or empty for user: {}", email);
        }

        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                log.error("updateProfile: Date of birth cannot be in the future: {}", request.getDateOfBirth());
                throw BadRequestException.builder().message("Date of birth cannot be in the future").build();
            }
            if (Period.between(request.getDateOfBirth(), LocalDate.now()).getYears() < 13) {
                log.error("updateProfile: User must be at least 13 years old: {}", request.getDateOfBirth());
                throw BadRequestException.builder().message("User must be at least 13 years old").build();
            }
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber());
        } else {
            log.warn("updateProfile: PhoneNumber not provided or empty for user: {}", email);
        }

        // Automatically detect location based on IP
        String location = fetchLocationFromIp();
        if (location != null) {
            user.setLocation(location);
            log.info("updateProfile: Set location to {} for user: {}", location, email);
        } else {
            log.warn("updateProfile: Could not determine location for user: {}", email);
        }

        // Save updated user
        userRepository.save(user);

        // Log audit event

        LocalDateTime timestamp = LocalDateTime.now();
        auditLogService.logAction(email, "UPDATE_PROFILE", "User profile updated for: " + email);

        // Send notification
        try {
            emailService.sendProfileUpdateNotification(email, LocalDateTime.now());
        } catch (Exception e) {
            log.error("updateProfile: Failed to send profile update notification to {}: {}", email, e.getMessage());
        }

        // Revoke all refresh tokens
        refreshTokenRepository.deleteByUserEmail(email);
        log.info("updateProfile: Revoked all refresh tokens for user: {}", email);

        return userMapper.toProfileResponse(user);
    }

    private String fetchLocationFromIp() {
        try {
            String url = "http://ip-api.com/json/?fields=city,region,country";
            ResponseEntity<GeoLocationResponse> response = restTemplate.getForEntity(url, GeoLocationResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GeoLocationResponse geo = response.getBody();
                return String.format("%s, %s, %s", geo.getCity(), geo.getRegion(), geo.getCountry());
            }
        } catch (Exception e) {
            log.error("fetchLocationFromIp: Failed to fetch location: {}", e.getMessage());
        }
        return null;
    }

    // Inner class for geolocation response
    private static class GeoLocationResponse {
        private String city;
        private String region;
        private String country;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }


    @Override
    @Transactional
    public UserProfileResponse updatePrivacySettings(UserPrivacySettingsRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("updatePrivacySettings: Updating privacy settings for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (request.getProfileVisibility() != null) {
            privacySettings.setProfileVisibility(UserPrivacySettings.Visibility.valueOf(request.getProfileVisibility()));
        }
        if (request.getFollowersVisibility() != null) {
            privacySettings.setFollowersVisibility(UserPrivacySettings.Visibility.valueOf(request.getFollowersVisibility()));
        }
        if (request.getFollowingVisibility() != null) {
            privacySettings.setFollowingVisibility(UserPrivacySettings.Visibility.valueOf(request.getFollowingVisibility()));
        }
        if (request.getFavoritesVisibility() != null) {
            privacySettings.setFavoritesVisibility(UserPrivacySettings.Visibility.valueOf(request.getFavoritesVisibility()));
        }

        userRepository.save(user); // Cascade saves UserPrivacySettings
        log.info("Privacy settings updated for user: {}", email);

        auditLogService.logAction(email, "UPDATE_PRIVACY_SETTINGS", "Updated settings: " + request.toString());

        return userMapper.toProfileResponse(user);
    }


    @Override
    public UserPrivacySettings getPrivacySettings() {
        log.info("getPrivacySettings: Fetching privacy settings");
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();

//        UserPrivacySettings settings = privacySettingsRepository.findByUserId(user.getId())
//                .orElseThrow(() -> {
//                    log.error("settings not found for user : {}, Id : {}", user.getEmailAddress(), user.getId());
//                    return ResourceNotFoundException.builder().message(ExceptionMessages.RESOURCE_NOT_FOUND + " settings not Found for user:"+user.getEmailAddress()).build();
//                });

        if (privacySettings == null) {
            privacySettings = UserPrivacySettings.builder()
                    .user(user)
                    .profileVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .followersVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .followingVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .favoritesVisibility(UserPrivacySettings.Visibility.PUBLIC)
                    .build();
//            user.setPrivacySettings(settings);
//            userRepository.save(user);
            privacySettingsRepository.save(privacySettings);
        }

        return privacySettings;
    }

    private boolean checkVisibility(UserPrivacySettings.Visibility visibility, String targetEmail, String viewerEmail) {
        if (visibility == UserPrivacySettings.Visibility.PUBLIC) {
            return true;
        }
        if (visibility == UserPrivacySettings.Visibility.PRIVATE) {
            return targetEmail.equals(viewerEmail);
        }
        if (visibility == UserPrivacySettings.Visibility.FOLLOWERS) {
            User targetUser = userRepository.findByEmailAddressAndDeletedFalse(targetEmail)
                    .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + targetEmail).build());
            Optional<User> viewerOpt = userRepository.findByEmailAddressAndDeletedFalse(viewerEmail);
            if (viewerOpt.isEmpty()) {
                return false; // Anonymous or unregistered viewer -> no access
            }
            User viewer = viewerOpt.get();
            return targetUser.getFollowers().contains(viewer) || targetEmail.equals(viewerEmail);
        }
        return false;
    }
//    private void checkVisibility(User user, UserPrivacySettings.Visibility visibility, String requesterEmail, String resource) {
//        if (visibility == null) {
//            visibility = UserPrivacySettings.Visibility.PUBLIC; // Default if visibility is null
//        }
//
//        if (visibility == UserPrivacySettings.Visibility.PRIVATE && !user.getEmailAddress().equals(requesterEmail)) {
//            throw new SecurityException("Access denied to " + (resource != null ? resource : "profile"));
//        }
//
//        if (visibility == UserPrivacySettings.Visibility.FOLLOWERS && !user.getEmailAddress().equals(requesterEmail)) {
//            boolean isFollower = userRepository.existsFollowRelationship(requesterEmail, user.getEmailAddress());
//            if (!isFollower) {
//                throw new SecurityException("Access denied to " + (resource != null ? resource : "profile") + "; must be a follower");
//            }
//        }
//    }

    @Override
    @Transactional
    public Page<ProductResponse> getFavoriteProducts(String email, int page, int size) {
        String viewerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("getFavoriteProducts: Fetching favorite products for user: {}, page: {}, size: {}, viewer: {}", email, page, size, viewerEmail);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        UserPrivacySettings privacySettings = user.getPrivacySettings();
        if (privacySettings == null) {
            log.error("Privacy settings not found for user: {}", email);
            throw BadRequestException.builder().message("Privacy settings not initialized for user: " + email).build();
        }

        if (!checkVisibility(privacySettings.getFavoritesVisibility(), email, viewerEmail)) {
            log.error("Viewer {} not authorized to view favorite products of {}", viewerEmail, email);
            throw UnauthorizedException.builder().message("Not authorized to view favorite products").build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> favoriteProducts = productRepository.findFavoriteProductsByUserId(user.getId(), pageable);

        auditLogService.logAction(email, "VIEW_FAVORITE_PRODUCTS", viewerEmail, "Viewed favorite products by: " + viewerEmail);

        return favoriteProducts.map(productMapper::toResponse);
    }

//    @Override
//    public Page<ProductResponse> getFavoriteProducts(String emailAddress, int page, int size) {
//        log.info("getFavoriteProducts: Fetching favorite products for email: {}, page: {}, size: {}", emailAddress, page, size);
//        if (!EMAIL_PATTERN.matcher(emailAddress).matches()) {
//            throw new IllegalArgumentException("Invalid email format");
//        }
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        String requesterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        //checkVisibility(user, getPrivacySettings(userMapper.toDto(user)).getFavoritesVisibility(), requesterEmail, "favorite products");
//
//        Pageable pageable = PageRequest.of(page, size);
//        return userRepository.findFavoriteProductsByEmailAddress(emailAddress, pageable)
//                .map(productMapper::toResponse);
//    }

    @Override
    @RateLimiter(name = "toggleFavoriteProduct")
    public String toggleFavoriteProduct(Long productId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("toggleFavoriteProduct: {} attempting to favorite/unfavorite product {}", email, productId);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> ProductNotFoundException.builder().message("Product not found with id: " + productId).build());

        boolean isFavorited = user.getFavoriteProducts().contains(product);
        String action = isFavorited ? "UNFAVORITED_PRODUCT" : "FAVORITED_PRODUCT";

        if (isFavorited) {
            user.getFavoriteProducts().remove(product);
            product.getUsersFavourite().remove(user);
        } else {
            user.getFavoriteProducts().add(product);
            product.getUsersFavourite().add(user);
        }

        userRepository.save(user);
        productRepository.save(product);
        log.info("{} {} by {}", productId, action, email);

        auditLogService.logAction(email, action, "Product ID: " + productId);

        //return userMapper.toProfileResponse(user);
        return action;
    }

//    @Override
//    public UserDto toggleFavoriteProduct(Long productId) {
//        log.info("toggleFavoriteProduct: Toggling favorite product {} for user", productId);
//        String emailAddress = SecurityContextHolder.getContext().getAuthentication().getName();
//        User user = userRepository.findByEmailAddressAndDeletedFalse(emailAddress)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + emailAddress).build());
//
//        Product product = productRepository.findByIdAndDeletedFalse(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
//
//        boolean isFavorited = user.getFavoriteProducts().contains(product);
//        String action = isFavorited ? "REMOVE_FAVORITE_PRODUCT" : "ADD_FAVORITE_PRODUCT";
//
//        if (isFavorited) {
//            user.getFavoriteProducts().remove(product);
//            product.getUsersFavourite().remove(user);
//        } else {
//            user.getFavoriteProducts().add(product);
//            product.getUsersFavourite().add(user);
//        }
//
//        userRepository.save(user);
//        productRepository.save(product);
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(emailAddress)
//                .action(action)
//                .performedBy(emailAddress)
//                .timestamp(LocalDateTime.now())
//                .build());
//
//        return userMapper.toDto(user);
//    }

    @Override
    @RateLimiter(name = "pictureUpload")
    @Transactional
    public UserProfileResponse uploadProfilePicture(MultipartFile image, String email) {
        log.info("uploadProfilePicture: Uploading profile picture for user: {}", email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        if (image.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
            log.error("Image size {} exceeds limit {}", image.getSize(), GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds limit").build();
        }

        String mimeType;
        try {
            mimeType = tika.detect(image.getInputStream()); // Using tika-core:2.9.2
            if (mimeType == null || !groupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
        } catch (IOException e) {
            log.error("Error detecting image type: {}", e.getMessage());
            throw BadRequestException.builder().message("Error detecting image type: " + e.getMessage()).build();
        }

        try (var inputStream = image.getInputStream()) {
            Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "profiles");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            String fileExtension = getFileExtension(image.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
            Path filePath = uploadDir.resolve(fileName);

            if (user.getProfilePictureUrl() != null) {
                String oldImageUrl = user.getProfilePictureUrl();
                deleteOldImage(oldImageUrl);
            }
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            //user.setProfilePictureUrl("/Uploads/profiles/" + fileName);
            user.setProfilePictureUrl(filePath.toString());

            userRepository.save(user);

            log.info("Profile picture uploaded for user {}: {}", email, filePath);

            auditLogService.logAction(email, "UPLOAD_PROFILE_PICTURE", "New image: " + fileName);

            return userMapper.toProfileResponse(user);
        } catch (IOException e) {
            log.error("Failed to upload profile picture for user {}: {}", email, e.getMessage());
            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
        }
    }

//    v2
//    @Override
//    @RateLimiter(name = "profilePictureUpload")
//    public ProfilePictureResponse uploadProfilePicture(MultipartFile file) {
//        log.info("uploadProfilePicture: Uploading profile picture for authenticated user");
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (email == null || email.isEmpty()) {
//            log.error("No authenticated user found");
//            throw new SecurityException("Authentication required");
//        }
//
//        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
//
//        if (file == null || file.isEmpty()) {
//            log.error("Error uploading profile picture: file is empty");
//            throw BadRequestException.builder().message("Error uploading profile picture: file is empty.").build();
//        }
//        if (file.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
//            log.error("Error uploading profile picture: file size {} exceeds limit {}", file.getSize(), GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE);
//            throw BadRequestException.builder().message("Error uploading profile picture: file size exceeds limit.").build();
//        }
//
//        try {
//            if (!GroupConfig.isAllowedImage(file)) {
//                log.error("Error uploading profile picture: file type {} is not supported", file.getContentType());
//                throw BadRequestException.builder().message("Error uploading profile picture: file type not supported: " + file.getContentType()).build();
//            }
//
//            Path uploadDir = Path.of(GroupConfig.UPLOAD_DIR);
//            if (!Files.exists(uploadDir)) {
//                Files.createDirectories(uploadDir);
//                log.info("Created upload directory: {}", uploadDir);
//            }
//
//            String fileExtension = getFileExtension(file.getOriginalFilename());
//            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
//            Path filePath = uploadDir.resolve(fileName);
//
//            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//            log.info("Saved profile picture to: {}", filePath);
//
//            String oldProfilePictureUrl = user.getProfilePictureUrl();
//            if (oldProfilePictureUrl != null && !oldProfilePictureUrl.isEmpty()) {
//                Path oldFilePath = Path.of(oldProfilePictureUrl);
//                if (Files.exists(oldFilePath)) {
//                    try {
//                        Files.setPosixFilePermissions(oldFilePath, Set.of(PosixFilePermission.OWNER_WRITE));
//                        boolean deleted = Files.deleteIfExists(oldFilePath);
//                        if (deleted) {
//                            log.info("Successfully deleted old profile picture: {}", oldProfilePictureUrl);
//                        } else {
//                            log.warn("Old profile picture file not deleted at: {}", oldProfilePictureUrl);
//                        }
//                    } catch (IOException e) {
//                        log.warn("Failed to delete old profile picture at: {}. Continuing with upload.", oldFilePath, e);
//                    }
//                } else {
//                    log.warn("Old profile picture not found at: {}", oldFilePath);
//                }
//            }
//
//            String newProfilePictureUrl = "/" + GroupConfig.UPLOAD_DIR + "/" + fileName;
//            user.setProfilePictureUrl(newProfilePictureUrl);
//            userRepository.save(user);
//
//            auditLogRepository.save(AuditLog.builder()
//                    .userEmail(email)
//                    .action("UPLOAD_PROFILE_PICTURE")
//                    .performedBy(email)
//                    .details("Uploaded file: " + fileName + ", size: " + file.getSize() + " bytes")
//                    .timestamp(LocalDateTime.now())
//                    .build());
//
//            return ProfilePictureResponse.builder()
//                    .profilePictureUrl(newProfilePictureUrl)
//                    .message("Profile picture uploaded successfully")
//                    .build();
//        } catch (IOException e) {
//            log.error("Failed to upload profile picture for user {}: {}", email, e.getMessage());
//            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
//        }
//    }

//    v1
//    @Override
//    public UserDto uploadProfilePicture(String email, MultipartFile file) {
//        log.info("uploadProfilePicture: Uploading profile picture for user: {}", email);
//        User user = userRepository.findByEmailAddress(email)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        if (file.isEmpty()) {
//            log.error("Error uploading profile picture: file is empty");
//            throw BadRequestException.builder().message("Error uploading profile picture: file is empty.").build();
//        }
//        if (file.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
//            log.error("Error uploading profile picture: file size is too large");
//            throw BadRequestException.builder().message("Error uploading profile picture: file size is too large.").build();
//        }
//        try {
//            if (!GroupConfig.isAllowedImage(file)) {
//                log.error("Error uploading profile picture: file type is not supported for: {}", file.getContentType());
//                throw BadRequestException.builder().message("Error uploading profile picture: file type is not supported for: " + file.getContentType()).build();
//            }
//
//            Path uploadPath = Paths.get(GroupConfig.UPLOAD_DIR);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            String fileExtension = getFileExtension(file.getOriginalFilename());
//            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString() + "." + fileExtension;
//            Path filePath = uploadPath.resolve(fileName);
//
//            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
//                Path oldFilePath = Paths.get(user.getProfilePictureUrl());
//                if (Files.exists(oldFilePath)) {
//                    try {
//                        Files.setPosixFilePermissions(oldFilePath, Files.getPosixFilePermissions(filePath)); //java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
//                        boolean deleted = Files.deleteIfExists(oldFilePath);
//                        if (deleted) {
//                            log.info("Successfully deleted old profile picture: {}", user.getProfilePictureUrl());
//                        } else {
//                            log.warn("Old profile picture file not deleted at: {}", user.getProfilePictureUrl());
//                        }
//                    } catch (IOException e) {
//                        log.warn("Failed to delete old profile picture at: {}. Continuing with upload.", oldFilePath, e);
//                    }
//                } else {
//                    log.warn("Old profile picture not found at: {}", oldFilePath);
//                }
//            }
//
//            user.setProfilePictureUrl("/" + GroupConfig.UPLOAD_DIR + "/" + fileName);
//            userRepository.save(user);
//
//            auditLogRepository.save(AuditLog.builder()
//                    .userEmail(email)
//                    .action("UPLOAD_PROFILE_PICTURE")
//                    .performedBy(email)
//                    .timestamp(LocalDateTime.now())
//                    .build());
//
//            return userMapper.toDto(user);
//        } catch (IOException e) {
//            log.error("Failed to upload profile picture: {}", e.getMessage());
//            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
//        }
//    }



    *//*public UserDto uploadProfilePicture(String email, MultipartFile file) {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (file.isEmpty()) {
            log.error("Error uploading profile picture: file is empty");
            throw BadRequestException.builder().message("Error uploading profile picture: file is empty.")
                    .build();
        }
        if (file.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
            log.error("Error uploading profile picture: file size is too large");
            throw BadRequestException.builder().message("Error uploading profile picture: file size is too large.")
                    .build();
        }
        try {
            if (!GroupConfig.isAllowedImage(file)) {
                log.error("Error uploading profile picture: file type is not supported for :{}", file.getContentType());
                throw BadRequestException.builder().message("Error uploading profile picture: file type is not supported for : "
                                + file.getContentType())
                        .build();
            }

            Path uploadPath = Paths.get(GroupConfig.UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString() + "." + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            if (user.getProfilePictureUrl() != null || !user.getProfilePictureUrl().isEmpty()) {
                Path oldFilePath = Paths.get(user.getProfilePictureUrl());
                if (Files.exists(oldFilePath)) {
                    try {

                        //Path oldFilePath = Paths.get(user.getProfilePictureUrl()).toAbsolutePath().normalize();
                        // verify oldfile deletion, it wasn't deleted on mac
//                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(uploadPath);
//                if (!perms.contains(PosixFilePermission.OWNER_WRITE)) {
//                    perms.add(PosixFilePermission.OWNER_WRITE);
//                    Files.setPosixFilePermissions(oldFilePath, perms);
//                    log.info("Updated permissions for file: {}", oldFilePath);
//                }

                        boolean deleted = Files.deleteIfExists(oldFilePath);

                        if (deleted) {
                            log.info("Successfully deleted old profile picture: {}", user.getProfilePictureUrl());
                        } else {
                            log.warn("Old profile picture file not deleted at: {}", user.getProfilePictureUrl());
                        }
                    } catch (IOException e) {
                        log.warn("Failed to delete old profile picture at: {}. Continuing with upload.", oldFilePath, e);
                    }
                } else {
                    log.warn("Old profile picture not found at: {}", oldFilePath);
                }
            }

            user.setProfilePictureUrl("/" + GroupConfig.UPLOAD_DIR + fileName);

            userRepository.save(user);

            return userMapper.toDto(user);
        } catch (IOException e) {
            log.error("Failed to upload profile picture: {}", e.getMessage());
            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
        }
    }*//*
    
    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            if (Files.exists(oldFilePath)) {
                try {
                    Files.deleteIfExists(oldFilePath);
                    log.info("Deleted old profile image: {}", oldImageUrl);
                } catch (IOException e) {
                    log.warn("Failed to delete old profile image {}: {}", oldImageUrl, e.getMessage());
                }
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}*/
