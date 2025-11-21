package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.AddressMapper;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.AddressRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IUserAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing addresses associated with User entities.
 * Extends AbstractAddressService to inherit common address management functionality
 * and implements User-specific address operations.
 */
@Slf4j
@Service
public class UserAddressService extends AbstractAddressService<User> implements IUserAddressService {

    private final UserRepository userRepository;

    /**
     * Constructor for UserAddressService.
     * Required repositories and mappers are injected via constructor.
     *
     * @param addressRepository Repository for address operations
     * @param addressMapper     Mapper for converting between entities and DTOs
     * @param userRepository    Repository for user operations
     */
    public UserAddressService(
            AddressRepository addressRepository,
            AddressMapper addressMapper,
            UserRepository userRepository
    ) {
        super(addressRepository, addressMapper);
        this.userRepository = userRepository;
    }

    // ========================================
    // Implementation of Abstract Methods
    // ========================================

    @Override
    protected void validateParentExists(Long userId) {
        log.debug("Validating user exists: {}", userId);
        if (!userRepository.existsByIdAndDeletedFalse(userId)) {
            log.error("User not found: {}", userId);
            throw UserNotFoundException.builder()
                    .message("User not found: " + userId)
                    .build();
        }
    }

    @Override
    protected void linkAddressToParent(Address address, Long userId) {
        log.debug("Linking address to user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());
        address.setUser(user);
        address.setCompany(null); // Ensure company is null for user addresses
    }

    @Override
    protected Page<Address> findAddressesByParentId(Long userId, Pageable pageable) {
        log.debug("Finding paginated addresses for user: {}", userId);
        return addressRepository.findByUserId(userId, pageable);
    }

    @Override
    protected List<Address> findAllAddressesByParentId(Long userId) {
        log.debug("Finding all addresses for user: {}", userId);
        return addressRepository.findAllByUserId(userId);
    }

    @Override
    protected Address findAddressByIdAndParentId(Long userId, Long addressId) {
        log.debug("Finding address {} for user {}", addressId, userId);
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address " + addressId + " not found for user " + userId)
                        .build());
    }

    @Override
    protected void unsetDefaultAddressesForParent(Long userId, AddressType type, Long excludeAddressId) {
        log.debug("Unsetting default {} addresses for user {}, excluding: {}",
                type, userId, excludeAddressId);

        List<Address> addresses = addressRepository.findByUserIdAndType(userId, type);

        addresses.stream()
                .filter(Address::isDefault)
                .filter(a -> excludeAddressId == null || !a.getId().equals(excludeAddressId))
                .forEach(a -> {
                    log.debug("Unsetting default flag for address: {}", a.getId());
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }

    // ========================================
    // User-Specific Public Methods
    // ========================================

    /**
     * Get all addresses of a specific type for a user.
     * This method returns entities rather than DTOs for internal use.
     *
     * @param userId The user ID
     * @param type   The address type
     * @return List of address entities
     */
    @Transactional(readOnly = true)
    public List<Address> getAddressesByUserIdAndType(Long userId, AddressType type) {
        log.info("Fetching {} addresses for user {}", type, userId);
        validateParentExists(userId);
        return addressRepository.findByUserIdAndType(userId, type);
    }

    /**
     * Get all addresses of a specific type for a user as DTOs.
     *
     * @param userId The user ID
     * @param type   The address type
     * @return List of address responses
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressResponsesByUserIdAndType(Long userId, AddressType type) {
        log.info("Fetching {} address responses for user {}", type, userId);
        List<Address> addresses = getAddressesByUserIdAndType(userId, type);
        return addresses.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get the default address of a specific type for a user.
     * Returns null if no default address exists.
     *
     * @param userId The user ID
     * @param type   The address type
     * @return The default address entity or null
     */
    @Transactional(readOnly = true)
    public Address getDefaultAddressByUserIdAndType(Long userId, AddressType type) {
        log.info("Fetching default {} address for user {}", type, userId);
        validateParentExists(userId);
        return addressRepository.findDefaultByUserIdAndType(userId, type)
                .orElse(null);
    }

    /**
     * Get the default address of a specific type for a user as DTO.
     * Returns null if no default address exists.
     *
     * @param userId The user ID
     * @param type   The address type
     * @return The default address response or null
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddressResponseByUserIdAndType(Long userId, AddressType type) {
        log.info("Fetching default {} address response for user {}", type, userId);
        Address address = getDefaultAddressByUserIdAndType(userId, type);
        return address != null ? addressMapper.toResponse(address) : null;
    }

    /**
     * Count the total number of addresses for a user.
     *
     * @param userId The user ID
     * @return The count of addresses
     */
    @Transactional(readOnly = true)
    public long countAddressesByUserId(Long userId) {
        log.info("Counting addresses for user {}", userId);
        validateParentExists(userId);
        return addressRepository.countByUserId(userId);
    }

    /**
     * Count addresses of a specific type for a user.
     *
     * @param userId The user ID
     * @param type   The address type
     * @return The count of addresses
     */
    @Transactional(readOnly = true)
    public long countAddressesByUserIdAndType(Long userId, AddressType type) {
        log.info("Counting {} addresses for user {}", type, userId);
        validateParentExists(userId);
        return addressRepository.findByUserIdAndType(userId, type).size();
    }

    /**
     * Check if a user has any addresses.
     *
     * @param userId The user ID
     * @return true if user has at least one address
     */
    @Override
    @Transactional(readOnly = true)
    public boolean userHasAddresses(Long userId) {
        log.debug("Checking if user {} has addresses", userId);
        return countAddressesByUserId(userId) > 0;
    }

    /**
     * Check if a user has a default address of a specific type.
     *
     * @param userId The user ID
     * @param type   The address type
     * @return true if default address exists
     */
    @Transactional(readOnly = true)
    public boolean hasDefaultAddressForType(Long userId, AddressType type) {
        log.debug("Checking if user {} has default {} address", userId, type);
        return getDefaultAddressByUserIdAndType(userId, type) != null;
    }

    /**
     * Get all shipping addresses for a user.
     * Convenience method for frequently used address type.
     *
     * @param userId The user ID
     * @return List of shipping addresses
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getShippingAddresses(Long userId) {
        log.info("Fetching shipping addresses for user {}", userId);
        return getAddressResponsesByUserIdAndType(userId, AddressType.SHIPPING);
    }

    /**
     * Get all billing addresses for a user.
     * Convenience method for frequently used address type.
     *
     * @param userId The user ID
     * @return List of billing addresses
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getBillingAddresses(Long userId) {
        log.info("Fetching billing addresses for user {}", userId);
        return getAddressResponsesByUserIdAndType(userId, AddressType.BILLING);
    }

    /**
     * Get default shipping address for a user.
     *
     * @param userId The user ID
     * @return The default shipping address or null
     */
    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultShippingAddress(Long userId) {
        log.info("Fetching default shipping address for user {}", userId);
        return getDefaultAddressResponseByUserIdAndType(userId, AddressType.SHIPPING);
    }

    /**
     * Get default billing address for a user.
     *
     * @param userId The user ID
     * @return The default billing address or null
     */
    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultBillingAddress(Long userId) {
        log.info("Fetching default billing address for user {}", userId);
        return getDefaultAddressResponseByUserIdAndType(userId, AddressType.BILLING);
    }

    /**
     * Delete all addresses for a user.
     * Use with caution - typically used when deleting a user.
     *
     * @param userId The user ID
     */
    @Transactional
    public void deleteAllUserAddresses(Long userId) {
        log.warn("Deleting all addresses for user {}", userId);
        validateParentExists(userId);
        addressRepository.deleteAllByUserId(userId);
        log.info("All addresses deleted for user {}", userId);
    }
}


//
//@Service
/// /@RequiredArgsConstructor
//public class UserAddressService extends AbstractAddressService<User> implements IUserAddressService {
//
//    private final UserRepository userRepository;
//
//    public UserAddressService(
//            AddressRepository addressRepository,
//            AddressMapper addressMapper,
//            UserRepository userRepository
//    ) {
//        super(addressRepository, addressMapper);
//        this.userRepository = userRepository;
//    }
//
//    @Override
//    protected Set<Address> getAddresses(User parent) {
//        return parent.getAddresses();
//    }
//
//    @Override
//    protected User loadParentWithAddresses(Long userId) {
//        return userRepository.findByIdAndDeletedFalseWithAddresses(userId)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("User not found: " + userId)
//                        .build());
//    }
//
//    @Override
//    protected void saveParent(User user) {
//        userRepository.save(user);
//    }
//
//    @Override
//    protected void linkAddressToParent(Address address, User user) {
//        address.setUser(user);
//    }
//
//    @Override
//    protected UserNotFoundException parentNotFoundException(Long id) {
//        return UserNotFoundException.builder().message("User not found: " + id).build();
//    }
//
//    @Override
//    protected Address getAddressByIdAndParentId(Long userId, Long addressId) {
//        return getAddressEntity(userId, addressId);
//    }
//}

/*
import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.AddressMapper;
import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.AddressRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IUserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService implements IUserAddressService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper userAddressMapper;

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        log.info("Adding address for user ID: {}", userId);
        User user = userRepository.findByIdAndDeletedFalseWithAddresses(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());

        Address address = userAddressMapper.toEntity(request);
        address.setUser(user);
        if (request.isDefault()) {
            unsetOtherDefaultAddresses(user, request.getAddressType());
        }
        address.setDefault(request.isDefault());
        address = addressRepository.save(address);
        user.getAddresses().add(address);
        userRepository.save(user);
        log.info("Address added successfully for user ID: {}, address ID: {}", userId, address.getId());
        return userAddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        log.info("Updating address ID: {} for user ID: {}", addressId, userId);
        User user = userRepository.findByIdAndDeletedFalseWithAddresses(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!user.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to user: " + addressId)
                    .build();
        }

        userAddressMapper.updateEntityFromRequest(request, address);
        if (request.isDefault()) {
            unsetOtherDefaultAddresses(user, request.getAddressType(), addressId);
        }
        address.setDefault(request.isDefault());
        address = addressRepository.save(address);
        log.info("Address ID: {} updated successfully for user ID: {}", addressId, userId);
        return userAddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address ID: {} for user ID: {}", addressId, userId);
        User user = userRepository.findByIdAndDeletedFalseWithAddresses(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!user.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to user: " + addressId)
                    .build();
        }

        user.getAddresses().remove(address);
        addressRepository.delete(address);
        userRepository.save(user);
        log.info("Address ID: {} deleted successfully for user ID: {}", addressId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long userId, Long addressId) {
        log.info("Retrieving address ID: {} for user ID: {}", addressId, userId);
        User user = userRepository.findByIdAndDeletedFalseWithAddresses(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!user.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to user: " + addressId)
                    .build();
        }

        return userAddressMapper.toResponse(address);
    }

    @Transactional(readOnly = true)
    public Address getAddressByIdAndByUserId(Long userId, Long addressId) {
        log.info("Retrieving address entity {} for user ID: {}", addressId, userId);
        User user = userRepository.findByIdAndDeletedFalseWithAddresses(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!user.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to user: " + addressId)
                    .build();
        }
        return address;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses(Long userId) {
        log.info("Retrieving all addresses for user ID: {}", userId);
        User user = userRepository.findByIdAndDeletedFalseWithAddresses(userId)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("User not found: " + userId)
                        .build());
        return user.getAddresses().stream()
                .map(userAddressMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void unsetOtherDefaultAddresses(User user, AddressType addressType, Long... excludeAddressId) {
        user.getAddresses().stream()
                .filter(a -> a.getAddressType() == addressType && a.isDefault() && (excludeAddressId.length == 0 || !a.getId().equals(excludeAddressId[0])))
                .forEach(a -> a.setDefault(false));
    }
}*/
