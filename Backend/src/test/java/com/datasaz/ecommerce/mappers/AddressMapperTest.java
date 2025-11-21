package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressMapper Unit Tests")
class AddressMapperTest {

    @InjectMocks
    private AddressMapper addressMapper;

    private AddressRequest request;
    private Address address;
    private AddressResponse expectedResponse;

    @BeforeEach
    void setUp() {
        request = AddressRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .phoneNumber("+1234567890")
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .reference("Leave at door")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .build();

        address = Address.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .phoneNumber("+1234567890")
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .reference("Leave at door")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .user(User.builder().id(10L).build())
                .company(Company.builder().id(20L).build())
                .version(1L)
                .build();

        expectedResponse = AddressResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .phoneNumber("+1234567890")
                .addressLine1("123 Main St")
                .addressLine2("Apt 4B")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .reference("Leave at door")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .userId(10L)
                .companyId(20L)
                .version(1L)
                .build();
    }

    // === toEntity Tests ===

    @Test
    @DisplayName("toEntity - valid request maps all fields")
    void toEntity_validRequest_mapsAllFields() {
        Address result = addressMapper.toEntity(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(request.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(request.getAddressLine1(), result.getAddressLine1());
        assertEquals(request.getAddressLine2(), result.getAddressLine2());
        assertEquals(request.getCity(), result.getCity());
        assertEquals(request.getState(), result.getState());
        assertEquals(request.getPostalCode(), result.getPostalCode());
        assertEquals(request.getCountry(), result.getCountry());
        assertEquals(request.getReference(), result.getReference());
        assertEquals(request.getAddressType(), result.getAddressType());
        assertEquals(request.isDefault(), result.isDefault());
    }

    @Test
    @DisplayName("toEntity - null request returns null")
    void toEntity_nullRequest_returnsNull() {
        Address result = addressMapper.toEntity(null);
        assertNull(result);
    }

    @Test
    @DisplayName("toEntity - minimal request maps required fields")
    void toEntity_minimalRequest_mapsRequiredFields() {
        AddressRequest minimal = AddressRequest.builder()
                .name("Minimal")
                .addressLine1("123 St")
                .city("City")
                .postalCode("12345")
                .country("Country")
                .addressType(AddressType.BILLING)
                .build();

        Address result = addressMapper.toEntity(minimal);

        assertNotNull(result);
        assertEquals("Minimal", result.getName());
        assertEquals("123 St", result.getAddressLine1());
        assertEquals("City", result.getCity());
        assertEquals("12345", result.getPostalCode());
        assertEquals("Country", result.getCountry());
        assertEquals(AddressType.BILLING, result.getAddressType());
        assertFalse(result.isDefault()); // Default false
        assertNull(result.getEmail()); // Optional
        assertNull(result.getPhoneNumber()); // Optional
        assertNull(result.getAddressLine2()); // Optional
        assertNull(result.getState()); // Optional
        assertNull(result.getReference()); // Optional
    }

    // === toResponse Tests ===

    @Test
    @DisplayName("toResponse - valid address maps all fields")
    void toResponse_validAddress_mapsAllFields() {
        AddressResponse result = addressMapper.toResponse(address);

        assertNotNull(result);
        assertEquals(address.getId(), result.getId());
        assertEquals(address.getName(), result.getName());
        assertEquals(address.getEmail(), result.getEmail());
        assertEquals(address.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(address.getAddressLine1(), result.getAddressLine1());
        assertEquals(address.getAddressLine2(), result.getAddressLine2());
        assertEquals(address.getCity(), result.getCity());
        assertEquals(address.getState(), result.getState());
        assertEquals(address.getPostalCode(), result.getPostalCode());
        assertEquals(address.getCountry(), result.getCountry());
        assertEquals(address.getReference(), result.getReference());
        assertEquals(address.getAddressType(), result.getAddressType());
        assertEquals(address.isDefault(), result.isDefault());
        assertEquals(address.getUser().getId(), result.getUserId());
        assertEquals(address.getCompany().getId(), result.getCompanyId());
        assertEquals(address.getVersion(), result.getVersion());
    }

    @Test
    @DisplayName("toResponse - null address returns null")
    void toResponse_nullAddress_returnsNull() {
        AddressResponse result = addressMapper.toResponse(null);
        assertNull(result);
    }

    @Test
    @DisplayName("toResponse - address with null optional fields")
    void toResponse_nullOptionalFields_excludesNulls() {
        Address minimalAddress = Address.builder()
                .id(2L)
                .name("Test")
                .addressLine1("Test St")
                .city("Test City")
                .postalCode("12345")
                .country("Test Country")
                .addressType(AddressType.BILLING)
                .isDefault(false)
                .build();

        AddressResponse result = addressMapper.toResponse(minimalAddress);

        assertNotNull(result);
        assertEquals("Test", result.getName());
        assertNull(result.getEmail()); // Expected null
        assertNull(result.getPhoneNumber()); // Expected null
        assertNull(result.getAddressLine2()); // Expected null
        assertNull(result.getState()); // Expected null
        assertNull(result.getReference()); // Expected null
        assertNull(result.getUserId()); // Expected null
        assertNull(result.getCompanyId()); // Expected null
        assertNull(result.getVersion()); // Expected null
    }

    // === updateEntityFromRequest Tests ===

    @Test
    @DisplayName("updateEntityFromRequest - updates all fields")
    void updateEntityFromRequest_updatesAllFields() {
        AddressRequest updateRequest = AddressRequest.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+1987654321")
                .addressLine1("Updated St")
                .addressLine2("Updated Apt")
                .city("Updated City")
                .state("Updated State")
                .postalCode("99999")
                .country("Updated Country")
                .reference("Updated ref")
                .addressType(AddressType.BILLING)
                .isDefault(false)
                .build();

        addressMapper.updateEntityFromRequest(updateRequest, address);

        assertEquals("Updated Name", address.getName());
        assertEquals("updated@example.com", address.getEmail());
        assertEquals("+1987654321", address.getPhoneNumber());
        assertEquals("Updated St", address.getAddressLine1());
        assertEquals("Updated Apt", address.getAddressLine2());
        assertEquals("Updated City", address.getCity());
        assertEquals("Updated State", address.getState());
        assertEquals("99999", address.getPostalCode());
        assertEquals("Updated Country", address.getCountry());
        assertEquals("Updated ref", address.getReference());
        assertEquals(AddressType.BILLING, address.getAddressType());
        assertFalse(address.isDefault());
    }

    @Test
    @DisplayName("updateEntityFromRequest - partial update leaves unchanged fields")
    void updateEntityFromRequest_partialUpdate_leavesUnchanged() {
        AddressRequest partialRequest = AddressRequest.builder()
                .name("New Name")
                .addressType(AddressType.CONTACT)
                .isDefault(true)
                .build();

        addressMapper.updateEntityFromRequest(partialRequest, address);

        // Updated fields
        assertEquals("New Name", address.getName());
        assertEquals(AddressType.CONTACT, address.getAddressType());

        // Unchanged fields
        assertEquals("123 Main St", address.getAddressLine1()); // Original
        assertEquals("New York", address.getCity()); // Original
        assertTrue(address.isDefault()); // Original
    }

    @Test
    @DisplayName("updateEntityFromRequest - null request does nothing")
    void updateEntityFromRequest_nullRequest_doesNothing() {
        addressMapper.updateEntityFromRequest(null, address);

        assertEquals("John Doe", address.getName()); // Unchanged
        assertEquals(AddressType.SHIPPING, address.getAddressType()); // Unchanged
        assertTrue(address.isDefault()); // Unchanged
    }

    @Test
    @DisplayName("updateEntityFromRequest - null address does nothing")
    void updateEntityFromRequest_nullAddress_doesNothing() {
        addressMapper.updateEntityFromRequest(request, null);
        // No assertion needed - method should not throw
    }

    @Test
    @DisplayName("updateEntityFromRequest - sets isDefault correctly")
    void updateEntityFromRequest_setsDefaultCorrectly() {
        AddressRequest defaultRequest = AddressRequest.builder()
                .isDefault(true)
                .build();

        addressMapper.updateEntityFromRequest(defaultRequest, address);
        assertTrue(address.isDefault());

        AddressRequest nonDefaultRequest = AddressRequest.builder()
                .isDefault(false)
                .build();

        addressMapper.updateEntityFromRequest(nonDefaultRequest, address);
        assertFalse(address.isDefault());
    }
}
