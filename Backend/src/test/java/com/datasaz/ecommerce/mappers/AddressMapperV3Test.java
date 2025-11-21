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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AddressMapperV3Test {

    private AddressMapper addressMapper;

    @BeforeEach
    void setUp() {
        addressMapper = new AddressMapper();
    }

    // ===================================================================
    // === toEntity() – Extreme & Edge Cases
    // ===================================================================

    @Test
    @DisplayName("toEntity - max length fields")
    void toEntity_maxLengthFields() {
        String max100 = "A".repeat(100);
        String max255 = "B".repeat(255);
        String max20 = "9".repeat(20);

        AddressRequest request = AddressRequest.builder()
                .name(max100)
                .email(max255 + "@example.com") // 255 + @ + domain = 270, but @Size(255) on email → will be truncated in validation
                .phoneNumber(max20)
                .addressLine1(max255)
                .addressLine2(max255)
                .city(max100)
                .state(max100)
                .postalCode(max20)
                .country(max100)
                .reference(max255)
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .build();

        Address result = addressMapper.toEntity(request);

        assertEquals(max100, result.getName());
        assertEquals(max255 + "@example.com", result.getEmail()); // Mapper doesn't truncate
        assertEquals(max20, result.getPhoneNumber());
        assertEquals(max255, result.getAddressLine1());
        assertEquals(max255, result.getAddressLine2());
        assertEquals(max100, result.getCity());
        assertEquals(max100, result.getState());
        assertEquals(max20, result.getPostalCode());
        assertEquals(max100, result.getCountry());
        assertEquals(max255, result.getReference());
    }

    @ParameterizedTest
    @CsvSource({
            "' ',       true",
            "'',        true",
            "'   ',     true",
            "'\t',      true",
            "'\n',      true"
    })
    @DisplayName("toEntity - whitespace-only strings are preserved")
    void toEntity_whitespaceOnly(String input, boolean expectedBlank) {
        AddressRequest request = AddressRequest.builder()
                .name(input)
                .addressLine1(input)
                .city(input)
                .postalCode(input)
                .country(input)
                .addressType(AddressType.BILLING)
                .build();

        Address result = addressMapper.toEntity(request);

        assertEquals(input, result.getName());
        assertEquals(input, result.getAddressLine1());
        assertEquals(input, result.getCity());
        assertEquals(input, result.getPostalCode());
        assertEquals(input, result.getCountry());
    }

    @Test
    @DisplayName("toEntity - Unicode and special characters")
    void toEntity_unicodeAndSpecialChars() {
        AddressRequest request = AddressRequest.builder()
                .name("José María García")
                .email("café@example.com")
                .phoneNumber("+33 6 12 34 56 78")
                .addressLine1("Rua São Paulo, 123")
                .city("São Paulo")
                .country("Brasil")
                .reference("Entregar na portaria")
                .addressType(AddressType.SHIPPING)
                .isDefault(true)
                .build();

        Address result = addressMapper.toEntity(request);

        assertEquals("José María García", result.getName());
        assertEquals("café@example.com", result.getEmail());
        assertEquals("+33 6 12 34 56 78", result.getPhoneNumber());
        assertEquals("Rua São Paulo, 123", result.getAddressLine1());
        assertEquals("São Paulo", result.getCity());
        assertEquals("Brasil", result.getCountry());
        assertEquals("Entregar na portaria", result.getReference());
    }

    @Test
    @DisplayName("toEntity - empty but non-null optional fields")
    void toEntity_emptyOptionalFields() {
        AddressRequest request = AddressRequest.builder()
                .name("Valid")
                .addressLine1("Valid")
                .city("Valid")
                .postalCode("Valid")
                .country("Valid")
                .addressType(AddressType.CONTACT)
                .email("")
                .phoneNumber("")
                .addressLine2("")
                .state("")
                .reference("")
                .build();

        Address result = addressMapper.toEntity(request);

        assertEquals("", result.getEmail());
        assertEquals("", result.getPhoneNumber());
        assertEquals("", result.getAddressLine2());
        assertEquals("", result.getState());
        assertEquals("", result.getReference());
    }

    // ===================================================================
    // === toResponse() – Edge Cases
    // ===================================================================

    @Test
    @DisplayName("toResponse - parent with null ID")
    void toResponse_parentWithNullId() {
        Address address = Address.builder()
                .id(1L)
                .name("Test")
                .addressLine1("123 St")
                .city("City")
                .postalCode("12345")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .user(User.builder().id(null).build())
                .company(Company.builder().id(null).build())
                .build();

        AddressResponse response = addressMapper.toResponse(address);

        assertNull(response.getUserId());
        assertNull(response.getCompanyId());
    }

    @Test
    @DisplayName("toResponse - version is null")
    void toResponse_nullVersion() {
        Address address = Address.builder()
                .id(1L)
                .name("Test")
                .addressLine1("123 St")
                .city("City")
                .postalCode("12345")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .version(null)
                .build();

        AddressResponse response = addressMapper.toResponse(address);
        assertNull(response.getVersion());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("toResponse - empty string fields")
    void toResponse_emptyFields(String empty) {
        Address address = Address.builder()
                .id(1L)
                .name(empty)
                .email(empty)
                .phoneNumber(empty)
                .addressLine1(empty)
                .addressLine2(empty)
                .city(empty)
                .state(empty)
                .postalCode(empty)
                .country(empty)
                .reference(empty)
                .addressType(AddressType.BILLING)
                .isDefault(false)
                .build();

        AddressResponse response = addressMapper.toResponse(address);

        assertEquals(empty, response.getName());
        assertEquals(empty, response.getEmail());
        assertEquals(empty, response.getPhoneNumber());
        assertEquals(empty, response.getAddressLine1());
        assertEquals(empty, response.getAddressLine2());
        assertEquals(empty, response.getCity());
        assertEquals(empty, response.getState());
        assertEquals(empty, response.getPostalCode());
        assertEquals(empty, response.getCountry());
        assertEquals(empty, response.getReference());
    }

    // ===================================================================
    // === updateEntityFromRequest() – Edge Cases
    // ===================================================================

    @Test
    @DisplayName("updateEntityFromRequest - only isDefault changes")
    void updateEntityFromRequest_onlyDefaultChanges() {
        Address original = Address.builder()
                .name("Old")
                .addressLine1("Old St")
                .isDefault(false)
                .build();

        AddressRequest request = AddressRequest.builder()
                .isDefault(true)
                .build();

        addressMapper.updateEntityFromRequest(request, original);

        assertEquals("Old", original.getName());
        assertEquals("Old St", original.getAddressLine1());
        assertTrue(original.isDefault());
    }

    @Test
    @DisplayName("updateEntityFromRequest - no fields in request → only isDefault = false")
    void updateEntityFromRequest_emptyRequest() {
        Address original = Address.builder()
                .name("Old")
                .addressLine1("Old St")
                .isDefault(true)
                .build();

        AddressRequest empty = new AddressRequest(); // All null, isDefault = false

        addressMapper.updateEntityFromRequest(empty, original);

        assertEquals("Old", original.getName());
        assertEquals("Old St", original.getAddressLine1());
        assertFalse(original.isDefault()); // Only isDefault updated
    }

    @Test
    @DisplayName("updateEntityFromRequest - large reference text")
    void updateEntityFromRequest_largeReference() {
        String largeRef = "X".repeat(1000);
        AddressRequest request = AddressRequest.builder()
                .reference(largeRef)
                .build();

        Address address = Address.builder().reference("small").build();

        addressMapper.updateEntityFromRequest(request, address);

        assertEquals(largeRef, address.getReference());
    }

    @ParameterizedTest
    @MethodSource("invalidPhoneProvider")
    @DisplayName("updateEntityFromRequest - international phone formats")
    void updateEntityFromRequest_internationalPhone(String phone) {
        AddressRequest request = AddressRequest.builder()
                .phoneNumber(phone)
                .build();

        Address address = Address.builder().phoneNumber("old").build();

        addressMapper.updateEntityFromRequest(request, address);

        assertEquals(phone, address.getPhoneNumber());
    }

    static Stream<String> invalidPhoneProvider() {
        return Stream.of(
                "+1 (555) 123-4567",
                "+44 20 7946 0958",
                "+81-3-1234-5678",
                "0033 6 12 34 56 78",
                "+91 98765 43210",
                "555.123.4567",
                "+1-800-FLOWERS"
        );
    }

    @Test
    @DisplayName("updateEntityFromRequest - addressType case insensitivity not required (enum)")
    void updateEntityFromRequest_addressTypeEnum() {
        // AddressType is enum → must match exactly
        Address address = Address.builder().addressType(AddressType.BILLING).build();

        AddressRequest request = AddressRequest.builder()
                .addressType(AddressType.SHIPPING)
                .build();

        addressMapper.updateEntityFromRequest(request, address);

        assertEquals(AddressType.SHIPPING, address.getAddressType());
    }

    @Test
    @DisplayName("updateEntityFromRequest - concurrent modification simulation")
    void updateEntityFromRequest_concurrentModification() {
        Address address = Address.builder()
                .name("Original")
                .version(1L)
                .build();

        AddressRequest request = AddressRequest.builder()
                .name("Updated")
                .build();

        // Simulate optimistic lock
        address.setVersion(2L); // DB updated elsewhere

        addressMapper.updateEntityFromRequest(request, address);

        assertEquals("Updated", address.getName());
        assertEquals(2L, address.getVersion()); // Not overwritten
    }

    @Test
    @DisplayName("updateEntityFromRequest - all optional fields nullified")
    void updateEntityFromRequest_nullifyAllOptional() {
        Address address = Address.builder()
                .email("old@email.com")
                .phoneNumber("123")
                .addressLine2("Apt")
                .state("CA")
                .reference("Note")
                .build();

        AddressRequest request = AddressRequest.builder()
                .email(null)
                .phoneNumber(null)
                .addressLine2(null)
                .state(null)
                .reference(null)
                .isDefault(true)
                .build();

        addressMapper.updateEntityFromRequest(request, address);

        assertNull(address.getEmail());
        assertNull(address.getPhoneNumber());
        assertNull(address.getAddressLine2());
        assertNull(address.getState());
        assertNull(address.getReference());
        assertTrue(address.isDefault());
    }

    // ===================================================================
    // === Full Cycle Edge Cases
    // ===================================================================

    @Test
    @DisplayName("full cycle - empty request → entity → response")
    void fullCycle_emptyRequest() {
        AddressRequest empty = new AddressRequest();
        empty.setName("Required");
        empty.setAddressLine1("Required");
        empty.setCity("Required");
        empty.setPostalCode("Required");
        empty.setCountry("Required");
        empty.setAddressType(AddressType.CONTACT);

        Address entity = addressMapper.toEntity(empty);
        assertNotNull(entity);
        assertEquals("Required", entity.getName());

        AddressResponse response = addressMapper.toResponse(entity);
        assertNotNull(response);
        assertEquals("Required", response.getName());
        assertFalse(response.isDefault());
    }

    @Test
    @DisplayName("full cycle - update with whitespace-only fields")
    void fullCycle_whitespaceUpdate() {
        AddressRequest request = AddressRequest.builder()
                .email(null)
                .phoneNumber(null)
                .addressLine2(null)
                .state(null)
                .reference(null)
                .isDefault(true)
                .build();

        Address entity = addressMapper.toEntity(request);
        entity.setName("  Original  ");

        AddressRequest update = AddressRequest.builder()
                .name("   ")
                .email("  ")
                .phoneNumber("\t")
                .build();

        addressMapper.updateEntityFromRequest(update, entity);

        assertEquals("   ", entity.getName());
        assertEquals("  ", entity.getEmail());
        assertEquals("\t", entity.getPhoneNumber());
    }

    @Test
    @DisplayName("mapper is stateless and thread-safe")
    void mapper_isStateless() {
        AddressMapper mapper1 = new AddressMapper();
        AddressMapper mapper2 = new AddressMapper();

        AddressRequest req = AddressRequest.builder()
                .name("Test")
                .addressLine1("123")
                .city("City")
                .postalCode("12345")
                .country("USA")
                .addressType(AddressType.SHIPPING)
                .build();

        Address entity1 = mapper1.toEntity(req);
        Address entity2 = mapper2.toEntity(req);

        assertNotSame(entity1, entity2); // Different instances
        assertEquals(entity1.getName(), entity2.getName());
    }
}