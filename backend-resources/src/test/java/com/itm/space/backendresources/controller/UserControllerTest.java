package com.itm.space.backendresources.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.BackendResourcesApplication;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendResourcesApplication.class)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "MODERATOR")
    void createUser_Success() throws Exception {
        UserRequest userRequest = new UserRequest(
                "testuser", "test@example.com", "password", "Test", "User");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk());

        verify(userService, times(1)).createUser(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void getUserById_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse mockResponse = new UserResponse(
                "Test", "User", "test@example.com", List.of("ROLE_USER"), List.of("GROUP_TEST"));

        when(userService.getUserById(userId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.groups[0]").value("GROUP_TEST"));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void helloEndpoint_Success() throws Exception {
        mockMvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("user"));
    }

    @Test
    @WithMockUser // без роли MODERATOR
    void createUser_Forbidden() throws Exception {
        UserRequest userRequest = new UserRequest(
                "testuser", "test@example.com", "password", "Test", "User");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void createUser_ValidationFailed() throws Exception {

        UserRequest invalidRequest = new UserRequest(
                "a", "invalid-email", "123", "", "");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists());
    }
}