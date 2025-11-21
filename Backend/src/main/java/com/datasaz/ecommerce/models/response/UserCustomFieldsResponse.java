package com.datasaz.ecommerce.models.response;


import com.datasaz.ecommerce.repositories.entities.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCustomFieldsResponse {
    //UserCustomFields userCustomFields;
    private Long id;
    private String fieldKey;
    private String fieldValue;
    private String description;

    private User user;
}



