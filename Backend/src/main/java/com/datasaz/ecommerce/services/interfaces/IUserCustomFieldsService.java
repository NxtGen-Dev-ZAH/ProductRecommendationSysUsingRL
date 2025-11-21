package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.UserDto;
import jakarta.validation.Valid;

import java.util.List;

public interface IUserCustomFieldsService {

    UserDto addCustomField(String email, @Valid UserDto.CustomFieldDto customFieldDto);

    //UserDto removeCustomField(String email, String customFieldName);
    UserDto deleteCustomField(String email, Long fieldId);

    UserDto updateCustomField(String email, Long fieldId, @Valid UserDto.CustomFieldDto customFieldDto);

    List<UserDto.CustomFieldDto> getAllCustomFields(String email);

    UserDto.CustomFieldDto getCustomField(String email, String fieldKey);
//    UserDto.CustomFieldDto getCustomField(String email, String customFieldName);

}
