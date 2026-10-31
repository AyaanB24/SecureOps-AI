package com.secureops.auth;

import com.secureops.auth.dto.LoginRequest;
import com.secureops.auth.dto.RegisterRequest;
import com.secureops.user.User;
import com.secureops.user.UserRepository;
import com.secureops.user.UserRole;
import com.secureops.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FILE: src/test/java/com/secureops/auth/AuthControllerTest.java
 * PURPOSE: Integration tests for authentication endpoints (registration and login).
 * WHY IT EXISTS: Verifies endpoint behavior, validation, security rules, and database persistence.
 * COVERAGE: Successful registration, duplicate email, invalid input, successful login, invalid credentials, role escalation prevention.
 */
@SpringBootTest(classes = {com.secureops.SecureopsApplication.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
    }

    // ==================== Registration Tests ====================

    @Test
    void testRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser@example.com", "New User", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.name").value("New User"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.message").exists());

        Optional<User> savedUser = userRepository.findByEmail("newuser@example.com");
        assertTrue(savedUser.isPresent());
        assertEquals("New User", savedUser.get().getName());
        assertEquals(UserRole.USER, savedUser.get().getRole());
        assertTrue(savedUser.get().getEnabled());
    }

    @Test
    void testRegisterWithDuplicateEmailReturnsConflict() throws Exception {
        RegisterRequest firstRequest = new RegisterRequest("duplicate@example.com", "First User", "Password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());

        RegisterRequest secondRequest = new RegisterRequest("duplicate@example.com", "Different User", "Password456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testRegisterWithInvalidEmailReturnsValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid-email", "User", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void testRegisterWithShortPasswordReturnsValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest("user@example.com", "User", "Short1");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void testRegisterWithMissingFieldsReturnsValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void testRegisteredUserPasswordIsHashed() throws Exception {
        String plainPassword = "PlainPassword123";
        RegisterRequest request = new RegisterRequest("hash@example.com", "Hash Test", plainPassword);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        Optional<User> savedUser = userRepository.findByEmail("hash@example.com");
        assertTrue(savedUser.isPresent());
        User user = savedUser.get();

        assertNotEquals(plainPassword, user.getPasswordHash());
        assertTrue(passwordEncoder.matches(plainPassword, user.getPasswordHash()));
    }

    @Test
    void testRegistrationAlwaysAssignsUserRole() throws Exception {
        String jsonRequest = """
            {
                "email": "user@example.com",
                "name": "Test User",
                "password": "SecurePass123",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("USER"));

        Optional<User> savedUser = userRepository.findByEmail("user@example.com");
        assertTrue(savedUser.isPresent());
        assertEquals(UserRole.USER, savedUser.get().getRole());
    }

    // ==================== Login Tests ====================

    @Test
    void testLoginWithValidCredentialsSucceeds() throws Exception {
        String email = "login@example.com";
        String password = "ValidPass123";
        userService.registerUser(email, "Login User", password);

        LoginRequest request = new LoginRequest(email, password);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testLoginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String email = "wrongpass@example.com";
        String correctPassword = "CorrectPass123";
        userService.registerUser(email, "User", correctPassword);

        LoginRequest request = new LoginRequest(email, "WrongPassword123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginWithNonexistentEmailReturnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "AnyPassword123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginResponseDoesNotIncludePasswordHash() throws Exception {
        userService.registerUser("nopass@example.com", "No Pass User", "SecurePass123");

        LoginRequest request = new LoginRequest("nopass@example.com", "SecurePass123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.role").exists())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void testLoginWithDisabledAccountReturnsUnauthorized() throws Exception {
        User user = userService.registerUser("disabled@example.com", "Disabled User", "Pass123");
        user.setEnabled(false);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("disabled@example.com", "Pass123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void testLoginWithInvalidEmailFormatReturnsValidationError() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "Password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
