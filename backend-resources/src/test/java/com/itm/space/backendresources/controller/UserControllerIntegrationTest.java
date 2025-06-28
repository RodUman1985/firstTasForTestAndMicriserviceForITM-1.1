package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_API = "/api/users";
    private String uniqueUsername;
    private String uniqueEmail;
    private String createdUserId;

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    private UserRequest buildValidUserRequest() {
        return new UserRequest(
                uniqueUsername,
                uniqueEmail,
                "ControllerPass1!",
                "Controller",
                "Tester"
        );
    }

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        uniqueUsername = "controlleruser_" + unique;
        uniqueEmail = "controlleruser_" + unique + "@example.com";
        createdUserId = null;
    }

    @AfterEach
    void tearDown() {
        if (createdUserId != null) {
            try {
                keycloak.realm(realm).users().delete(createdUserId);
            } catch (Exception ignored) {}
        }
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void createUser_withValidData_returnsOk() throws Exception {
        UserRequest request = buildValidUserRequest();
        mvc.perform(requestWithContent(post(BASE_API), request))
                .andExpect(status().isOk());


        List<UserRepresentation> users = keycloak.realm(realm).users().search(uniqueUsername);
        UserRepresentation user = users.stream()
                .filter(u -> uniqueUsername.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(user, "Пользователь должен быть создан");
        createdUserId = user.getId();
    }

    @Test
    @WithMockUser // Без роли MODERATOR
    void createUser_withoutProperRole_forbidden() throws Exception {
        UserRequest request = buildValidUserRequest();
        mvc.perform(requestWithContent(post(BASE_API), request))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void createUser_withInvalidData_returnsBadRequest() throws Exception {
        UserRequest request = new UserRequest(
                "", // invalid username
                "invalid", // invalid email
                "123", // short password
                "", // empty first name
                "" // empty last name
        );
        mvc.perform(requestWithContent(post(BASE_API), request))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", not(empty())))
                .andExpect(jsonPath("$.email", not(empty())))
                .andExpect(jsonPath("$.password", not(empty())))
                .andExpect(jsonPath("$.firstName", not(empty())))
                .andExpect(jsonPath("$.lastName", not(empty())));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void getUserById_nonExisting_returnsError() throws Exception {
        mvc.perform(get(BASE_API + "/" + UUID.randomUUID()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void hello_withValidUser_returnsUsername() throws Exception {
        mvc.perform(get(BASE_API + "/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyOrNullString())));
    }

    @Test
    void hello_withoutAuth_unauthorized() throws Exception {
        mvc.perform(get(BASE_API + "/hello"))
                .andExpect(status().isUnauthorized());
    }
}