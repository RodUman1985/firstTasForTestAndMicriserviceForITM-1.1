package com.itm.space.backendresources.service;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest  {

    @Mock
    private Keycloak keycloakClient;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;



    @InjectMocks
    private UserServiceImpl userService;

    private final String realm = "ITM";

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field realmField = UserServiceImpl.class.getDeclaredField("realm");
        realmField.setAccessible(true);
        realmField.set(userService, realm);
    }

    @Test
    void createUser_Success() {
        UserRequest userRequest = new UserRequest(
                "testuser", "test@example.com", "password", "John", "Doe");
        Response response = mock(Response.class);

        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(response);

        when(response.getStatus()).thenReturn(201);

        lenient().when(response.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(response.getLocation()).thenReturn(URI.create("http://test/auth/admin/realms/ITM/users/12345-6789"));

        assertDoesNotThrow(() -> userService.createUser(userRequest));
        verify(usersResource, times(1)).create(any());
    }

    @Test
    void createUser_Failure() {
        UserRequest userRequest = new UserRequest(
                "testuser", "test@example.com", "password", "John", "Doe");

        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenThrow(new WebApplicationException("Error", 400));

        BackendResourcesException exception = assertThrows(
                BackendResourcesException.class,
                () -> userService.createUser(userRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void getUserById_Success() {
        UUID userId = UUID.randomUUID();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setFirstName("John");
        userRepresentation.setLastName("Doe");
        userRepresentation.setEmail("test@example.com");

        List<RoleRepresentation> roles = Collections.emptyList();
        List<GroupRepresentation> groups = Collections.emptyList();
        UserResponse expectedResponse = new UserResponse(
                "John", "Doe", "test@example.com", Collections.emptyList(), Collections.emptyList());

        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        MappingsRepresentation mappingsRepresentation = mock(MappingsRepresentation.class);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);
        when(mappingsRepresentation.getRealmMappings()).thenReturn(roles);

        when(userResource.groups()).thenReturn(groups);

        when(userMapper.userRepresentationToUserResponse(userRepresentation, roles, groups))
                .thenReturn(expectedResponse);

        UserResponse actualResponse = userService.getUserById(userId);

        assertEquals(expectedResponse, actualResponse);
        verify(userResource, times(1)).toRepresentation();
    }

    @Test
    void getUserById_Failure() {
        UUID userId = UUID.randomUUID();

        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenThrow(new RuntimeException("Error"));

        BackendResourcesException exception = assertThrows(
                BackendResourcesException.class,
                () -> userService.getUserById(userId));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }
}