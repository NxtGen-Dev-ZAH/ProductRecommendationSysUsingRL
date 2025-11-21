package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ICompanyAddressService extends IAddressService {

    // CRUD operations
    AddressResponse addAddress(Long companyId, AddressRequest request);

    AddressResponse updateAddress(Long companyId, Long addressId, AddressRequest request);
    void deleteAddress(Long companyId, Long addressId);

    AddressResponse getAddress(Long companyId, Long addressId);

    // List operations
    List<AddressResponse> getAllAddresses(Long companyId);

    Page<AddressResponse> getAddressesPaginated(Long companyId, int page, int size);

    List<AddressResponse> getAddressesByType(Long companyId, AddressType type);

    // Default address operations
    AddressResponse getDefaultAddress(Long companyId, AddressType type);

    AddressResponse setDefaultAddress(Long companyId, Long addressId);

    // Internal operations
    Address getAddressEntity(Long companyId, Long addressId);

    // Company-specific operations
    List<Address> getAddressesByCompanyIdAndType(Long companyId, AddressType type);

    Address getDefaultAddressByCompanyIdAndType(Long companyId, AddressType type);

    long countAddressesByCompanyId(Long companyId);

    List<AddressResponse> getBillingAddresses(Long companyId);

    List<AddressResponse> getContactAddresses(Long companyId);

    List<AddressResponse> getExpeditionAddresses(Long companyId);

    AddressResponse getPrimaryBusinessAddress(Long companyId);
}
