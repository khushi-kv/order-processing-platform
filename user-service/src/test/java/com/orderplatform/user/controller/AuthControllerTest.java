package com.orderplatform.user.controller;

import com.orderplatform.user.config.JwtConfig;
import com.orderplatform.user.config.SecurityConfig;
import com.orderplatform.user.dto.LoginResponse;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.Role;
import com.orderplatform.user.exception.DuplicateResourceException;
import com.orderplatform.user.exception.InvalidCredentialsException;
import com.orderplatform.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer only: real security and error handling, AuthService mocked. */
@WebMvcTest(controllers = AuthController.class,
        properties = "app.jwt.secret=test-secret-that-is-at-least-32-characters-long")
@Import({SecurityConfig.class, JwtConfig.class})
class AuthControllerTest {

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String VALID_REGISTER = """
            {"name":"Khushi","email":"khushi@test.com","password":"Secret123"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerIsOpenAndReturns201WithoutThePassword() throws Exception {
        when(authService.register(any())).thenReturn(
                new UserResponse(1L, "Khushi", "khushi@test.com", Role.CUSTOMER, Instant.parse("2026-10-26T10:00:00Z")));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerReturns400WithOneDetailPerInvalidField() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"Secret123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "name: Name is required", "email: Email format is invalid")));
        verifyNoInteractions(authService);
    }

    @Test
    void registerReturns409ForADuplicateEmail() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email is already registered: khushi@test.com"));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isConflict());
    }

    @Test
    void registerReturns409WhenTheDatabaseRejectsAConcurrentDuplicate() throws Exception {
        when(authService.register(any())).thenThrow(new DataIntegrityViolationException("duplicate key users_email_key"));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isConflict());
    }

    @Test
    void malformedJsonReturns400InTheSameFormat() throws Exception {
        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(authService.register(any())).thenThrow(new IllegalStateException("connection pool exhausted"));

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void loginIsOpenAndReturnsTheToken() throws Exception {
        when(authService.login(any())).thenReturn(LoginResponse.bearer("signed.jwt.token", 3600));

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"khushi@test.com","password":"Secret123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"));
    }

    @Test
    void loginReturns401ForBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"khushi@test.com","password":"Wrong123"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
