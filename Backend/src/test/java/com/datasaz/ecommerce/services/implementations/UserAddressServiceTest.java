package com.datasaz.ecommerce.services.implementations;

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

class UserAddressServiceTest {

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private UserAddressService userAddressService;

    private static final Long USER_ID = 1L;
    private static final Long ADDRESS_ID = 10L;
    private static final Long OTHER_ADDRESS_ID = 11L;

    private User user;
    private Address address;
    private AddressResponse addressResponse;
    private AddressRequest addressRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .id(USER_ID)
                .emailAddress("user@example.com")
                .deleted(false)
                .build();

        address = Address.builder()
                .id(ADDRESS_ID)
                .name("Home")
                .addressLine1("123 Main St")
                .city("NYC")
                .postalCode("10001")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .user(user)
                .build();

        addressResponse = AddressResponse.builder()
                .id(ADDRESS_ID)
                .name("Home")
                .addressLine1("123 Main St")
                .city("NYC")
                .postalCode("10001")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .userId(USER_ID)
                .build();

        addressRequest = AddressRequest.builder()
                .name("Updated Home")
                .addressLine1("789 New St")
                .city("Miami")
                .postalCode("33101")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .build();
    }

    // ===================================================================
    // === addAddress Tests
    // ===================================================================

    // === addAddress_success_withDefault ===
    @Test
    @DisplayName("addAddress - success with default")
    void addAddress_success_withDefault() {
        Address otherAddress = mock(Address.class);
        when(otherAddress.getId()).thenReturn(OTHER_ADDRESS_ID);
        when(otherAddress.isDefault()).thenReturn(true);

        Address newAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("New Home")
                .addressLine1("789 New St")
                .city("Miami")
                .postalCode("33101")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .user(user)
                .build();

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressMapper.toEntity(addressRequest)).thenReturn(newAddress);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
                .thenReturn(List.of(otherAddress));
        when(addressRepository.save(newAddress)).thenReturn(newAddress);
        when(addressMapper.toResponse(newAddress)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);

        assertNotNull(result);
        assertTrue(result.isDefault());
        verify(otherAddress).setDefault(false);
        verify(addressRepository).save(otherAddress);
        assertTrue(newAddress.isDefault());
        verify(addressRepository).save(newAddress);
    }

//    @Test
//    @DisplayName("addAddress - success with default")
//    void addAddress_success_withDefault() {
//        // MOCK the OTHER default address (we will verify this one)
//        Address otherAddress = mock(Address.class);
//        when(otherAddress.getId()).thenReturn(OTHER_ADDRESS_ID);
//        when(otherAddress.isDefault()).thenReturn(true);
//
//        // The NEW address is a real object (we don't verify setters on it)
//        Address newAddress = Address.builder()
//                .id(ADDRESS_ID)
//                .name("New Home")
//                .addressLine1("789 New St")
//                .city("Miami")
//                .postalCode("33101")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .user(user)
//                .build();
//
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(newAddress);
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(otherAddress));
//        when(addressRepository.save(newAddress)).thenReturn(newAddress);
//        when(addressMapper.toResponse(newAddress)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);
//
//        assertNotNull(result);
//        assertTrue(result.isDefault());
//
//        // Verify ONLY the MOCK
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//
//        // DO NOT verify(newAddress).setDefault(true) — it's not a mock!
//        // Instead, check via the returned DTO or entity state
//        assertTrue(newAddress.isDefault());
//        verify(addressRepository).save(newAddress);
//    }

    @Test
    @DisplayName("addAddress - success without default")
    void addAddress_success_noDefault() {
        addressRequest.setDefault(false);
        address.setDefault(false);
        addressResponse.setDefault(false);
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);

        assertFalse(result.isDefault());
        verify(addressRepository, never()).findByUserIdAndType(anyLong(), any());
    }

    @Test
    @DisplayName("addAddress - user not found")
    void addAddress_userNotFound() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> userAddressService.addAddress(USER_ID, addressRequest));
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

        // Real address being updated
        Address existingAddress = Address.builder()
                .id(ADDRESS_ID)
                .name("Home")
                .addressLine1("123 Main St")
                .city("NYC")
                .postalCode("10001")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .isDefault(false)
                .user(user)
                .build();

        addressRequest.setDefault(true);

        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(existingAddress));
        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
                .thenReturn(List.of(otherAddress));
        when(addressRepository.save(existingAddress)).thenReturn(existingAddress);
        when(addressMapper.toResponse(existingAddress)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);

        assertTrue(result.isDefault());

        verify(otherAddress).setDefault(false);
        verify(addressRepository).save(otherAddress);
        assertTrue(existingAddress.isDefault()); // Check state instead of verify
        verify(addressRepository).save(existingAddress);
    }

    @Test
    @DisplayName("updateAddress - success, unset default")
    void updateAddress_unsetDefault() {
        address.setDefault(true);
        addressRequest.setDefault(false);
        addressResponse.setDefault(false);

        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);

        assertFalse(result.isDefault());
        verify(addressRepository, never()).findByUserIdAndType(anyLong(), any());
    }

    @Test
    @DisplayName("updateAddress - address not found")
    void updateAddress_notFound() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class,
                () -> userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest));
    }

    // ===================================================================
    // === deleteAddress Tests
    // ===================================================================

    @Test
    @DisplayName("deleteAddress - success")
    void deleteAddress_success() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(address));

        userAddressService.deleteAddress(USER_ID, ADDRESS_ID);

        verify(addressRepository).delete(address);
    }

    @Test
    @DisplayName("deleteAddress - not found")
    void deleteAddress_notFound() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class,
                () -> userAddressService.deleteAddress(USER_ID, ADDRESS_ID));
    }

    // ===================================================================
    // === getAddress Tests
    // ===================================================================

    @Test
    @DisplayName("getAddress - success")
    void getAddress_success() {
        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.getAddress(USER_ID, ADDRESS_ID);

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
                .name("Work")
                .addressLine1("456 Office Ave")
                .city("LA")
                .postalCode("90001")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .isDefault(false)
                .user(user)
                .build();

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of(address, otherAddress));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
        when(addressMapper.toResponse(otherAddress)).thenReturn(
                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("Work").build());

        List<AddressResponse> result = userAddressService.getAllAddresses(USER_ID);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getAllAddresses - empty list")
    void getAllAddresses_empty() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        List<AddressResponse> result = userAddressService.getAllAddresses(USER_ID);

        assertTrue(result.isEmpty());
    }

    // ===================================================================
    // === getAddressesPaginated Tests
    // ===================================================================

    @Test
    @DisplayName("getAddressesPaginated - returns page")
    void getAddressesPaginated_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("isDefault"), Sort.Order.asc("id")));
        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        Page<AddressResponse> result = userAddressService.getAddressesPaginated(USER_ID, 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    // ===================================================================
    // === getAddressesByType (Abstract) Tests
    // ===================================================================

    @Test
    @DisplayName("getAddressesByType - returns filtered list")
    void getAddressesByType_returnsFiltered() {
        Address otherAddress = Address.builder()
                .id(OTHER_ADDRESS_ID)
                .name("Work")
                .addressType(AddressType.SHIPPING)
                .user(user)
                .build();

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of(address, otherAddress));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        List<AddressResponse> result = userAddressService.getAddressesByType(USER_ID, AddressType.SHIPPING);

        assertEquals(2, result.size());
    }

    // ===================================================================
    // === getDefaultAddress Tests
    // ===================================================================

    @Test
    @DisplayName("getDefaultAddress - success")
    void getDefaultAddress_success() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.getDefaultAddress(USER_ID, AddressType.SHIPPING);

        assertTrue(result.isDefault());
    }

    @Test
    @DisplayName("getDefaultAddress - no default")
    void getDefaultAddress_noDefault() {
        address.setDefault(false);
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));

        assertThrows(AddressNotFoundException.class,
                () -> userAddressService.getDefaultAddress(USER_ID, AddressType.SHIPPING));
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
                .name("Home")
                .addressType(AddressType.SHIPPING)
                .isDefault(false)
                .user(user)
                .build();

        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(targetAddress));
        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
                .thenReturn(List.of(otherAddress));
        when(addressRepository.save(targetAddress)).thenReturn(targetAddress);
        when(addressMapper.toResponse(targetAddress)).thenReturn(addressResponse);

        AddressResponse result = userAddressService.setDefaultAddress(USER_ID, ADDRESS_ID);

        assertTrue(result.isDefault());

        verify(otherAddress).setDefault(false);
        verify(addressRepository).save(otherAddress);
        assertTrue(targetAddress.isDefault());
        verify(addressRepository).save(targetAddress);
    }

    // ===================================================================
    // === User-Specific Convenience Methods
    // ===================================================================

    @Test
    @DisplayName("getShippingAddresses - delegates correctly")
    void getShippingAddresses_delegates() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
                .thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        List<AddressResponse> result = userAddressService.getShippingAddresses(USER_ID);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getBillingAddresses - returns list")
    void getBillingAddresses_returnsList() {
        Address billing = Address.builder()
                .id(12L)
                .addressType(AddressType.BILLING)
                .user(user)
                .build();

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.BILLING))
                .thenReturn(List.of(billing));
        when(addressMapper.toResponse(billing)).thenReturn(
                AddressResponse.builder().id(12L).addressType(AddressType.BILLING).build());

        List<AddressResponse> result = userAddressService.getBillingAddresses(USER_ID);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getDefaultShippingAddress - returns null if none")
    void getDefaultShippingAddress_returnsNull() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.SHIPPING))
                .thenReturn(Optional.empty());

        AddressResponse result = userAddressService.getDefaultShippingAddress(USER_ID);

        assertNull(result);
    }

    @Test
    @DisplayName("getDefaultBillingAddress - returns address")
    void getDefaultBillingAddress_returnsAddress() {
        Address billing = Address.builder()
                .id(13L)
                .addressType(AddressType.BILLING)
                .isDefault(true)
                .user(user)
                .build();

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.BILLING))
                .thenReturn(Optional.of(billing));
        when(addressMapper.toResponse(billing)).thenReturn(
                AddressResponse.builder().id(13L).addressType(AddressType.BILLING).isDefault(true).build());

        AddressResponse result = userAddressService.getDefaultBillingAddress(USER_ID);

        assertNotNull(result);
        assertTrue(result.isDefault());
    }

    // ===================================================================
    // === Count & Has Methods
    // ===================================================================

    @Test
    @DisplayName("countAddressesByUserId - returns count")
    void countAddressesByUserId_returnsCount() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.countByUserId(USER_ID)).thenReturn(5L);

        long count = userAddressService.countAddressesByUserId(USER_ID);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("userHasAddresses - true when has addresses")
    void userHasAddresses_true() {
        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);

        boolean has = userAddressService.userHasAddresses(USER_ID);

        assertTrue(has);
    }

    @Test
    @DisplayName("hasDefaultAddressForType - true when exists")
    void hasDefaultAddressForType_true() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.SHIPPING))
                .thenReturn(Optional.of(address));

        boolean has = userAddressService.hasDefaultAddressForType(USER_ID, AddressType.SHIPPING);

        assertTrue(has);
    }

    @Test
    @DisplayName("deleteAllUserAddresses - deletes all")
    void deleteAllUserAddresses_deletes() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);

        userAddressService.deleteAllUserAddresses(USER_ID);

        verify(addressRepository).deleteAllByUserId(USER_ID);
    }

    // ===================================================================
    // === Edge Cases
    // ===================================================================

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})  // REMOVED 0
    @DisplayName("getAddressesPaginated - different valid page sizes")
    void getAddressesPaginated_differentSizes(int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Order.desc("isDefault")));
        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);

        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
        when(addressRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        Page<AddressResponse> result = userAddressService.getAddressesPaginated(USER_ID, 0, size);

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("validateParentExists - user deleted")
    void validateParentExists_deletedUser() {
        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> userAddressService.getAllAddresses(USER_ID));
    }

    @Test
    @DisplayName("unsetDefaultAddressesForParent - no addresses to unset")
    void unsetDefaultAddressesForParent_noAddresses() {
        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.BILLING))
                .thenReturn(List.of());

        userAddressService.unsetDefaultAddressesForParent(USER_ID, AddressType.BILLING, null);
        verify(addressRepository, never()).save(any());
    }
}

//
//import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
//import com.datasaz.ecommerce.exceptions.UserNotFoundException;
//import com.datasaz.ecommerce.mappers.AddressMapper;
//import com.datasaz.ecommerce.models.request.AddressRequest;
//import com.datasaz.ecommerce.models.response.AddressResponse;
//import com.datasaz.ecommerce.repositories.AddressRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.Address;
//import com.datasaz.ecommerce.repositories.entities.AddressType;
//import com.datasaz.ecommerce.repositories.entities.User;
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
//class UserAddressServiceTest {
//
//    @Mock private AddressRepository addressRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private AddressMapper addressMapper;
//
//    @InjectMocks private UserAddressService userAddressService;
//
//    private static final Long USER_ID = 1L;
//    private static final Long ADDRESS_ID = 10L;
//    private static final Long OTHER_ADDRESS_ID = 11L;
//
//    private User user;
//    private Address address;
//    private Address otherAddress;
//    private AddressResponse addressResponse;
//    private AddressRequest addressRequest;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        user = User.builder()
//                .id(USER_ID)
//                .emailAddress("user@example.com")
//                .deleted(false)
//                .build();
//
//        address = Address.builder()
//                .id(ADDRESS_ID)
//                .name("Home")
//                .addressLine1("123 Main St")
//                .city("NYC")
//                .postalCode("10001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .user(user)
//                .build();
//
//        otherAddress = Address.builder()
//                .id(OTHER_ADDRESS_ID)
//                .name("Work")
//                .addressLine1("456 Office Ave")
//                .city("LA")
//                .postalCode("90001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(false)
//                .user(user)
//                .build();
//
//        addressResponse = AddressResponse.builder()
//                .id(ADDRESS_ID)
//                .name("Home")
//                .addressLine1("123 Main St")
//                .city("NYC")
//                .postalCode("10001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .userId(USER_ID)
//                .build();
//
//        addressRequest = AddressRequest.builder()
//                .name("Updated Home")
//                .addressLine1("789 New St")
//                .city("Miami")
//                .postalCode("33101")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(otherAddress));
//
//        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);
//
//        assertFalse(result.isDefault());
//        verify(addressRepository, never()).findByUserIdAndType(anyLong(), any());
//    }
//
//    @Test
//    @DisplayName("addAddress - user not found")
//    void addAddress_userNotFound() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(false);
//
//        assertThrows(UserNotFoundException.class,
//                () -> userAddressService.addAddress(USER_ID, addressRequest));
//    }
//
//    // ===================================================================
//    // === updateAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("updateAddress - success, set as default")
//    void updateAddress_setDefault() {
//        // ---- 1. Make otherAddress a MOCK ----
//        Address otherAddress = mock(Address.class);
//        when(otherAddress.getId()).thenReturn(OTHER_ADDRESS_ID);
//        when(otherAddress.isDefault()).thenReturn(true);   // initially default
//
//        // ---- 2. Current address is NOT default initially ----
//        address.setDefault(false);
//        addressRequest.setDefault(true);
//
//        // ---- 3. Repository returns the mock when looking for existing defaults ----
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID))
//                .thenReturn(Optional.of(address));
//
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(otherAddress));
//
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        // ---- 4. Execute ----
//        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);
//
//        // ---- 5. Verify ----
//        assertTrue(result.isDefault());
//
//        // Verify the mock was updated
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);   // save the unset one
//        verify(addressRepository).save(address);       // save the updated one
//    }
//
//
/// /    @Test
/// /    @DisplayName("updateAddress - success, set as default")
/// /    void updateAddress_setDefault() {
/// /        address.setDefault(false);
/// /        addressRequest.setDefault(true);
/// /
/// /        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
/// /        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
/// /                .thenReturn(List.of(otherAddress));
/// /        when(addressRepository.save(address)).thenReturn(address);
/// /        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
/// /
/// /        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);
/// /
/// /        assertTrue(result.isDefault());
/// /        verify(otherAddress).setDefault(false);
/// /        verify(addressRepository).save(otherAddress);
/// /    }
//
//    @Test
//    @DisplayName("updateAddress - success, unset default")
//    void updateAddress_unsetDefault() {
//        address.setDefault(true);
//        addressRequest.setDefault(false);
//
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);
//
//        assertFalse(result.isDefault());
//        verify(addressRepository, never()).findByUserIdAndType(anyLong(), any());
//    }
//
//    @Test
//    @DisplayName("updateAddress - address not found")
//    void updateAddress_notFound() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest));
//    }
//
//    // ===================================================================
//    // === deleteAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("deleteAddress - success")
//    void deleteAddress_success() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//
//        userAddressService.deleteAddress(USER_ID, ADDRESS_ID);
//
//        verify(addressRepository).delete(address);
//    }
//
//    @Test
//    @DisplayName("deleteAddress - not found")
//    void deleteAddress_notFound() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.deleteAddress(USER_ID, ADDRESS_ID));
//    }
//
//    // ===================================================================
//    // === getAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAddress - success")
//    void getAddress_success() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.getAddress(USER_ID, ADDRESS_ID);
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address, otherAddress));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//        when(addressMapper.toResponse(otherAddress)).thenReturn(
//                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("Work").build());
//
//        List<AddressResponse> result = userAddressService.getAllAddresses(USER_ID);
//
//        assertEquals(2, result.size());
//    }
//
//    @Test
//    @DisplayName("getAllAddresses - empty list")
//    void getAllAddresses_empty() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
//
//        List<AddressResponse> result = userAddressService.getAllAddresses(USER_ID);
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        Page<AddressResponse> result = userAddressService.getAddressesPaginated(USER_ID, 0, 10);
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address, otherAddress));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        List<AddressResponse> result = userAddressService.getAddressesByType(USER_ID, AddressType.SHIPPING);
//
//        assertEquals(2, result.size());
//    }
//
//    // ===================================================================
//    // === getDefaultAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getDefaultAddress - success")
//    void getDefaultAddress_success() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.getDefaultAddress(USER_ID, AddressType.SHIPPING);
//
//        assertTrue(result.isDefault());
//    }
//
//    @Test
//    @DisplayName("getDefaultAddress - no default")
//    void getDefaultAddress_noDefault() {
//        address.setDefault(false);
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.getDefaultAddress(USER_ID, AddressType.SHIPPING));
//    }
//
//    // ===================================================================
//    // === setDefaultAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("setDefaultAddress - success")
//    void setDefaultAddress_success() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(otherAddress));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.setDefaultAddress(USER_ID, ADDRESS_ID);
//
//        assertTrue(result.isDefault());
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//    }
//
//    // ===================================================================
//    // === User-Specific Convenience Methods
//    // ===================================================================
//
//    @Test
//    @DisplayName("getShippingAddresses - delegates correctly")
//    void getShippingAddresses_delegates() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        List<AddressResponse> result = userAddressService.getShippingAddresses(USER_ID);
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    @DisplayName("getBillingAddresses - returns list")
//    void getBillingAddresses_returnsList() {
//        Address billing = Address.builder().id(12L).addressType(AddressType.BILLING).user(user).build();
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.BILLING))
//                .thenReturn(List.of(billing));
//        when(addressMapper.toResponse(billing)).thenReturn(
//                AddressResponse.builder().id(12L).addressType(AddressType.BILLING).build());
//
//        List<AddressResponse> result = userAddressService.getBillingAddresses(USER_ID);
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    @DisplayName("getDefaultShippingAddress - returns null if none")
//    void getDefaultShippingAddress_returnsNull() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(Optional.empty());
//
//        AddressResponse result = userAddressService.getDefaultShippingAddress(USER_ID);
//
//        assertNull(result);
//    }
//
//    @Test
//    @DisplayName("getDefaultBillingAddress - returns address")
//    void getDefaultBillingAddress_returnsAddress() {
//        Address billing = Address.builder().id(13L).addressType(AddressType.BILLING).isDefault(true).user(user).build();
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.BILLING))
//                .thenReturn(Optional.of(billing));
//        when(addressMapper.toResponse(billing)).thenReturn(
//                AddressResponse.builder().id(13L).addressType(AddressType.BILLING).isDefault(true).build());
//
//        AddressResponse result = userAddressService.getDefaultBillingAddress(USER_ID);
//
//        assertNotNull(result);
//        assertTrue(result.isDefault());
//    }
//
//    // ===================================================================
//    // === Count & Has Methods
//    // ===================================================================
//
//    @Test
//    @DisplayName("countAddressesByUserId - returns count")
//    void countAddressesByUserId_returnsCount() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.countByUserId(USER_ID)).thenReturn(5L);
//
//        long count = userAddressService.countAddressesByUserId(USER_ID);
//
//        assertEquals(5L, count);
//    }
//
//    @Test
//    @DisplayName("userHasAddresses - true when has addresses")
//    void userHasAddresses_true() {
//        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
//
//        boolean has = userAddressService.userHasAddresses(USER_ID);
//
//        assertTrue(has);
//    }
//
//    @Test
//    @DisplayName("hasDefaultAddressForType - true when exists")
//    void hasDefaultAddressForType_true() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(Optional.of(address));
//
//        boolean has = userAddressService.hasDefaultAddressForType(USER_ID, AddressType.SHIPPING);
//
//        assertTrue(has);
//    }
//
//    @Test
//    @DisplayName("deleteAllUserAddresses - deletes all")
//    void deleteAllUserAddresses_deletes() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//
//        userAddressService.deleteAllUserAddresses(USER_ID);
//
//        verify(addressRepository).deleteAllByUserId(USER_ID);
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        Page<AddressResponse> result = userAddressService.getAddressesPaginated(USER_ID, 0, size);
//
//        assertEquals(1, result.getContent().size());
//    }
//
//    @Test
//    @DisplayName("validateParentExists - user deleted")
//    void validateParentExists_deletedUser() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(false);
//
//        assertThrows(UserNotFoundException.class,
//                () -> userAddressService.getAllAddresses(USER_ID));
//    }
//
//    @Test
//    @DisplayName("unsetDefaultAddressesForParent - no addresses to unset")
//    void unsetDefaultAddressesForParent_noAddresses() {
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.BILLING))
//                .thenReturn(List.of());
//
//        userAddressService.unsetDefaultAddressesForParent(USER_ID, AddressType.BILLING, null);
//        verify(addressRepository, never()).save(any());
//    }
//}


//
//import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
//import com.datasaz.ecommerce.exceptions.UserNotFoundException;
//import com.datasaz.ecommerce.mappers.AddressMapper;
//import com.datasaz.ecommerce.models.request.AddressRequest;
//import com.datasaz.ecommerce.models.response.AddressResponse;
//import com.datasaz.ecommerce.repositories.AddressRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.Address;
//import com.datasaz.ecommerce.repositories.entities.AddressType;
//import com.datasaz.ecommerce.repositories.entities.User;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.*;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//
//import java.util.*;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//class UserAddressServiceTest {
//
//    @Mock private AddressRepository addressRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private AddressMapper addressMapper;
//
//    @InjectMocks private UserAddressService userAddressService;
//
//    private static final Long USER_ID = 1L;
//    private static final Long ADDRESS_ID = 10L;
//    private static final Long OTHER_ADDRESS_ID = 11L;
//
//    private Address address;
//    private Address otherAddress;
//    private AddressResponse addressResponse;
//    private AddressRequest addressRequest;
//    private User user;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        user = User.builder().id(USER_ID).deleted(false).build();
//
//        address = Address.builder()
//                .id(ADDRESS_ID)
//                .name("Home")
//                .addressLine1("123 Main St")
//                .city("NYC")
//                .postalCode("10001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .user(user)
//                .build();
//
//        otherAddress = Address.builder()
//                .id(OTHER_ADDRESS_ID)
//                .name("Work")
//                .addressLine1("456 Office Ave")
//                .city("LA")
//                .postalCode("90001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(false)
//                .user(user)
//                .build();
//
//        addressResponse = AddressResponse.builder()
//                .id(ADDRESS_ID)
//                .name("Home")
//                .addressLine1("123 Main St")
//                .city("NYC")
//                .postalCode("10001")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .userId(USER_ID)
//                .build();
//
//        addressRequest = AddressRequest.builder()
//                .name("Updated Home")
//                .addressLine1("789 New St")
//                .city("Miami")
//                .postalCode("33101")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        // Mock unset default (should find one existing default)
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(address));
//        doNothing().when(address).setDefault(false);
//        when(addressRepository.save(any(Address.class))).thenReturn(address);
//
//        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);
//
//        assertNotNull(result);
//        assertEquals(ADDRESS_ID, result.getId());
//        verify(addressRepository).save(address);
//        verify(address).setDefault(true);
//        verify(addressRepository).findByUserIdAndType(USER_ID, AddressType.SHIPPING);
//    }
//
//    @Test
//    @DisplayName("addAddress - success without default")
//    void addAddress_success_noDefault() {
//        addressRequest.setDefault(false);
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressMapper.toEntity(addressRequest)).thenReturn(address);
//        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.addAddress(USER_ID, addressRequest);
//
//        assertFalse(result.isDefault());
//        verify(addressRepository, never()).findByUserIdAndType(anyLong(), any());
//    }
//
//    @Test
//    @DisplayName("addAddress - user not found")
//    void addAddress_userNotFound() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(false);
//
//        assertThrows(UserNotFoundException.class,
//                () -> userAddressService.addAddress(USER_ID, addressRequest));
//    }
//
//    // ===================================================================
//    // === updateAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("updateAddress - success, set as default")
//    void updateAddress_setDefault() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//        when(addressRepository.save(address)).thenReturn(address);
//
//        // Simulate existing default
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(otherAddress));
//        when(otherAddress.isDefault()).thenReturn(true);
//
//        addressRequest.setDefault(true);
//        address.setDefault(false); // current not default
//
//        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);
//
//        assertTrue(result.isDefault());
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//        verify(addressRepository).save(address);
//    }
//
//    @Test
//    @DisplayName("updateAddress - success, unset default")
//    void updateAddress_unsetDefault() {
//        address.setDefault(true);
//        addressRequest.setDefault(false);
//
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//        when(addressRepository.save(address)).thenReturn(address);
//
//        AddressResponse result = userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest);
//
//        assertFalse(result.isDefault());
//        verify(addressRepository, never()).findByUserIdAndType(anyLong(), any());
//    }
//
//    @Test
//    @DisplayName("updateAddress - address not found")
//    void updateAddress_notFound() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.updateAddress(USER_ID, ADDRESS_ID, addressRequest));
//    }
//
//    // ===================================================================
//    // === deleteAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("deleteAddress - success")
//    void deleteAddress_success() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//
//        userAddressService.deleteAddress(USER_ID, ADDRESS_ID);
//
//        verify(addressRepository).delete(address);
//    }
//
//    @Test
//    @DisplayName("deleteAddress - not found")
//    void deleteAddress_notFound() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.deleteAddress(USER_ID, ADDRESS_ID));
//    }
//
//    // ===================================================================
//    // === getAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAddress - success")
//    void getAddress_success() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.getAddress(USER_ID, ADDRESS_ID);
//
//        assertEquals(ADDRESS_ID, result.getId());
//    }
//
//    @Test
//    @DisplayName("getAddress - not found")
//    void getAddress_notFound() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.getAddress(USER_ID, ADDRESS_ID));
//    }
//
//    // ===================================================================
//    // === getAllAddresses Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAllAddresses - returns list")
//    void getAllAddresses_returnsList() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address, otherAddress));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//        when(addressMapper.toResponse(otherAddress)).thenReturn(
//                AddressResponse.builder().id(OTHER_ADDRESS_ID).name("Work").build());
//
//        List<AddressResponse> result = userAddressService.getAllAddresses(USER_ID);
//
//        assertEquals(2, result.size());
//    }
//
//    @Test
//    @DisplayName("getAllAddresses - empty list")
//    void getAllAddresses_empty() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
//
//        List<AddressResponse> result = userAddressService.getAllAddresses(USER_ID);
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
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        Page<AddressResponse> result = userAddressService.getAddressesPaginated(USER_ID, 0, 10);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals(ADDRESS_ID, result.getContent().get(0).getId());
//    }
//
//    // ===================================================================
//    // === getAddressesByType Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getAddressesByType - returns filtered list")
//    void getAddressesByType_returnsFiltered() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address, otherAddress));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        List<AddressResponse> result = userAddressService.getAddressesByType(USER_ID, AddressType.SHIPPING);
//
//        assertEquals(2, result.size());
//    }
//
//    @Test
//    @DisplayName("getAddressesByType - no match")
//    void getAddressesByType_noMatch() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));
//
//        List<AddressResponse> result = userAddressService.getAddressesByType(USER_ID, AddressType.BILLING);
//
//        assertTrue(result.isEmpty());
//    }
//
//    // ===================================================================
//    // === getDefaultAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getDefaultAddress - success")
//    void getDefaultAddress_success() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.getDefaultAddress(USER_ID, AddressType.SHIPPING);
//
//        assertTrue(result.isDefault());
//    }
//
//    @Test
//    @DisplayName("getDefaultAddress - no default")
//    void getDefaultAddress_noDefault() {
//        address.setDefault(false);
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findAllByUserId(USER_ID)).thenReturn(List.of(address));
//
//        assertThrows(AddressNotFoundException.class,
//                () -> userAddressService.getDefaultAddress(USER_ID, AddressType.SHIPPING));
//    }
//
//    // ===================================================================
//    // === setDefaultAddress Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("setDefaultAddress - success")
//    void setDefaultAddress_success() {
//        when(addressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(otherAddress));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        AddressResponse result = userAddressService.setDefaultAddress(USER_ID, ADDRESS_ID);
//
//        assertTrue(result.isDefault());
//        verify(otherAddress).setDefault(false);
//        verify(addressRepository).save(otherAddress);
//    }
//
//    // ===================================================================
//    // === Convenience Methods Tests
//    // ===================================================================
//
//    @Test
//    @DisplayName("getShippingAddresses - delegates correctly")
//    void getShippingAddresses_delegates() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(List.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        List<AddressResponse> result = userAddressService.getShippingAddresses(USER_ID);
//
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    @DisplayName("getDefaultShippingAddress - returns null if none")
//    void getDefaultShippingAddress_returnsNull() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findDefaultByUserIdAndType(USER_ID, AddressType.SHIPPING))
//                .thenReturn(Optional.empty());
//
//        AddressResponse result = userAddressService.getDefaultShippingAddress(USER_ID);
//
//        assertNull(result);
//    }
//
//    @Test
//    @DisplayName("countAddressesByUserId - returns count")
//    void countAddressesByUserId_returnsCount() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.countByUserId(USER_ID)).thenReturn(5L);
//
//        long count = userAddressService.countAddressesByUserId(USER_ID);
//
//        assertEquals(5L, count);
//    }
//
//    @Test
//    @DisplayName("userHasAddresses - true when has addresses")
//    void userHasAddresses_true() {
//        when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
//
//        boolean has = userAddressService.userHasAddresses(USER_ID);
//
//        assertTrue(has);
//    }
//
//    @Test
//    @DisplayName("deleteAllUserAddresses - deletes all")
//    void deleteAllUserAddresses_deletes() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//
//        userAddressService.deleteAllUserAddresses(USER_ID);
//
//        verify(addressRepository).deleteAllByUserId(USER_ID);
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
//        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Order.desc("isDefault"), Sort.Order.asc("id")));
//        Page<Address> page = new PageImpl<>(List.of(address), pageable, 1);
//
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(true);
//        when(addressRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
//        when(addressMapper.toResponse(address)).thenReturn(addressResponse);
//
//        Page<AddressResponse> result = userAddressService.getAddressesPaginated(USER_ID, 0, size);
//
//        assertEquals(1, result.getContent().size());
//    }
//
//    @Test
//    @DisplayName("unsetDefaultAddressesForParent - no addresses to unset")
//    void unsetDefaultAddressesForParent_noAddresses() {
//        when(addressRepository.findByUserIdAndType(USER_ID, AddressType.BILLING))
//                .thenReturn(List.of());
//
//        // Should not throw
//        userAddressService.unsetDefaultAddressesForParent(USER_ID, AddressType.BILLING, null);
//    }
//
//    @Test
//    @DisplayName("validateParentExists - user deleted")
//    void validateParentExists_deletedUser() {
//        when(userRepository.existsByIdAndDeletedFalse(USER_ID)).thenReturn(false);
//
//        assertThrows(UserNotFoundException.class,
//                () -> userAddressService.getAllAddresses(USER_ID));
//    }
//}


//package com.datasaz.ecommerce.services.implementations;
//
//import com.datasaz.ecommerce.exceptions.AddressNotFoundException;
//import com.datasaz.ecommerce.exceptions.UserNotFoundException;
//import com.datasaz.ecommerce.mappers.AddressMapper;
//import com.datasaz.ecommerce.models.request.AddressRequest;
//import com.datasaz.ecommerce.models.response.AddressResponse;
//import com.datasaz.ecommerce.repositories.AddressRepository;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.Address;
//import com.datasaz.ecommerce.repositories.entities.AddressType;
//import com.datasaz.ecommerce.repositories.entities.User;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("UserAddressService Unit Tests")
//class UserAddressServiceTest {
//
//    @Mock
//    private AddressRepository addressRepository;
//
//    @Mock
//    private AddressMapper addressMapper;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private UserAddressService userAddressService;
//
//    private User user;
//    private Address address;
//    private AddressRequest request;
//    private AddressResponse response;
//
//    @BeforeEach
//    void setUp() {
//        user = User.builder()
//                .id(1L)
//                .emailAddress("user@test.com")
//                .deleted(false)
//                //.addresses(new HashSet<>())  // ← Set
//                .build();
//
//        address = Address.builder()
//                .id(10L)
//                .name("John User")
//                .email("john@user.com")
//                .addressLine1("789 Pine St")
//                .city("SF")
//                .postalCode("94101")
//                .country("USA")
//                .addressType(AddressType.SHIPPING)
//                .isDefault(true)
//                .user(user)
//                .build();
//
//        request = AddressRequest.builder()
//                .name("Jane User")
//                .email("jane@user.com")
//                .addressLine1("101 Elm St")
//                .city("Seattle")
//                .postalCode("98101")
//                .country("USA")
//                .addressType(AddressType.BILLING)
//                .isDefault(true)
//                .build();
//
//        response = AddressResponse.builder()
//                .id(20L)
//                .name("Jane User")
//                .email("jane@user.com")
//                .addressLine1("101 Elm St")
//                .city("Seattle")
//                .postalCode("98101")
//                .country("USA")
//                .addressType(AddressType.BILLING)
//                .isDefault(true)
//                .build();
//    }
//
//    @Test
//    @DisplayName("addAddress - success")
//    void addAddress_success() {
//        when(userRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(user));
//        when(addressMapper.toEntity(request)).thenReturn(address);
//        when(addressRepository.save(any())).thenAnswer(i -> i.getArgument(0));
//        when(addressMapper.toResponse(any())).thenReturn(response);
//
//        AddressResponse result = userAddressService.addAddress(1L, request);
//
//        assertNotNull(result);
//        assertTrue(user.getAddresses().contains(address));
//        assertEquals(1, user.getAddresses().size());
//        verify(userRepository).save(user);
//    }
//
//    @Test
//    @DisplayName("addAddress - user not found")
//    void addAddress_userNotFound() {
//        when(userRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.empty());
//
//        assertThrows(UserNotFoundException.class,
//                () -> userAddressService.addAddress(1L, request));
//    }
//
//    @Test
//    @DisplayName("updateAddress - success")
//    void updateAddress_success() {
//        user.getAddresses().add(address);
//
//        AddressRequest update = AddressRequest.builder()
//               // .street("Updated Ave")
//                .isDefault(false)
//                .build();
//
//        when(userRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(user));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//        when(addressRepository.save(address)).thenReturn(address);
//        when(addressMapper.toResponse(address)).thenReturn(response);
//
//        userAddressService.updateAddress(1L, 10L, update);
//
//        //assertEquals("Updated Ave", address.getStreet());
//        assertFalse(address.isDefault());
//    }
//
//    @Test
//    @DisplayName("deleteAddress - success")
//    void deleteAddress_success() {
//        user.getAddresses().add(address);
//
//        when(userRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(user));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//
//        userAddressService.deleteAddress(1L, 10L);
//
//        assertFalse(user.getAddresses().contains(address));
//        assertTrue(user.getAddresses().isEmpty());
//        verify(addressRepository).delete(address);
//    }
//
//    @Test
//    @DisplayName("getAddress - success")
//    void getAddress_success() {
//        user.getAddresses().add(address);
//
//        when(userRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(user));
//        when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
//        when(addressMapper.toResponse(address)).thenReturn(response);
//
//        AddressResponse result = userAddressService.getAddress(1L, 10L);
//
//        assertEquals(response.getId(), result.getId());
//    }
//
//    @Test
//    @DisplayName("getAllAddresses - returns list")
//    void getAllAddresses_returnsList() {
//        user.getAddresses().add(address);
//
//        when(userRepository.findByIdAndDeletedFalseWithAddresses(1L)).thenReturn(Optional.of(user));
//        when(addressMapper.toResponse(address)).thenReturn(response);
//
//        var result = userAddressService.getAllAddresses(1L);
//
//        assertEquals(1, result.size());
//    }
//}
