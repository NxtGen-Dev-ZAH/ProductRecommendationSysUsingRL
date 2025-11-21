package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IUserAddressService extends IAddressService {

    // CRUD operations
    AddressResponse addAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);
    void deleteAddress(Long userId, Long addressId);

    AddressResponse getAddress(Long userId, Long addressId);

    // List operations
    List<AddressResponse> getAllAddresses(Long userId);

    Page<AddressResponse> getAddressesPaginated(Long userId, int page, int size);

    List<AddressResponse> getAddressesByType(Long userId, AddressType type);

    // Default address operations
    AddressResponse getDefaultAddress(Long userId, AddressType type);

    AddressResponse setDefaultAddress(Long userId, Long addressId);

    // Internal operations
    Address getAddressEntity(Long userId, Long addressId);

    // User-specific operations
    List<Address> getAddressesByUserIdAndType(Long userId, AddressType type);

    Address getDefaultAddressByUserIdAndType(Long userId, AddressType type);

    long countAddressesByUserId(Long userId);

    List<AddressResponse> getShippingAddresses(Long userId);

    List<AddressResponse> getBillingAddresses(Long userId);

    AddressResponse getDefaultShippingAddress(Long userId);

    AddressResponse getDefaultBillingAddress(Long userId);

    boolean userHasAddresses(Long userId);
}

