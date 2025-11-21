package com.datasaz.ecommerce.exceptions.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ExceptionResponse {
    private String message;
    private int status;
    private String error;
    private String path;
    private long timestamp;
    private Map<String, String> errors; // Added to support validation error details
}


//package com.datasaz.ecommerce.exceptions.response;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//
//@Data
//@Builder
//@AllArgsConstructor
//public class ExceptionResponse {
//    private int status;
//    private String code;
//    private String message;
//    private String error;
//    private String path;
//    private long timestamp;
//
//}
