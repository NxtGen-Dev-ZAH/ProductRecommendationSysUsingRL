//package com.datasaz.ecommerce.controllers;
//
//import com.datasaz.ecommerce.services.interfaces.IAuthService;
//import com.datasaz.ecommerce.services.interfaces.IEmailService;
//import com.datasaz.ecommerce.services.interfaces.IUserService;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//
//@ExtendWith(MockitoExtension.class)
//public class RegisterControllerTest {
//    @Mock
//    private IAuthService authService;
//
//    @Mock
//    private IUserService clientService;
//
//    @Mock
//    private IEmailService emailService;
//
//    private BCryptPasswordEncoder passwordEncoder;
//
/// /    @InjectMocks
/// /    private RegisterController registerController;
//
//
////    @BeforeEach
////    void setUp() {
////        registerController = new RegisterController(authService, clientService, emailService);
////        passwordEncoder = new BCryptPasswordEncoder();
////    }
//
////    @Test
////    @DisplayName("Test register with valid data")
////    void testRegister() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("password")
////                .confirmPassword("password")
////                .firstName("Nom").build();
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password("hashedPassword")
////                .firstName("Nom").build();
////        RegisterResponse mockRegisterResponse = RegisterResponse.builder()
////                .user(mockUser).build();
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.empty());
////        when(clientService.saveExistingUser(any(User.class))).thenReturn(mockRegisterResponse.getUser());
////        ResponseEntity<Map<String, Object>> result = registerController.register(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("message", "Client created successfully. Please check your email for verification link.");
////        expectedResponse.put("clientId", mockRegisterResponse.getUser().getId());
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.CREATED, result.getStatusCode());
////    }
//
////    @Test
////    @DisplayName("Test register with existing email")
////    void testRegisterEmailAlreadyExists() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("password")
////                .confirmPassword("password")
////                .firstName("Nom").build();
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.of(User.builder().build()));
////
////        ResponseEntity<Map<String, Object>> result = registerController.register(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("error", "This email is already taken. Please try another email address.");
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
////    }
//
////    @Test
////    @DisplayName("Test register with passwords not matching")
////    void testRegisterPasswordsDoNotMatch() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("password")
////                .confirmPassword("differentPassword")
////                .firstName("Nom").build();
////
////        ResponseEntity<Map<String, Object>> result = registerController.register(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("error", "The passwords do not match. Please try again with matching passwords.");
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
////    }
//
////    @Test
////    @DisplayName("Test login with in-correct credentials")
////    void testLogin_UnAuthorized() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("password")
////                .build();
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password("hashedPassword")
////                .firstName("Nom").build();
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
////
////        ResponseEntity<Map<String, Object>> result = registerController.login(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("error", "Email or password is incorrect. Please try again with correct credentials.");
////       // expectedResponse.put("clientId", mockClient.getId());
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
////    }
//
////    @Test
////    @DisplayName("Test login with correct credentials")
////    void testLogin_Authorized() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("password")
////                .build();
////        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password(hashedPassword)
////                .firstName("Nom").build();
////
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
////        ResponseEntity<Map<String, Object>> result = registerController.login(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("message", "Connexion réussie!");
////        // expectedResponse.put("clientId", mockClient.getId());
////        Assertions.assertEquals(expectedResponse.get("message"), result.getBody().get("message"));
////        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
////    }
//
////    @Test
////    @DisplayName("Test login with non-existing email")
////    void testLoginEmailNotFound() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("password")
////                .build();
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.empty());
////
////        ResponseEntity<Map<String, Object>> result = registerController.login(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("error", "Email or password is incorrect. Please try again with correct credentials.");
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
////    }
//
//
////    @Test
////    @DisplayName("Test login with incorrect password")
////    void testLoginIncorrectPassword() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password("wrongPassword")
////                .build();
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password("hashedPassword")
////                .firstName("Nom").build();
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
////
////        ResponseEntity<Map<String, Object>> result = registerController.login(registerRequest);
////
////        Map<String, Object> expectedResponse = new HashMap<>();
////        expectedResponse.put("error", "Email or password is incorrect. Please try again with correct credentials.");
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
////    }
//
//
////    @Test
////    @DisplayName("Test forgot password with email")
////    void testForgetPassowrd() {
////        Map<String, String> mockRequest = new HashMap<>();
////                mockRequest.put("email", "email");
////
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password("hashedPassword")
////                .firstName("Nom").build();
////        when(clientService.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
////
////        ResponseEntity<String> result = registerController.forgotPassword(mockRequest);
////
////        String expectedResponse = "An email to reset your password has been sent to email. Please check your inbox or spam folder for the reset link.";
////        Assertions.assertEquals(expectedResponse, result.getBody());
////        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
////    }
//
////    @Test
////    void resetPassword() {
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password("hashedPassword")
////                .firstName("Nom").build();
////        when(clientService.findByResetToken(anyString())).thenReturn(Optional.of(mockUser));
////        when(clientService.saveExistingUser(any(User.class))).thenReturn(mockUser);
////        Map<String,String> request = new HashMap<>();
////        request.put("newPassword", "newPassword");
////        request.put("token", "token");
////        ResponseEntity<String> response = registerController.resetPassword(request);
////        // Verify that the password was updated
////        Assertions.assertEquals("Password reset successfully", response.getBody());
////
////    }
//
//}
