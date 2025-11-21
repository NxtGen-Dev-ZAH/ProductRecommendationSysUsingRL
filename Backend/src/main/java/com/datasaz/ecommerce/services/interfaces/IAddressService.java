package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;

import java.util.List;

public interface IAddressService {
    AddressResponse addAddress(Long parentId, AddressRequest request);

    AddressResponse updateAddress(Long parentId, Long addressId, AddressRequest request);

    void deleteAddress(Long parentId, Long addressId);

    AddressResponse getAddress(Long parentId, Long addressId);

    List<AddressResponse> getAllAddresses(Long parentId);
}
