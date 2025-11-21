//package com.datasaz.ecommerce.controllers;
//
//import com.datasaz.ecommerce.controllers.admin.AdminClientController;
//import com.datasaz.ecommerce.services.interfaces.IEmailService;
//import com.datasaz.ecommerce.services.interfaces.IUserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//public class AdminUserControllerTest {
//    @Mock
//    private IUserService clientService;
//
//    @Mock
//    private IEmailService emailService;
//
//    @InjectMocks
//    private AdminClientController clientController;
//
//    @BeforeEach
//    void setUp() {
//        clientController = new AdminClientController(clientService, emailService);
//
//    }
//
/// /    @Test
/// /    @DisplayName("Test addClient")
/// /    void test_addClient() {
/// /        when(clientService.saveNewUser(any(RegisterRequest.class)))
/// /                .thenReturn(User.builder().id(1L).build());
/// /        RegisterResponse registerResponse = clientController.addClient(RegisterRequest.builder().build());
/// /        Assertions.assertEquals(1L, registerResponse.getUser().getId());
/// /
/// /    }
//
////    @Test
////    @DisplayName("Test getClientById")
////    void test_getUsertById() {
////        when(clientService.findById(1L))
////                .thenReturn(Optional.of(User.builder().id(1L).build()));
////        ResponseEntity<User> clientResponse = clientController.getClientById(1L);
////        Assertions.assertEquals(HttpStatus.OK, clientResponse.getStatusCode());
////        Assertions.assertEquals(1L, clientResponse.getBody().getId());
////
////    }
////    @Test
////    @DisplayName("Test getClientById Not Found")
////    void test_getUsertById_NotFound() {
////        when(clientService.findById(1L))
////                .thenReturn(Optional.empty());
////        ResponseEntity<User> clientResponse = clientController.getClientById(1L);
////        Assertions.assertEquals(HttpStatus.NOT_FOUND, clientResponse.getStatusCode());
////
////    }
////    @Test
////    @DisplayName("Test getAllClients")
////    void test_getAllClients() {
////        when(clientService.findAll())
////                .thenReturn(List.of(User.builder().id(1L).build()));
////        List<User> userResponse = clientController.getAllClients();
////        Assertions.assertEquals(1, userResponse.size());
////        Assertions.assertEquals(1L, userResponse.get(0).getId());
////
////    }
////    @Test
////    @DisplayName("Test updateClient")
////    void test_updateUser() {
////        when(clientService.updateUserPassword(any(RegisterRequest.class), any(Long.class)))
////                .thenReturn(User.builder().id(1L).build());
////        RegisterResponse registerResponse = clientController.updateClient(RegisterRequest.builder().build(), 1L);
////        Assertions.assertEquals(1L, registerResponse.getUser().getId());
////
////    }
////    @Test
////    @DisplayName("Test deleteClient")
////    void test_deleteClient() {
////        doNothing().when(clientService).deleteById(1L);
////        ResponseEntity<Void> clientResponse = clientController.deleteClient(1L);
////        Assertions.assertEquals(HttpStatus.NO_CONTENT, clientResponse.getStatusCode());
////
////    }
//
//}
