package com.datasaz.ecommerce.models.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFollowersCountResponse {
    private Long count;
}
