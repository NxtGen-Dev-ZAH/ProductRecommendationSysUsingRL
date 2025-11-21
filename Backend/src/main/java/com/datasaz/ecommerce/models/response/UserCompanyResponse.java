package com.datasaz.ecommerce.models.response;


import com.datasaz.ecommerce.repositories.entities.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserCompanyResponse {
    //UserCompany userCompany;
    private Long id;
    private String companyName;
    private String vatNumber;
    private String SIREN; //company identification number
    private String emailAddress;
    private List<User> companyUsers;
}



