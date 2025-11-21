package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
import com.datasaz.ecommerce.exceptions.CompanyNotFoundException;
import com.datasaz.ecommerce.mappers.AddressMapper;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.AddressRepository;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.services.interfaces.ICompanyAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing addresses associated with Company entities.
 * Extends AbstractAddressService to inherit common address management functionality
 * and implements Company-specific address operations.
 */
@Slf4j
@Service
public class CompanyAddressService extends AbstractAddressService<Company> implements ICompanyAddressService {

    private final CompanyRepository companyRepository;

    /**
     * Constructor for CompanyAddressService.
     * Required repositories and mappers are injected via constructor.
     *
     * @param addressRepository Repository for address operations
     * @param addressMapper     Mapper for converting between entities and DTOs
     * @param companyRepository Repository for company operations
     */
    public CompanyAddressService(
            AddressRepository addressRepository,
            AddressMapper addressMapper,
            CompanyRepository companyRepository
    ) {
        super(addressRepository, addressMapper);
        this.companyRepository = companyRepository;
    }

    // ========================================
    // Implementation of Abstract Methods
    // ========================================

    @Override
    protected void validateParentExists(Long companyId) {
        log.debug("Validating company exists: {}", companyId);
        if (!companyRepository.existsByIdAndDeletedFalse(companyId)) {
            log.error("Company not found: {}", companyId);
            throw CompanyNotFoundException.builder()
                    .message("Company not found: " + companyId)
                    .build();
        }
    }

    @Override
    protected void linkAddressToParent(Address address, Long companyId) {
        log.debug("Linking address to company: {}", companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());
        address.setCompany(company);
        address.setUser(null); // Ensure user is null for company addresses
    }

    @Override
    protected Page<Address> findAddressesByParentId(Long companyId, Pageable pageable) {
        log.debug("Finding paginated addresses for company: {}", companyId);
        return addressRepository.findByCompanyId(companyId, pageable);
    }

    @Override
    protected List<Address> findAllAddressesByParentId(Long companyId) {
        log.debug("Finding all addresses for company: {}", companyId);
        return addressRepository.findAllByCompanyId(companyId);
    }

    @Override
    protected Address findAddressByIdAndParentId(Long companyId, Long addressId) {
        log.debug("Finding address {} for company {}", addressId, companyId);
        return addressRepository.findByIdAndCompanyId(addressId, companyId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address " + addressId + " not found for company " + companyId)
                        .build());
    }

    @Override
    protected void unsetDefaultAddressesForParent(Long companyId, AddressType type, Long excludeAddressId) {
        log.debug("Unsetting default {} addresses for company {}, excluding: {}",
                type, companyId, excludeAddressId);

        List<Address> addresses = addressRepository.findByCompanyIdAndType(companyId, type);

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
    // Company-Specific Public Methods
    // ========================================

    /**
     * Get all addresses of a specific type for a company.
     * This method returns entities rather than DTOs for internal use.
     *
     * @param companyId The company ID
     * @param type      The address type
     * @return List of address entities
     */
    @Transactional(readOnly = true)
    public List<Address> getAddressesByCompanyIdAndType(Long companyId, AddressType type) {
        log.info("Fetching {} addresses for company {}", type, companyId);
        validateParentExists(companyId);
        return addressRepository.findByCompanyIdAndType(companyId, type);
    }

    /**
     * Get all addresses of a specific type for a company as DTOs.
     *
     * @param companyId The company ID
     * @param type      The address type
     * @return List of address responses
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressResponsesByCompanyIdAndType(Long companyId, AddressType type) {
        log.info("Fetching {} address responses for company {}", type, companyId);
        List<Address> addresses = getAddressesByCompanyIdAndType(companyId, type);
        return addresses.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get the default address of a specific type for a company.
     * Returns null if no default address exists.
     *
     * @param companyId The company ID
     * @param type      The address type
     * @return The default address entity or null
     */
    @Transactional(readOnly = true)
    public Address getDefaultAddressByCompanyIdAndType(Long companyId, AddressType type) {
        log.info("Fetching default {} address for company {}", type, companyId);
        validateParentExists(companyId);
        return addressRepository.findDefaultByCompanyIdAndType(companyId, type)
                .orElse(null);
    }

    /**
     * Get the default address of a specific type for a company as DTO.
     * Returns null if no default address exists.
     *
     * @param companyId The company ID
     * @param type      The address type
     * @return The default address response or null
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddressResponseByCompanyIdAndType(Long companyId, AddressType type) {
        log.info("Fetching default {} address response for company {}", type, companyId);
        Address address = getDefaultAddressByCompanyIdAndType(companyId, type);
        return address != null ? addressMapper.toResponse(address) : null;
    }

    /**
     * Count the total number of addresses for a company.
     *
     * @param companyId The company ID
     * @return The count of addresses
     */
    @Transactional(readOnly = true)
    public long countAddressesByCompanyId(Long companyId) {
        log.info("Counting addresses for company {}", companyId);
        validateParentExists(companyId);
        return addressRepository.countByCompanyId(companyId);
    }

    /**
     * Count addresses of a specific type for a company.
     *
     * @param companyId The company ID
     * @param type      The address type
     * @return The count of addresses
     */
    @Transactional(readOnly = true)
    public long countAddressesByCompanyIdAndType(Long companyId, AddressType type) {
        log.info("Counting {} addresses for company {}", type, companyId);
        validateParentExists(companyId);
        return addressRepository.findByCompanyIdAndType(companyId, type).size();
    }

    /**
     * Check if a company has any addresses.
     *
     * @param companyId The company ID
     * @return true if company has at least one address
     */
    @Transactional(readOnly = true)
    public boolean companyHasAddresses(Long companyId) {
        log.debug("Checking if company {} has addresses", companyId);
        return countAddressesByCompanyId(companyId) > 0;
    }

    /**
     * Check if a company has a default address of a specific type.
     *
     * @param companyId The company ID
     * @param type      The address type
     * @return true if default address exists
     */
    @Transactional(readOnly = true)
    public boolean hasDefaultAddressForType(Long companyId, AddressType type) {
        log.debug("Checking if company {} has default {} address", companyId, type);
        return getDefaultAddressByCompanyIdAndType(companyId, type) != null;
    }

    /**
     * Get all expedition addresses for a company.
     * Expedition addresses are used for shipping/warehouse locations.
     *
     * @param companyId The company ID
     * @return List of expedition addresses
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getExpeditionAddresses(Long companyId) {
        log.info("Fetching expedition addresses for company {}", companyId);
        return getAddressResponsesByCompanyIdAndType(companyId, AddressType.EXPEDITION);
    }

    /**
     * Get all billing addresses for a company.
     *
     * @param companyId The company ID
     * @return List of billing addresses
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getBillingAddresses(Long companyId) {
        log.info("Fetching billing addresses for company {}", companyId);
        return getAddressResponsesByCompanyIdAndType(companyId, AddressType.BILLING);
    }

    /**
     * Get all contact addresses for a company.
     *
     * @param companyId The company ID
     * @return List of contact addresses
     */
    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getContactAddresses(Long companyId) {
        log.info("Fetching contact addresses for company {}", companyId);
        return getAddressResponsesByCompanyIdAndType(companyId, AddressType.CONTACT);
    }

    /**
     * Get default expedition address for a company.
     *
     * @param companyId The company ID
     * @return The default expedition address or null
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultExpeditionAddress(Long companyId) {
        log.info("Fetching default expedition address for company {}", companyId);
        return getDefaultAddressResponseByCompanyIdAndType(companyId, AddressType.EXPEDITION);
    }

    /**
     * Get default billing address for a company.
     *
     * @param companyId The company ID
     * @return The default billing address or null
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultBillingAddress(Long companyId) {
        log.info("Fetching default billing address for company {}", companyId);
        return getDefaultAddressResponseByCompanyIdAndType(companyId, AddressType.BILLING);
    }

    /**
     * Get default contact address for a company.
     *
     * @param companyId The company ID
     * @return The default contact address or null
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultContactAddress(Long companyId) {
        log.info("Fetching default contact address for company {}", companyId);
        return getDefaultAddressResponseByCompanyIdAndType(companyId, AddressType.CONTACT);
    }

    /**
     * Delete all addresses for a company.
     * Use with caution - typically used when deleting a company.
     *
     * @param companyId The company ID
     */
    @Transactional
    public void deleteAllCompanyAddresses(Long companyId) {
        log.warn("Deleting all addresses for company {}", companyId);
        validateParentExists(companyId);
        addressRepository.deleteAllByCompanyId(companyId);
        log.info("All addresses deleted for company {}", companyId);
    }

    /**
     * Get primary business address for a company.
     * Returns the default contact address or the first available address.
     *
     * @param companyId The company ID
     * @return The primary business address or null
     */
    @Override
    @Transactional(readOnly = true)
    public AddressResponse getPrimaryBusinessAddress(Long companyId) {
        log.info("Fetching primary business address for company {}", companyId);

        // Try to get default contact address first
        AddressResponse contactAddress = getDefaultContactAddress(companyId);
        if (contactAddress != null) {
            return contactAddress;
        }

        // Fallback to any contact address
        List<AddressResponse> contactAddresses = getContactAddresses(companyId);
        if (!contactAddresses.isEmpty()) {
            return contactAddresses.get(0);
        }

        // Fallback to any address
        List<AddressResponse> allAddresses = getAllAddresses(companyId);
        return allAddresses.isEmpty() ? null : allAddresses.get(0);
    }
}


//@Slf4j
//@Service
/// /@RequiredArgsConstructor
//public class CompanyAddressService extends AbstractAddressService<Company> implements ICompanyAddressService {
//
//    private final CompanyRepository companyRepository;
//
//    public CompanyAddressService(
//            AddressRepository addressRepository,
//            AddressMapper addressMapper,
//            CompanyRepository companyRepository
//    ) {
//        super(addressRepository, addressMapper);  // Call parent
//        this.companyRepository = companyRepository;
//    }
//
//    @Override
//    protected Set<Address> getAddresses(Company parent) {
//        return parent.getAddresses();
//    }
//
//    @Override
//    protected Company loadParentWithAddresses(Long companyId) {
//        return companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
//                .orElseThrow(() -> CompanyNotFoundException.builder()
//                        .message("Company not found: " + companyId)
//                        .build());
//    }
//
//    @Override
//    protected void saveParent(Company company) {
//        companyRepository.save(company);
//    }
//
//    @Override
//    protected void linkAddressToParent(Address address, Company company) {
//        address.setCompany(company);
//    }
//
//    @Override
//    protected CompanyNotFoundException parentNotFoundException(Long id) {
//        return CompanyNotFoundException.builder().message("Company not found: " + id).build();
//    }
//
//    @Override
//    protected Address getAddressByIdAndParentId(Long companyId, Long addressId) {
//        return getAddressEntity(companyId, addressId);
//    }
//}

/*
import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
import com.datasaz.ecommerce.exceptions.CompanyNotFoundException;
import com.datasaz.ecommerce.mappers.AddressMapper;
import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.AddressRepository;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.services.interfaces.ICompanyAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAddressService implements ICompanyAddressService {

    private final CompanyRepository companyRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressResponse addAddress(Long companyId, AddressRequest request) {
        log.info("Adding address for company ID: {}", companyId);
        Company company = companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());

        Address address = addressMapper.toEntity(request);
        address.setCompany(company);
        if (request.isDefault()) {
            unsetOtherDefaultAddresses(company, request.getAddressType());
        }
        address.setDefault(request.isDefault());
        address = addressRepository.save(address);
        company.getAddresses().add(address);
        companyRepository.save(company);
        log.info("Address added successfully for company ID: {}, address ID: {}", companyId, address.getId());
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long companyId, Long addressId, AddressRequest request) {
        log.info("Updating address ID: {} for company ID: {}", addressId, companyId);
        Company company = companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!company.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to company: " + addressId)
                    .build();
        }

        addressMapper.updateEntityFromRequest(request, address);
        if (request.isDefault()) {
            unsetOtherDefaultAddresses(company, request.getAddressType(), addressId);
        }
        address.setDefault(request.isDefault());
        address = addressRepository.save(address);
        log.info("Address ID: {} updated successfully for company ID: {}", addressId, companyId);
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long companyId, Long addressId) {
        log.info("Deleting address ID: {} for company ID: {}", addressId, companyId);
        Company company = companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!company.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to company: " + addressId)
                    .build();
        }

        company.getAddresses().remove(address);
        addressRepository.delete(address);
        companyRepository.save(company);
        log.info("Address ID: {} deleted successfully for company ID: {}", addressId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long companyId, Long addressId) {
        log.info("Retrieving address ID: {} for company ID: {}", addressId, companyId);
        Company company = companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!company.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to company: " + addressId)
                    .build();
        }

        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses(Long companyId) {
        log.info("Retrieving all addresses for company ID: {}", companyId);
        Company company = companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());
        return company.getAddresses().stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public Address getAddressByIdAndByCompanyId(Long companyId, Long addressId) {
        log.info("Retrieving address entity {} for company ID: {}", addressId, companyId);
        Company company = companyRepository.findByIdAndDeletedFalseWithAddresses(companyId)
                .orElseThrow(() -> CompanyNotFoundException.builder()
                        .message("Company not found: " + companyId)
                        .build());
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> AddressNotFoundException.builder()
                        .message("Address not found: " + addressId)
                        .build());

        if (!company.getAddresses().contains(address)) {
            throw AddressNotFoundException.builder()
                    .message("Address does not belong to company: " + addressId)
                    .build();
        }
        return address;
    }

    private void unsetOtherDefaultAddresses(Company company, AddressType addressType, Long... excludeAddressId) {
        company.getAddresses().stream()
                .filter(a -> a.getAddressType() == addressType && a.isDefault() && (excludeAddressId.length == 0 || !a.getId().equals(excludeAddressId[0])))
                .forEach(a -> a.setDefault(false));
    }
}
*/
