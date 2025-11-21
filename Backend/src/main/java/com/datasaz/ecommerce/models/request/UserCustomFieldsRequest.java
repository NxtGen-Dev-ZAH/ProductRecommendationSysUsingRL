package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCustomFieldsRequest {

    private String fieldKey;
    private String fieldValue;
    private String description;

    private User user;
}
