package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
import com.datasaz.ecommerce.mappers.AddressMapper;
import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.AddressRepository;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base service for managing addresses associated with parent entities (User or Company).
 * This service provides common CRUD operations and address management functionality
 * that can be shared across different parent entity types.
 *
 * @param <T> The type of parent entity (User or Company)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAddressService<T> {

    protected final AddressRepository addressRepository;
    protected final AddressMapper addressMapper;

    // ========================================
    // Abstract methods to be implemented by subclasses
    // ========================================

    /**
     * Validates that the parent entity exists and is not deleted.
     * Throws appropriate exception if not found.
     *
     * @param parentId The ID of the parent entity
     */
    protected abstract void validateParentExists(Long parentId);

    /**
     * Links the address to the parent entity by setting the appropriate foreign key.
     *
     * @param address  The address to link
     * @param parentId The ID of the parent entity
     */
    protected abstract void linkAddressToParent(Address address, Long parentId);

    /**
     * Finds paginated addresses for a specific parent entity.
     *
     * @param parentId The ID of the parent entity
     * @param pageable Pagination parameters
     * @return Page of addresses
     */
    protected abstract Page<Address> findAddressesByParentId(Long parentId, Pageable pageable);

    /**
     * Finds all addresses for a specific parent entity (non-paginated).
     *
     * @param parentId The ID of the parent entity
     * @return List of all addresses
     */
    protected abstract List<Address> findAllAddressesByParentId(Long parentId);

    /**
     * Finds a specific address by ID that belongs to the parent entity.
     *
     * @param parentId  The ID of the parent entity
     * @param addressId The ID of the address
     * @return The address entity
     * @throws AddressNotFoundException if address not found or doesn't belong to parent
     */
    protected abstract Address findAddressByIdAndParentId(Long parentId, Long addressId);

    /**
     * Unsets default flag for all addresses of the same type for the parent entity,
     * optionally excluding a specific address.
     *
     * @param parentId         The ID of the parent entity
     * @param type             The address type
     * @param excludeAddressId Optional address ID to exclude from unsetting
     */
    protected abstract void unsetDefaultAddressesForParent(Long parentId, AddressType type, Long excludeAddressId);

    // ========================================
    // CRUD Operations
    // ========================================

    /**
     * Add a new address for the parent entity.
     * If the address is marked as default, automatically unsets other default addresses of the same type.
     *
     * @param parentId The ID of the parent entity
     * @param request  The address request data
     * @return The created address response
     */
    @Transactional
    public AddressResponse addAddress(Long parentId, AddressRequest request) {
        log.info("Adding {} address for parent ID: {}", request.getAddressType(), parentId);

        // Validate parent exists
        validateParentExists(parentId);

        // Create address entity from request
        Address address = addressMapper.toEntity(request);

        // Link address to parent entity
        linkAddressToParent(address, parentId);

        // Handle default address logic
        if (request.isDefault()) {
            log.debug("Setting address as default, unsetting other default {} addresses", request.getAddressType());
            unsetDefaultAddressesForParent(parentId, request.getAddressType(), null);
            address.setDefault(true);
        }

        // Save address
        address = addressRepository.save(address);

        log.info("Address {} added successfully for parent: {}", address.getId(), parentId);
        return addressMapper.toResponse(address);
    }

    /**
     * Update an existing address for the parent entity.
     *
     * @param parentId  The ID of the parent entity
     * @param addressId The ID of the address to update
     * @param request   The updated address data
     * @return The updated address response
     */
    @Transactional
    public AddressResponse updateAddress(Long parentId, Long addressId, AddressRequest request) {
        log.info("Updating address {} for parent {}", addressId, parentId);

        // Validate and fetch address
        Address address = findAddressByIdAndParentId(parentId, addressId);

        // Update fields from request
        addressMapper.updateEntityFromRequest(request, address);

        // Handle default address logic
        if (request.isDefault() && !address.isDefault()) {
            log.debug("Setting address as default, unsetting other default {} addresses", request.getAddressType());
            unsetDefaultAddressesForParent(parentId, request.getAddressType(), addressId);
            address.setDefault(true);
        } else if (!request.isDefault() && address.isDefault()) {
            log.debug("Removing default flag from address {}", addressId);
            address.setDefault(false);
        }

        // Save updated address
        address = addressRepository.save(address);

        log.info("Address {} updated successfully", addressId);
        return addressMapper.toResponse(address);
    }

    /**
     * Delete an address for the parent entity.
     *
     * @param parentId  The ID of the parent entity
     * @param addressId The ID of the address to delete
     */
    @Transactional
    public void deleteAddress(Long parentId, Long addressId) {
        log.info("Deleting address {} for parent {}", addressId, parentId);

        // Validate and fetch address
        Address address = findAddressByIdAndParentId(parentId, addressId);

        // Delete address
        addressRepository.delete(address);

        log.info("Address {} deleted successfully", addressId);
    }

    /**
     * Get a single address by ID for the parent entity.
     *
     * @param parentId  The ID of the parent entity
     * @param addressId The ID of the address
     * @return The address response
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long parentId, Long addressId) {
        log.debug("Fetching address {} for parent {}", addressId, parentId);

        Address address = findAddressByIdAndParentId(parentId, addressId);
        return addressMapper.toResponse(address);
    }

    // ========================================
    // List Operations
    // ========================================

    /**
     * Get all addresses for a parent entity (non-paginated).
     * Useful for dropdown lists or when all addresses are needed at once.
     *
     * @param parentId The ID of the parent entity
     * @return List of all addresses
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses(Long parentId) {
        log.debug("Fetching all addresses for parent {}", parentId);

        validateParentExists(parentId);
        List<Address> addresses = findAllAddressesByParentId(parentId);

        log.debug("Found {} addresses for parent {}", addresses.size(), parentId);
        return addresses.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated addresses for a parent entity.
     * Results are sorted with default addresses first, then by ID.
     *
     * @param parentId The ID of the parent entity
     * @param page     Page number (0-based)
     * @param size     Page size
     * @return Page of addresses
     */
    @Transactional(readOnly = true)
    public Page<AddressResponse> getAddressesPaginated(Long parentId, int page, int size) {
        log.debug("Fetching paginated addresses for parent {}, page: {}, size: {}", parentId, page, size);

        validateParentExists(parentId);

        // Sort by default flag (descending) then by ID
        Sort sort = Sort.by(Sort.Order.desc("isDefault"), Sort.Order.asc("id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Address> addressPage = findAddressesByParentId(parentId, pageable);

        log.debug("Found {} addresses (page {} of {})",
                addressPage.getNumberOfElements(),
                addressPage.getNumber(),
                addressPage.getTotalPages());

        return addressPage.map(addressMapper::toResponse);
    }

    /**
     * Get addresses filtered by type for a parent entity.
     *
     * @param parentId The ID of the parent entity
     * @param type     The address type to filter by
     * @return List of addresses of the specified type
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByType(Long parentId, AddressType type) {
        log.debug("Fetching {} addresses for parent {}", type, parentId);

        validateParentExists(parentId);

        List<Address> addresses = findAllAddressesByParentId(parentId).stream()
                .filter(a -> a.getAddressType() == type)
                .collect(Collectors.toList());

        log.debug("Found {} {} addresses for parent {}", addresses.size(), type, parentId);

        return addresses.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // Default Address Operations
    // ========================================

    /**
     * Get the default address of a specific type for the parent entity.
     *
     * @param parentId The ID of the parent entity
     * @param type     The address type
     * @return The default address response
     * @throws AddressNotFoundException if no default address found
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long parentId, AddressType type) {
        log.debug("Fetching default {} address for parent {}", type, parentId);

        validateParentExists(parentId);

        List<Address> addresses = findAllAddressesByParentId(parentId);

        Address defaultAddress = addresses.stream()
                .filter(a -> a.getAddressType() == type && a.isDefault())
                .findFirst()
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("No default " + type + " address found for parent: " + parentId)
                        .build());

        return addressMapper.toResponse(defaultAddress);
    }

    /**
     * Set a specific address as the default for its type.
     * Automatically unsets other default addresses of the same type.
     *
     * @param parentId  The ID of the parent entity
     * @param addressId The ID of the address to set as default
     * @return The updated address response
     */
    @Transactional
    public AddressResponse setDefaultAddress(Long parentId, Long addressId) {
        log.info("Setting address {} as default for parent {}", addressId, parentId);

        // Fetch and validate address
        Address address = findAddressByIdAndParentId(parentId, addressId);

        // Unset other default addresses of the same type
        unsetDefaultAddressesForParent(parentId, address.getAddressType(), addressId);

        // Set this address as default
        address.setDefault(true);
        address = addressRepository.save(address);

        log.info("Address {} set as default {} address", addressId, address.getAddressType());
        return addressMapper.toResponse(address);
    }

    // ========================================
    // Internal/Helper Methods
    // ========================================

    /**
     * Get the address entity (for internal use by other services).
     * This returns the JPA entity rather than the DTO.
     *
     * @param parentId  The ID of the parent entity
     * @param addressId The ID of the address
     * @return The address entity
     */
    @Transactional(readOnly = true)
    public Address getAddressEntity(Long parentId, Long addressId) {
        log.debug("Fetching address entity {} for parent {}", addressId, parentId);
        return findAddressByIdAndParentId(parentId, addressId);
    }

    /**
     * Check if a parent has any addresses.
     *
     * @param parentId The ID of the parent entity
     * @return true if parent has at least one address
     */
    @Transactional(readOnly = true)
    public boolean hasAddresses(Long parentId) {
        validateParentExists(parentId);
        List<Address> addresses = findAllAddressesByParentId(parentId);
        return !addresses.isEmpty();
    }

    /**
     * Check if a parent has a default address of a specific type.
     *
     * @param parentId The ID of the parent entity
     * @param type     The address type
     * @return true if a default address of the type exists
     */
    @Transactional(readOnly = true)
    public boolean hasDefaultAddress(Long parentId, AddressType type) {
        validateParentExists(parentId);
        List<Address> addresses = findAllAddressesByParentId(parentId);
        return addresses.stream()
                .anyMatch(a -> a.getAddressType() == type && a.isDefault());
    }
}


//@Slf4j
//@RequiredArgsConstructor
//public abstract class AbstractAddressService<T> {
//
//    protected final AddressRepository addressRepository;
//    protected final AddressMapper addressMapper;
//
//    protected abstract Set<Address> getAddresses(T parent);
//
//    protected abstract Address getAddressByIdAndParentId(Long parentId, Long addressId);
//
//    protected abstract <E extends RuntimeException> E parentNotFoundException(Long id);
//
//    @Transactional
//    public AddressResponse addAddress(Long parentId, AddressRequest request) {
//        log.info("Adding address for parent ID: {}", parentId);
//        T parent = loadParentWithAddresses(parentId);
//
//        Address address = addressMapper.toEntity(request);
//        linkAddressToParent(address, parent);
//
//        if (request.isDefault()) {
//            unsetOtherDefaultAddresses(parent, request.getAddressType());
//        }
//        address.setDefault(request.isDefault());
//
//        address = addressRepository.save(address);
//        getAddresses(parent).add(address);
//        saveParent(parent);
//
//        log.info("Address added: {} for parent: {}", address.getId(), parentId);
//        return addressMapper.toResponse(address);
//    }
//
//    @Transactional
//    public AddressResponse updateAddress(Long parentId, Long addressId, AddressRequest request) {
//        log.info("Updating address {} for parent {}", addressId, parentId);
//        T parent = loadParentWithAddresses(parentId);
//        Address address = validateAddressBelongsToParent(parent, addressId);
//
//        addressMapper.updateEntityFromRequest(request, address);
//        if (request.isDefault()) {
//            unsetOtherDefaultAddresses(parent, request.getAddressType(), addressId);
//        }
//        address.setDefault(request.isDefault());
//        address = addressRepository.save(address);
//
//        log.info("Address {} updated", addressId);
//        return addressMapper.toResponse(address);
//    }
//
//    @Transactional
//    public void deleteAddress(Long parentId, Long addressId) {
//        log.info("Deleting address {} for parent {}", addressId, parentId);
//        T parent = loadParentWithAddresses(parentId);
//        Address address = validateAddressBelongsToParent(parent, addressId);
//
//        getAddresses(parent).remove(address);
//        addressRepository.delete(address);
//        saveParent(parent);
//        log.info("Address {} deleted", addressId);
//    }
//
//    @Transactional(readOnly = true)
//    public AddressResponse getAddress(Long parentId, Long addressId) {
//        T parent = loadParentWithAddresses(parentId);
//        Address address = validateAddressBelongsToParent(parent, addressId);
//        return addressMapper.toResponse(address);
//    }
//
//    @Transactional(readOnly = true)
//    public List<AddressResponse> getAllAddresses(Long parentId) {
//        T parent = loadParentWithAddresses(parentId);
//        return getAddresses(parent).stream()
//                .map(addressMapper::toResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public Address getAddressEntity(Long parentId, Long addressId) {
//        T parent = loadParentWithAddresses(parentId);
//        return validateAddressBelongsToParent(parent, addressId);
//    }
//
//    protected abstract T loadParentWithAddresses(Long parentId);
//
//    protected abstract void saveParent(T parent);
//
//    protected abstract void linkAddressToParent(Address address, T parent);
//
//    private Address validateAddressBelongsToParent(T parent, Long addressId) {
//        return getAddresses(parent).stream()
//                .filter(a -> a.getId().equals(addressId))
//                .findFirst()
//                .orElseThrow(() -> AddressNotFoundException.builder()
//                        .message("Address not found or does not belong to parent: " + addressId)
//                        .build());
//    }
//
//    private void unsetOtherDefaultAddresses(T parent, AddressType type, Long... excludeId) {
//        getAddresses(parent).stream()
//                .filter(a -> a.getAddressType() == type &&
//                        a.isDefault() &&
//                        (excludeId.length == 0 || !a.getId().equals(excludeId[0])))
//                .forEach(a -> a.setDefault(false));
//    }
//}
