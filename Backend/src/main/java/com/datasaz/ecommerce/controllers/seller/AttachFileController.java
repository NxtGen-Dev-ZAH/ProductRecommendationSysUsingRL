//package com.datasaz.ecommerce.controllers.seller;
//
//import com.datasaz.ecommerce.exceptions.BadRequestException;
//import com.datasaz.ecommerce.models.Request.AttachFileRequest;
//import com.datasaz.ecommerce.repositories.entities.ProductFileAttach;
//import com.datasaz.ecommerce.services.implementations.ProductImageService;
//import com.datasaz.ecommerce.utilities.AttachFileValidator;
//import jakarta.validation.constraints.NotNull;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Set;
//
//import static org.springframework.http.ResponseEntity.noContent;
//
//@RestController
//@RequestMapping("/seller/product/file")
//@RequiredArgsConstructor
//public class AttachFileController {
//
//    private final ProductImageService attachFileService;
//
//    @PostMapping("/attach")
//    public ResponseEntity<Void> attachFile(
//            @RequestBody AttachFileRequest attachFileRequest,
//            @RequestHeader HttpHeaders httpHeaders) {
//
//        //securityService.validateSecurity(String.valueOf(cdbId), httpHeaders);
//        attachFileService.saveTempFile(attachFileRequest);
//        return noContent().build();
//    }
//
//    @DeleteMapping("/delete/{fileName}")
//    public ResponseEntity<Void> deleteFile(
//            @RequestParam("fileName") @NotNull String fileName,
//            @RequestParam("product") @NotNull Long product,
//            @RequestHeader HttpHeaders httpHeaders) {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName(fileName)
//                .productId(product)
//                .build();
//        attachFileService.deleteTempFile(attachFileRequest);
//        return noContent().build();
//    }
//
//    @GetMapping("/get/{fileName}")
//    public ResponseEntity<Object> getFile(
//            @RequestParam("fileName") @NotNull String fileName,
//            @RequestParam("product") @NotNull Long product,
//            @RequestHeader HttpHeaders httpHeaders) {
//
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName(fileName)
//                .productId(product)
//                .build();
//        ProductFileAttach productFileAttach = attachFileService.getTempFile(attachFileRequest);
//        HttpHeaders headers = setHeaderForGetFile(productFileAttach);
//        headers.setContentDisposition(ContentDisposition.builder("inline").filename(productFileAttach.getFileName()).build());
//
//        return new ResponseEntity<>(productFileAttach.getContent(), headers, HttpStatus.OK);
//    }
//
//    private HttpHeaders setHeaderForGetFile(ProductFileAttach productFileAttach) {
//        HttpHeaders headers = new HttpHeaders();
//        Set<String> mediaTypeSet = AttachFileValidator.getAllMediaTypes();
//        if (!mediaTypeSet.contains(productFileAttach.getFileType())) {
//            throw BadRequestException.builder().message("This file type is not supported.").build();
//        }
//        headers.setContentType(MediaType.valueOf(productFileAttach.getFileType()));
//        return headers;
//    }
//}
