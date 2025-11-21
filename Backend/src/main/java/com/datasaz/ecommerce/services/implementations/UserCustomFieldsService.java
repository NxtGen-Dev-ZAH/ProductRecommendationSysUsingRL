package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.UserCustomFieldsRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.repositories.entities.UserCustomFields;
import com.datasaz.ecommerce.services.interfaces.IUserCustomFieldsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserCustomFieldsService implements IUserCustomFieldsService {

    private final UserRepository userRepository;
    private final UserCustomFieldsRepository userCustomFieldsRepository;

    private final UserMapper userMapper;


    public UserDto addCustomField(String email, @Valid UserDto.CustomFieldDto customFieldDto) {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (userCustomFieldsRepository.existsByUserIdAndFieldKey(user.getId(), customFieldDto.getFieldKey())) {
            throw new IllegalArgumentException("Custom field key already exists: " + customFieldDto.getFieldKey());
        }

        UserCustomFields customField = UserCustomFields.builder()
                .fieldKey(customFieldDto.getFieldKey())
                .fieldValue(customFieldDto.getFieldValue())
                .description(customFieldDto.getDescription())
                .user(user)
                .build();

        user.getCustomFields().add(customField);
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public UserDto updateCustomField(String email, Long fieldId, @Valid UserDto.CustomFieldDto customFieldDto) {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserCustomFields customField = userCustomFieldsRepository.findById(fieldId)
                .orElseThrow(() -> new EntityNotFoundException("Custom field not found"));

        if (!customField.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Custom field does not belong to user");
        }

        customField.setFieldKey(customFieldDto.getFieldKey());
        customField.setFieldValue(customFieldDto.getFieldValue());
        customField.setDescription(customFieldDto.getDescription());

        userCustomFieldsRepository.save(customField);
        return userMapper.toDto(user);
    }

    public UserDto deleteCustomField(String email, Long fieldId) {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserCustomFields customField = userCustomFieldsRepository.findById(fieldId)
                .orElseThrow(() -> new EntityNotFoundException("Custom field not found"));

        if (!customField.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Custom field does not belong to user");
        }

        user.getCustomFields().remove(customField);
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public List<UserDto.CustomFieldDto> getAllCustomFields(String email) {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return userCustomFieldsRepository.findByUserId(user.getId()).stream()
                .map(cf -> UserDto.CustomFieldDto.builder()
                        .id(cf.getId())
                        .fieldKey(cf.getFieldKey())
                        .fieldValue(cf.getFieldValue())
                        .description(cf.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    public UserDto.CustomFieldDto getCustomField(String email, String fieldKey) {
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserCustomFields customField = userCustomFieldsRepository.findByFieldKey(fieldKey)
                .orElseThrow(() -> new EntityNotFoundException("Custom field not found"));

        if (!customField.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Custom field does not belong to user");
        }

        return UserDto.CustomFieldDto.builder()
                .id(customField.getId())
                .fieldKey(customField.getFieldKey())
                .fieldValue(customField.getFieldValue())
                .description(customField.getDescription())
                .build();
    }
}
