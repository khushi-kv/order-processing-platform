package com.orderplatform.user.controller;

import com.orderplatform.user.TestUsers;
import com.orderplatform.user.config.JwtConfig;
import com.orderplatform.user.config.SecurityConfig;
import com.orderplatform.user.dto.UserResponse;
import com.orderplatform.user.entity.Role;
import com.orderplatform.user.exception.ResourceNotFoundException;
import com.orderplatform.user.service.JwtService;
import com.orderplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer with the real security chain and JWT decoder; UserService mocked. */
@WebMvcTest(controllers = UserController.class,
        properties = "app.jwt.secret=test-secret-that-is-at-least-32-characters-long")
@Import({SecurityConfig.class, JwtConfig.class, JwtService.class})
class UserControllerTest {

    private static final String ME_URL = "/api/users/me";
    private static final UserResponse KHUSHI =
            new UserResponse(1L, "Khushi", "khushi@test.com", Role.CUSTOMER, Instant.parse("2026-10-26T10:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void meWithoutATokenReturns401() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
        verifyNoInteractions(userService);
    }

    @Test
    void meWithARealSignedTokenReturnsTheProfile() throws Exception {
        String token = jwtService.issueToken(TestUsers.customer(1L));
        when(userService.getProfile(1L)).thenReturn(KHUSHI);

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("khushi@test.com"));
    }

    @Test
    void meWithATamperedTokenReturns401() throws Exception {
        String token = jwtService.issueToken(TestUsers.customer(1L));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        verifyNoInteractions(userService);
    }

    @Test
    void meReturns404WhenTheUserNoLongerExists() throws Exception {
        when(userService.getProfile(99L)).thenThrow(new ResourceNotFoundException("User not found: 99"));

        mockMvc.perform(get(ME_URL).with(jwt().jwt(token -> token.subject("99"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMeChangesTheName() throws Exception {
        when(userService.updateProfile(eq(1L), any())).thenReturn(
                new UserResponse(1L, "Khushi Verma", "khushi@test.com", Role.CUSTOMER, KHUSHI.createdAt()));

        mockMvc.perform(put(ME_URL).with(jwt().jwt(token -> token.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Khushi Verma"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Khushi Verma"));
    }
}
