package com.datasaz.ecommerce.services.implementations;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompanyAddressServiceTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private CompanyAddressService companyAddressService;

    private static final Long COMPANY_ID = 100L;
    private static final Long ADDRESS_ID = 200L;
    private static final Long OTHER_ADDRESS_ID = 201L;

    private Company company;
    private Address address;
    private AddressResponse addressResponse;
    private AddressRequest addressRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        company = Company.builder()
                .id(COMPANY_ID)
                .name("Acme Corp")
                .contactEmail("admin@acme.com")
                .deleted(false)
                .build();

        address = Address.builder()
                .id(ADDRESS_ID)
                .name("Warehouse")
                .addressLine1("101 Factory Rd")
                .city("Chicago")
                .postalCode("60601")
                .country("USA")
                .addressType(AddressType.EXPEDITION)
                .isDefault(true)
                .company(company)
                .build();

        addressResponse = AddressResponse.builder()
                .id(ADDRESS_ID)
                .name("Warehouse")
                .addressLine1("101 Factory Rd")
                .city("Chicago")
                .postalCode("60601")
                .country("USA")
                .addressType(AddressType.EXPEDITION)
                .isDefault(true)
                .companyId(COMPANY_ID)
                .build();

        addressRequest = AddressRequest.builder()
                .name("Updated Warehouse")
                .addressLine1("999 New Rd")
                .city("Dallas")
                .postalCode("75201")
                .country("USA")
                .addressType(AddressType.EXPEDITION)
                .isDefault(true)
                .build();
    }

    // ===================================================================
    // === addAddress Tests
    // ===================================================================

    @Test
    @DisplayName("addAddress - success with default")
    void addAddress_success_withDefault() {
        Address otherAddress = mock(Address.class);
        when(otherAddress.getId()).thenReturn(OTHER_ADDRESS_ID);
        when(otherAddress.isDefault()).thenReturn(true);

        Address newAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("New Warehouse")
                .addressLine1("999 New Rd")
                .city("Dallas")
                .postalCode("75201")
                .country("USA")
                .addressType(AddressType.EXPEDITION)
                .isDefault(true)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressMapper.toEntity(addressRequest)).thenReturn(newAddress);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
                .thenReturn(List.of(otherAddress));
        when(addressRepository.save(newAddress)).thenReturn(newAddress);
        when(addressMapper.toResponse(newAddress)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.addAddress(COMPANY_ID, addressRequest);

        assertTrue(result.isDefault());
        verify(otherAddress).setDefault(false);
        verify(addressRepository).save(otherAddress);
        assertTrue(newAddress.isDefault());
        verify(addressRepository).save(newAddress);
    }

    @Test
    @DisplayName("addAddress - success without default")
    void addAddress_success_noDefault() {
        addressRequest.setDefault(false);
        addressResponse.setDefault(false);

        Address newAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("Non-Default")
                .addressType(AddressType.EXPEDITION)
                .isDefault(false)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressMapper.toEntity(addressRequest)).thenReturn(newAddress);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(addressRepository.save(newAddress)).thenReturn(newAddress);
        when(addressMapper.toResponse(newAddress)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.addAddress(COMPANY_ID, addressRequest);

        assertFalse(result.isDefault());
        verify(addressRepository, never()).findByCompanyIdAndType(anyLong(), any());
    }

    @Test
    @DisplayName("addAddress - company not found")
    void addAddress_companyNotFound() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(false);

        assertThrows(CompanyNotFoundException.class,
                () -> companyAddressService.addAddress(COMPANY_ID, addressRequest));
    }

    // ===================================================================
    // === updateAddress Tests
    // ===================================================================

    @Test
    @DisplayName("updateAddress - success, set as default")
    void updateAddress_setDefault() {
        Address otherAddress = mock(Address.class);
        when(otherAddress.getId()).thenReturn(OTHER_ADDRESS_ID);
        when(otherAddress.isDefault()).thenReturn(true);

        Address existingAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("Warehouse")
                .addressType(AddressType.EXPEDITION)
                .isDefault(false)
                .company(company)
                .build();

        addressRequest.setDefault(true);

        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.of(existingAddress));
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
                .thenReturn(List.of(otherAddress));
        when(addressRepository.save(existingAddress)).thenReturn(existingAddress);
        when(addressMapper.toResponse(existingAddress)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.updateAddress(COMPANY_ID, ADDRESS_ID, addressRequest);

        assertTrue(result.isDefault());
        verify(otherAddress).setDefault(false);
        verify(addressRepository).save(otherAddress);
        assertTrue(existingAddress.isDefault());
        verify(addressRepository).save(existingAddress);
    }

    @Test
    @DisplayName("updateAddress - success, unset default")
    void updateAddress_unsetDefault() {
        Address existingAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("Warehouse")
                .addressType(AddressType.EXPEDITION)
                .isDefault(true)
                .company(company)
                .build();

        addressRequest.setDefault(false);
        addressResponse.setDefault(false);

        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(existingAddress)).thenReturn(existingAddress);
        when(addressMapper.toResponse(existingAddress)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.updateAddress(COMPANY_ID, ADDRESS_ID, addressRequest);

        assertFalse(result.isDefault());
        verify(addressRepository, never()).findByCompanyIdAndType(anyLong(), any());
    }

    @Test
    @DisplayName("updateAddress - address not found")
    void updateAddress_notFound() {
        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class,
                () -> companyAddressService.updateAddress(COMPANY_ID, ADDRESS_ID, addressRequest));
    }

    // ===================================================================
    // === deleteAddress Tests
    // ===================================================================

    @Test
    @DisplayName("deleteAddress - success")
    void deleteAddress_success() {
        Address existingAddress = Address.builder().id(ADDRESS_ID).company(company).build();

        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.of(existingAddress));

        companyAddressService.deleteAddress(COMPANY_ID, ADDRESS_ID);

        verify(addressRepository).delete(existingAddress);
    }

    @Test
    @DisplayName("deleteAddress - not found")
    void deleteAddress_notFound() {
        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class,
                () -> companyAddressService.deleteAddress(COMPANY_ID, ADDRESS_ID));
    }

    // ===================================================================
    // === getAddress Tests
    // ===================================================================

    @Test
    @DisplayName("getAddress - success")
    void getAddress_success() {
        Address existingAddress = Address.builder().id(ADDRESS_ID).company(company).build();

        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.of(existingAddress));
        when(addressMapper.toResponse(existingAddress)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.getAddress(COMPANY_ID, ADDRESS_ID);

        assertEquals(ADDRESS_ID, result.getId());
    }

    // ===================================================================
    // === getAllAddresses Tests
    // ===================================================================

    @Test
    @DisplayName("getAllAddresses - returns list")
    void getAllAddresses_returnsList() {
        Address otherAddress = Address.builder()
                .id(OTHER_ADDRESS_ID)
                .name("HQ")
                .addressType(AddressType.CONTACT)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(address, otherAddress));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
        when(addressMapper.toResponse(otherAddress)).thenReturn(
                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("HQ").build());

        List<AddressResponse> result = companyAddressService.getAllAddresses(COMPANY_ID);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getAllAddresses - empty list")
    void getAllAddresses_empty() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

        List<AddressResponse> result = companyAddressService.getAllAddresses(COMPANY_ID);

        assertTrue(result.isEmpty());
    }

    // ===================================================================
    // === getAddressesPaginated Tests
    // ===================================================================

    @Test
    @DisplayName("getAddressesPaginated - returns page")
    void getAddressesPaginated_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("isDefault")));
        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findByCompanyId(eq(COMPANY_ID), any(Pageable.class))).thenReturn(page);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        Page<AddressResponse> result = companyAddressService.getAddressesPaginated(COMPANY_ID, 0, 10);

        assertEquals(1, result.getTotalElements());
    }


    // ===================================================================
    // === getAddressesByType Tests
    // ===================================================================

    @Test
    @DisplayName("getAddressesByType - returns filtered list")
    void getAddressesByType_returnsFiltered() {
        Address otherAddress = Address.builder()
                .id(OTHER_ADDRESS_ID)
                .addressType(AddressType.CONTACT)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(address, otherAddress));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        List<AddressResponse> result = companyAddressService.getAddressesByType(COMPANY_ID, AddressType.EXPEDITION);

        assertEquals(1, result.size());
    }

    // ===================================================================
    // === getDefaultAddress Tests
    // ===================================================================

    @Test
    @DisplayName("getDefaultAddress - success")
    void getDefaultAddress_success() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.getDefaultAddress(COMPANY_ID, AddressType.EXPEDITION);

        assertTrue(result.isDefault());
    }

    @Test
    @DisplayName("getDefaultAddress - no default")
    void getDefaultAddress_noDefault() {
        Address nonDefault = Address.builder()
                .id(ADDRESS_ID)
                .addressType(AddressType.EXPEDITION)
                .isDefault(false)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(nonDefault));

        assertThrows(AddressNotFoundException.class,
                () -> companyAddressService.getDefaultAddress(COMPANY_ID, AddressType.EXPEDITION));
    }

    // ===================================================================
    // === setDefaultAddress Tests
    // ===================================================================

    @Test
    @DisplayName("setDefaultAddress - success")
    void setDefaultAddress_success() {
        Address otherAddress = mock(Address.class);
        when(otherAddress.getId()).thenReturn(OTHER_ADDRESS_ID);
        when(otherAddress.isDefault()).thenReturn(true);

        Address targetAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("Warehouse")
                .addressType(AddressType.EXPEDITION)
                .isDefault(false)
                .company(company)
                .build();

        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID))
                .thenReturn(Optional.of(targetAddress));
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
                .thenReturn(List.of(otherAddress));
        when(addressRepository.save(targetAddress)).thenReturn(targetAddress);
        when(addressMapper.toResponse(targetAddress)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.setDefaultAddress(COMPANY_ID, ADDRESS_ID);

        assertTrue(result.isDefault());
        verify(otherAddress).setDefault(false);
        verify(addressRepository).save(otherAddress);
        assertTrue(targetAddress.isDefault());
        verify(addressRepository).save(targetAddress);
    }

    // ===================================================================
    // === Company-Specific Convenience Methods
    // ===================================================================

    @Test
    @DisplayName("getExpeditionAddresses - delegates correctly")
    void getExpeditionAddresses_delegates() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
                .thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        List<AddressResponse> result = companyAddressService.getExpeditionAddresses(COMPANY_ID);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getBillingAddresses - returns list")
    void getBillingAddresses_returnsList() {
        Address billing = Address.builder()
                .id(202L)
                .addressType(AddressType.BILLING)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.BILLING))
                .thenReturn(List.of(billing));
        when(addressMapper.toResponse(billing)).thenReturn(
                AddressResponse.builder().id(202L).addressType(AddressType.BILLING).build());

        List<AddressResponse> result = companyAddressService.getBillingAddresses(COMPANY_ID);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getContactAddresses - returns list")
    void getContactAddresses_returnsList() {
        Address contact = Address.builder()
                .id(203L)
                .addressType(AddressType.CONTACT)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
                .thenReturn(List.of(contact));
        when(addressMapper.toResponse(contact)).thenReturn(
                AddressResponse.builder().id(203L).addressType(AddressType.CONTACT).build());

        List<AddressResponse> result = companyAddressService.getContactAddresses(COMPANY_ID);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getDefaultExpeditionAddress - returns null if none")
    void getDefaultExpeditionAddress_returnsNull() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
                .thenReturn(Optional.empty());

        AddressResponse result = companyAddressService.getDefaultExpeditionAddress(COMPANY_ID);

        assertNull(result);
    }

    @Test
    @DisplayName("getDefaultBillingAddress - returns address")
    void getDefaultBillingAddress_returnsAddress() {
        Address billing = Address.builder()
                .id(204L)
                .addressType(AddressType.BILLING)
                .isDefault(true)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.BILLING))
                .thenReturn(Optional.of(billing));
        when(addressMapper.toResponse(billing)).thenReturn(
                AddressResponse.builder().id(204L).addressType(AddressType.BILLING).isDefault(true).build());

        AddressResponse result = companyAddressService.getDefaultBillingAddress(COMPANY_ID);

        assertNotNull(result);
        assertTrue(result.isDefault());
    }

    // ===================================================================
    // === getPrimaryBusinessAddress Tests
    // ===================================================================

    @Test
    @DisplayName("getPrimaryBusinessAddress - default contact first")
    void getPrimaryBusinessAddress_defaultContact() {
        Address defaultContact = Address.builder()
                .id(205L)
                .addressType(AddressType.CONTACT)
                .isDefault(true)
                .company(company)
                .build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
                .thenReturn(Optional.of(defaultContact));
        when(addressMapper.toResponse(defaultContact)).thenReturn(
                AddressResponse.builder().id(205L).addressType(AddressType.CONTACT).isDefault(true).build());

        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);

        assertEquals(205L, result.getId());
    }

    @Test
    @DisplayName("getPrimaryBusinessAddress - fallback to any contact")
    void getPrimaryBusinessAddress_fallbackContact() {
        Address contact = Address.builder().id(206L).addressType(AddressType.CONTACT).company(company).build();

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
                .thenReturn(Optional.empty());
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
                .thenReturn(List.of(contact));
        when(addressMapper.toResponse(contact)).thenReturn(
                AddressResponse.builder().id(206L).name("HQ").build());

        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);

        assertEquals(206L, result.getId());
    }

    @Test
    @DisplayName("getPrimaryBusinessAddress - fallback to any address")
    void getPrimaryBusinessAddress_fallbackAny() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(anyLong(), any())).thenReturn(Optional.empty());
        when(addressRepository.findByCompanyIdAndType(anyLong(), eq(AddressType.CONTACT))).thenReturn(List.of());
        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);

        assertEquals(ADDRESS_ID, result.getId());
    }

    @Test
    @DisplayName("getPrimaryBusinessAddress - returns null if no addresses")
    void getPrimaryBusinessAddress_noAddresses() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(anyLong(), any())).thenReturn(Optional.empty());
        when(addressRepository.findByCompanyIdAndType(anyLong(), any())).thenReturn(List.of());
        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());

        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);

        assertNull(result);
    }

    // ===================================================================
    // === Count & Has Methods
    // ===================================================================

    @Test
    @DisplayName("countAddressesByCompanyId - returns count")
    void countAddressesByCompanyId_returnsCount() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.countByCompanyId(COMPANY_ID)).thenReturn(7L);

        long count = companyAddressService.countAddressesByCompanyId(COMPANY_ID);

        assertEquals(7L, count);
    }

    @Test
    @DisplayName("companyHasAddresses - true when has addresses")
    void companyHasAddresses_true() {
        when(addressRepository.countByCompanyId(COMPANY_ID)).thenReturn(1L);
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);


        boolean has = companyAddressService.companyHasAddresses(COMPANY_ID);

        assertTrue(has);
    }

    @Test
    @DisplayName("hasDefaultAddressForType - true when exists")
    void hasDefaultAddressForType_true() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
                .thenReturn(Optional.of(address));

        boolean has = companyAddressService.hasDefaultAddressForType(COMPANY_ID, AddressType.EXPEDITION);

        assertTrue(has);
    }

    @Test
    @DisplayName("deleteAllCompanyAddresses - deletes all")
    void deleteAllCompanyAddresses_deletes() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);

        companyAddressService.deleteAllCompanyAddresses(COMPANY_ID);

        verify(addressRepository).deleteAllByCompanyId(COMPANY_ID);
    }

    // ===================================================================
    // === Edge Cases
    // ===================================================================

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})
    @DisplayName("getAddressesPaginated - different valid page sizes")
    void getAddressesPaginated_differentSizes(int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Order.desc("isDefault")));
        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);

        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
        when(addressRepository.findByCompanyId(eq(COMPANY_ID), any(Pageable.class))).thenReturn(page);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        Page<AddressResponse> result = companyAddressService.getAddressesPaginated(COMPANY_ID, 0, size);

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }


    @Test
    @DisplayName("validateParentExists - company deleted")
    void validateParentExists_deletedCompany() {
        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(false);

        assertThrows(CompanyNotFoundException.class,
                () -> companyAddressService.getAllAddresses(COMPANY_ID));
    }

    @Test
    @DisplayName("unsetDefaultAddressesForParent - no addresses to unset")
    void unsetDefaultAddressesForParent_noAddresses() {
        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.BILLING))
                .thenReturn(List.of());

        companyAddressService.unsetDefaultAddressesForParent(COMPANY_ID, AddressType.BILLING, null);
        verify(addressRepository, never()).save(any());
    }
}

//import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
//import com.datasaz.ecommerce.exceptions.CompanyNotFoundException;
//import com.datasaz.ecommerce.mappers.AddressMapper;
//import com.datasaz.ecommerce.models.request.AddressRequest;
//import com.datasaz.ecommerce.models.response.AddressResponse;
//import com.datasaz.ecommerce.repositories.AddressRepository;
//import com.datasaz.ecommerce.repositories.CompanyRepository;
//import com.datasaz.ecommerce.repositories.entities.Address;
//import com.datasaz.ecommerce.repositories.entities.AddressType;
//import com.datasaz.ecommerce.repositories.entities.Company;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//class CompanyAddressServiceTest {
//
//    @Mock private AddressRepository addressRepository;
//    @Mock private CompanyRepository companyRepository;
//    @Mock private AddressMapper addressMapper;
//
//    @InjectMocks private CompanyAddressService companyAddressService;
//
//    private static final Long COMPANY_ID = 100L;
//    private static final Long ADDRESS_ID = 200L;
//    private static final Long OTHER_ADDRESS_ID = 201L;
//
//    private Company company;
//    private Address address;
//    private Address otherAddress;
//    private AddressResponse addressResponse;
//    private AddressRequest addressRequest;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        company = Company.builder()
//                .id(COMPANY_ID)
//                .name("Acme Corp")
//                .deleted(false)
//                .build();
//
//        address = Address.builder()
//                .id(ADDRESS_ID)
//                .name("Warehouse")
//                .addressLine1("101 Factory Rd")
//                .city("Chicago")
//                .postalCode("60601")
//                .country("USA")
//                .addressType(AddressType.EXPEDITION)
//                .isDefault(true)
//                .company(company)
//                .build();
//
//        otherAddress = Address.builder()
//                .id(OTHER_ADDRESS_ID)
//                .name("HQ")
//                .addressLine1("1 Corporate Plaza")
//                .city("Austin")
//                .postalCode("78701")
//                .country("USA")
//                .addressType(AddressType.CONTACT)
//                .isDefault(false)
//                .company(company)
//                .build();
//
//        addressResponse = AddressResponse.builder()
//                .id(ADDRESS_ID)
//                .name("Warehouse")
//                .addressLine1("101 Factory Rd")
//                .city("Chicago")
//                .postalCode("60601")
//                .country("USA")
//                .addressType(AddressType.EXPEDITION)
//                .isDefault(true)
//                .companyId(COMPANY_ID)
//                .build();
//
//        addressRequest = AddressRequest.builder()
//                .name("Updated Warehouse")
//                .addressLine1("999 New Rd")
//                .city("Dallas")
//                .postalCode("75201")
//                .country("USA")
//                .addressType(AddressType.EXPEDITION)
//                .isDefault(true)
//                .build();
//    }
//
//    // ===================================================================
//    // === addAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("addAddress - success with default")
//    void addAddress_success_withDefault() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
//        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
//                .thenReturn(List.of(otherAddress));
//
//        AddressResponse result = companyAddressService.addAddress(COMPANY_ID, addressRequest);
//
//        assertNotNull(result);
//        assertEquals(ADDRESS_ID, result.getId());
//        verify(address).setDefault(true);
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//        verify(addressRepository).save(address);
//    }
//
//    @Test
//    @DisplayName("addAddress - success without default")
//    void addAddress_success_noDefault() {
//        addressRequest.setDefault(false);
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
//        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.addAddress(COMPANY_ID, addressRequest);
//
//        assertFalse(result.isDefault());
//        verify(addressRepository, never()).findByCompanyIdAndType(anyLong(), any());
//    }
//
//    @Test
//    @DisplayName("addAddress - company not found")
//    void addAddress_companyNotFound() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(false);
//
//        assertThrows(CompanyNotFoundException.class,
//                () -> companyAddressService.addAddress(COMPANY_ID, addressRequest));
//    }
//
//    // ===================================================================
//    // === updateAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("updateAddress - success, set as default")
//    void updateAddress_setDefault() {
//        address.setDefault(false);
//        addressRequest.setDefault(true);
//
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.of(address));
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
//                .thenReturn(List.of(otherAddress));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.updateAddress(COMPANY_ID, ADDRESS_ID, addressRequest);
//
//        assertTrue(result.isDefault());
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//    }
//
//    @Test
//    @DisplayName("updateAddress - success, unset default")
//    void updateAddress_unsetDefault() {
//        address.setDefault(true);
//        addressRequest.setDefault(false);
//
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.of(address));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.updateAddress(COMPANY_ID, ADDRESS_ID, addressRequest);
//
//        assertFalse(result.isDefault());
//        verify(addressRepository, never()).findByCompanyIdAndType(anyLong(), any());
//    }
//
//    @Test
//    @DisplayName("updateAddress - address not found")
//    void updateAddress_notFound() {
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> companyAddressService.updateAddress(COMPANY_ID, ADDRESS_ID, addressRequest));
//    }
//
//    // ===================================================================
//    // === deleteAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("deleteAddress - success")
//    void deleteAddress_success() {
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.of(address));
//
//        companyAddressService.deleteAddress(COMPANY_ID, ADDRESS_ID);
//
//        verify(addressRepository).delete(address);
//    }
//
//    @Test
//    @DisplayName("deleteAddress - not found")
//    void deleteAddress_notFound() {
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> companyAddressService.deleteAddress(COMPANY_ID, ADDRESS_ID));
//    }
//
//    // ===================================================================
//    // === getAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAddress - success")
//    void getAddress_success() {
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.getAddress(COMPANY_ID, ADDRESS_ID);
//
//        assertEquals(ADDRESS_ID, result.getId());
//    }
//
//    // ===================================================================
//    // === getAllAddresses Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAllAddresses - returns list")
//    void getAllAddresses_returnsList() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address, otherAddress));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//        when(addressMapper.toResponse(otherAddress)).thenReturn(
//                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("HQ").build());
//
//        List<AddressResponse> result = companyAddressService.getAllAddresses(COMPANY_ID);
//
//        assertEquals(2, result.size());
//    }
//
//    @Test
//    @DisplayName("getAllAddresses - empty list")
//    void getAllAddresses_empty() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());
//
//        List<AddressResponse> result = companyAddressService.getAllAddresses(COMPANY_ID);
//
//        assertTrue(result.isEmpty());
//    }
//
//    // ===================================================================
//    // === getAddressesPaginated Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAddressesPaginated - returns page")
//    void getAddressesPaginated_returnsPage() {
//        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("isDefault"), Sort.Order.asc("id")));
//        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);
//
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findByCompanyId(COMPANY_ID, pageable)).thenReturn(page);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        Page<AddressResponse> result = companyAddressService.getAddressesPaginated(COMPANY_ID, 0, 10);
//
//        assertEquals(1, result.getTotalElements());
//    }
//
//    // ===================================================================
//    // === getAddressesByType (Abstract) Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAddressesByType - returns filtered list")
//    void getAddressesByType_returnsFiltered() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address, otherAddress));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        List<AddressResponse> result = companyAddressService.getAddressesByType(COMPANY_ID, AddressType.EXPEDITION);
//
//        assertEquals(1, result.size());
//    }
//
//    // ===================================================================
//    // === getDefaultAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getDefaultAddress - success")
//    void getDefaultAddress_success() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.getDefaultAddress(COMPANY_ID, AddressType.EXPEDITION);
//
//        assertTrue(result.isDefault());
//    }
//
//    @Test
//    @DisplayName("getDefaultAddress - no default")
//    void getDefaultAddress_noDefault() {
//        address.setDefault(false);
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address));
//
//        assertThrows(AddressNotFoundException.class,
//                () -> companyAddressService.getDefaultAddress(COMPANY_ID, AddressType.EXPEDITION));
//    }
//
//    // ===================================================================
//    // === setDefaultAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("setDefaultAddress - success")
//    void setDefaultAddress_success() {
//        when(addressRepository.findByIdAndCompanyId(ADDRESS_ID, COMPANY_ID)).thenReturn(Optional.of(address));
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
//                .thenReturn(List.of(otherAddress));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.setDefaultAddress(COMPANY_ID, ADDRESS_ID);
//
//        assertTrue(result.isDefault());
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//    }
//
//    // ===================================================================
//    // === Company-Specific Convenience Methods
//    // ===================================================================
//
//    @Test
//    @DisplayName("getExpeditionAddresses - delegates correctly")
//    void getExpeditionAddresses_delegates() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
//                .thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        List<AddressResponse> result = companyAddressService.getExpeditionAddresses(COMPANY_ID);
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    @DisplayName("getBillingAddresses - returns list")
//    void getBillingAddresses_returnsList() {
//        Address billing = Address.builder().id(202L).addressType(AddressType.BILLING).company(company).build();
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.BILLING))
//                .thenReturn(List.of(billing));
//        when(addressMapper.toResponse(billing)).thenReturn(
//                AddressResponse.builder().id(202L).addressType(AddressType.BILLING).build());
//
//        List<AddressResponse> result = companyAddressService.getBillingAddresses(COMPANY_ID);
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    @DisplayName("getContactAddresses - returns list")
//    void getContactAddresses_returnsList() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
//                .thenReturn(List.of(otherAddress));
//        when(addressMapper.toResponse(otherAddress)).thenReturn(
//                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("HQ").build());
//
//        List<AddressResponse> result = companyAddressService.getContactAddresses(COMPANY_ID);
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    @DisplayName("getDefaultExpeditionAddress - returns null if none")
//    void getDefaultExpeditionAddress_returnsNull() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
//                .thenReturn(Optional.empty());
//
//        AddressResponse result = companyAddressService.getDefaultExpeditionAddress(COMPANY_ID);
//
//        assertNull(result);
//    }
//
//    @Test
//    @DisplayName("getDefaultBillingAddress - returns address")
//    void getDefaultBillingAddress_returnsAddress() {
//        Address billing = Address.builder().id(203L).addressType(AddressType.BILLING).isDefault(true).company(company).build();
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.BILLING))
//                .thenReturn(Optional.of(billing));
//        when(addressMapper.toResponse(billing)).thenReturn(
//                AddressResponse.builder().id(203L).addressType(AddressType.BILLING).isDefault(true).build());
//
//        AddressResponse result = companyAddressService.getDefaultBillingAddress(COMPANY_ID);
//
//        assertNotNull(result);
//        assertTrue(result.isDefault());
//    }
//
//    // ===================================================================
//    // === getPrimaryBusinessAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getPrimaryBusinessAddress - default contact first")
//    void getPrimaryBusinessAddress_defaultContact() {
//        Address defaultContact = Address.builder().id(204L).addressType(AddressType.CONTACT).isDefault(true).company(company).build();
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
//                .thenReturn(Optional.of(defaultContact));
//        when(addressMapper.toResponse(defaultContact)).thenReturn(
//                AddressResponse.builder().id(204L).addressType(AddressType.CONTACT).isDefault(true).build());
//
//        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);
//
//        assertEquals(204L, result.getId());
//    }
//
//    @Test
//    @DisplayName("getPrimaryBusinessAddress - fallback to any contact")
//    void getPrimaryBusinessAddress_fallbackContact() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
//                .thenReturn(Optional.empty());
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
//                .thenReturn(List.of(otherAddress));
//        when(addressMapper.toResponse(otherAddress)).thenReturn(
//                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("HQ").build());
//
//        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);
//
//        assertEquals(OTHER_ADDRESS_ID, result.getId());
//    }
//
//    @Test
//    @DisplayName("getPrimaryBusinessAddress - fallback to any address")
//    void getPrimaryBusinessAddress_fallbackAny() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
//                .thenReturn(Optional.empty());
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.CONTACT))
//                .thenReturn(List.of());
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);
//
//        assertEquals(ADDRESS_ID, result.getId());
//    }
//
//    @Test
//    @DisplayName("getPrimaryBusinessAddress - returns null if no addresses")
//    void getPrimaryBusinessAddress_noAddresses() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(anyLong(), any())).thenReturn(Optional.empty());
//        when(addressRepository.findByCompanyIdAndType(anyLong(), any())).thenReturn(List.of());
//        when(addressRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());
//
//        AddressResponse result = companyAddressService.getPrimaryBusinessAddress(COMPANY_ID);
//
//        assertNull(result);
//    }
//
//    // ===================================================================
//    // === Count & Has Methods
//    // ===================================================================
//
//    @Test
//    @DisplayName("countAddressesByCompanyId - returns count")
//    void countAddressesByCompanyId_returnsCount() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.countByCompanyId(COMPANY_ID)).thenReturn(7L);
//
//        long count = companyAddressService.countAddressesByCompanyId(COMPANY_ID);
//
//        assertEquals(7L, count);
//    }
//
//    @Test
//    @DisplayName("companyHasAddresses - true when has addresses")
//    void companyHasAddresses_true() {
//        when(addressRepository.countByCompanyId(COMPANY_ID)).thenReturn(1L);
//
//        boolean has = companyAddressService.companyHasAddresses(COMPANY_ID);
//
//        assertTrue(has);
//    }
//
//    @Test
//    @DisplayName("hasDefaultAddressForType - true when exists")
//    void hasDefaultAddressForType_true() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByCompanyIdAndType(COMPANY_ID, AddressType.EXPEDITION))
//                .thenReturn(Optional.of(address));
//
//        boolean has = companyAddressService.hasDefaultAddressForType(COMPANY_ID, AddressType.EXPEDITION);
//
//        assertTrue(has);
//    }
//
//    @Test
//    @DisplayName("deleteAllCompanyAddresses - deletes all")
//    void deleteAllCompanyAddresses_deletes() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//
//        companyAddressService.deleteAllCompanyAddresses(COMPANY_ID);
//
//        verify(addressRepository).deleteAllByCompanyId(COMPANY_ID);
//    }
//
//    // ===================================================================
//    // === Edge Cases
//    // ===================================================================
//
//    @ParameterizedTest
//    @ValueSource(ints = {0, 1, 5, 100})
//    @DisplayName("getAddressesPaginated - different page sizes")
//    void getAddressesPaginated_differentSizes(int size) {
//        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Order.desc("isDefault")));
//        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);
//
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(true);
//        when(addressRepository.findByCompanyId(COMPANY_ID, pageable)).thenReturn(page);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        Page<AddressResponse> result = companyAddressService.getAddressesPaginated(COMPANY_ID, 0, size);
//
//        assertEquals(1, result.getContent().size());
//    }
//
//    @Test
//    @DisplayName("validateParentExists - company deleted")
//    void validateParentExists_deletedCompany() {
//        when(companyRepository.existsByIdAndDeletedFalse(COMPANY_ID)).thenReturn(false);
//
//        assertThrows(CompanyNotFoundException.class,
//                () -> companyAddressService.getAllAddresses(COMPANY_ID));
//    }
//
//    @Test
//    @DisplayName("unsetDefaultAddressesForParent - no addresses to unset")
//    void unsetDefaultAddressesForParent_noAddresses() {
//        when(addressRepository.findByCompanyIdAndType(COMPANY_ID, AddressType.BILLING))
//                .thenReturn(List.of());
//
//        companyAddressService.unsetDefaultAddressesForParent(COMPANY_ID, AddressType.BILLING, null);
//        verify(addressRepository, never()).save(any());
//    }
//}


//import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
//import com.datasaz.ecommerce.exceptions.CompanyNotFoundException;
//import com.datasaz.ecommerce.mappers.AddressMapper;
//import com.datasaz.ecommerce.models.request.AddressRequest;
//import com.datasaz.ecommerce.models.response.AddressResponse;
//import com.datasaz.ecommerce.repositories.AddressRepository;
//import com.datasaz.ecommerce.repositories.CompanyRepository;
//import com.datasaz.ecommerce.repositories.entities.Address;
//import com.datasaz.ecommerce.repositories.entities.AddressType;
//import com.datasaz.ecommerce.repositories.entities.Company;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;

//@ExtendWith(MockitoExtension.class)
//@DisplayName("CompanyAddressService Unit Tests")
//class CompanyAddressServiceTest {
//
//    @Mock
//    private AddressRepository addressRepository;
//
//    @Mock
//    private AddressMapper addressMapper;
//
//    @Mock
//    private CompanyRepository companyRepository;
//
//    @InjectMocks
//    private CompanyAddressService companyAddressService;
//
//    private Company company;
//    private Address address;
//    private AddressRequest request;
//    private AddressResponse response;
//
//    @BeforeEach
//    void setUp() {
//        company = Company.builder()
//                .id(1L)
//                .name("Test Corp")
//                .contactEmail("admin@test.com")
//                .deleted(false)
//                //.addresses(new HashSet<>())  // ← Set
//                .build();
//
//        address = Address.builder()
//                .id(10L)
//                .name("John Doe")
//                .email("john@example.com")
//                .addressLine1("123 Main St")
//                .city("NYC")
//                .postalCode("10001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .company(company)
//                .build();
//
//        request = AddressRequest.builder()
//                .name("Jane Smith")
//                .email("jane@example.com")
//                .addressLine1("456 Oak Ave")
//                .city("LA")
//                .postalCode("90001")
//                .country("USA")
//                .addressType(AddressType.BILLING)
//                .isDefault(true)
//                .build();
//
//        response = AddressResponse.builder()
//                .id(20L)
//                .name("Jane Smith")
//                .email("jane@example.com")
//                .addressLine1("456 Oak Ave")
//                .city("LA")
//                .postalCode("90001")
//                .country("USA")
//                .addressType(AddressType.BILLING)
//                .isDefault(true)
//                .build();
//    }
//
//    @Test
//    @DisplayName("addAddress - success")
//    void addAddress_success() {
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressMapper.toEntity(request)).thenReturn(address);
//        when(addressRepository.save(any(Address.class))).thenAnswer(i -> i.getArgument(0));
//        when(addressMapper.toResponse(any(Address.class))).thenReturn(response);
//
//        AddressResponse result = companyAddressService.addAddress(1L, request);
//
//        assertNotNull(result);
//        assertTrue(company.getAddresses().contains(address));  // Set.contains()
//        assertEquals(1, company.getAddresses().size());
//        verify(companyRepository).save(company);
//    }
//
//    @Test
//    @DisplayName("addAddress - company not found")
//    void addAddress_companyNotFound() {
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.empty());
//
//        assertThrows(CompanyNotFoundException.class,
//                () -> companyAddressService.addAddress(1L, request));
//    }
//
//    @Test
//    @DisplayName("addAddress - unsets previous default of same type")
//    void addAddress_unsetsPreviousDefault() {
//        Address oldDefault = Address.builder()
//                .id(5L)
//                .name("Old")
//                .addressType(AddressType.BILLING)
//                .isDefault(true)
//                .company(company)
//                .build();
//        company.getAddresses().add(oldDefault);
//
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressMapper.toEntity(request)).thenReturn(address);
//        when(addressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//        when(addressMapper.toResponse(any())).thenReturn(response);
//
//        companyAddressService.addAddress(1L, request);
//
//        assertFalse(oldDefault.isDefault());
//        assertTrue(address.isDefault());
//        assertEquals(2, company.getAddresses().size());
//    }
//
//    @Test
//    @DisplayName("updateAddress - success")
//    void updateAddress_success() {
//        company.getAddresses().add(address);
//        AddressRequest updateRequest = AddressRequest.builder()
//                //.street("Updated St")
//                .isDefault(false)
//                .build();
//
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(response);
//
//        AddressResponse result = companyAddressService.updateAddress(1L, 10L, updateRequest);
//
//        //assertEquals("Updated St", address.getStreet());
//        assertFalse(address.isDefault());
//        verify(addressMapper).updateEntityFromRequest(updateRequest, address);
//    }
//
//    @Test
//    @DisplayName("updateAddress - address not in company")
//    void updateAddress_addressNotInCompany() {
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//
//        assertThrows(AddressNotFoundException.class,
//                () -> companyAddressService.updateAddress(1L, 10L, request));
//    }
//
//    @Test
//    @DisplayName("deleteAddress - success")
//    void deleteAddress_success() {
//        company.getAddresses().add(address);
//
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//
//        companyAddressService.deleteAddress(1L, 10L);
//
//        assertFalse(company.getAddresses().contains(address));
//        assertTrue(company.getAddresses().isEmpty());  // Set.isEmpty()
//        verify(addressRepository).delete(address);
//    }
//
//    @Test
//    @DisplayName("getAddress - success")
//    void getAddress_success() {
//        company.getAddresses().add(address);
//
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(response);
//
//        AddressResponse result = companyAddressService.getAddress(1L, 10L);
//
//        assertEquals(response.getId(), result.getId());
//    }
//
//    @Test
//    @DisplayName("getAllAddresses - returns list")
//    void getAllAddresses_returnsList() {
//        company.getAddresses().add(address);
//
//        when(companyRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(company));
//        when(addressMapper.toResponse(address)).thenReturn(response);
//
//        List<AddressResponse> result = companyAddressService.getAllAddresses(1L);
//
//        assertEquals(1, result.size());
//        assertEquals(response, result.get(0));
//    }
//}
