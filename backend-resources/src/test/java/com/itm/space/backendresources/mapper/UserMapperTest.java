package com.itm.space.backendresources.mapper;

import com.itm.space.backendresources.api.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);


    @Test
    void userRepresentationToUserResponse() {

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setFirstName("John");
        userRepresentation.setLastName("Doe");
        userRepresentation.setEmail("john.doe@example.com");

        RoleRepresentation role = new RoleRepresentation();
        role.setName("user");
        List<RoleRepresentation> roles = List.of(role);

        GroupRepresentation group = new GroupRepresentation();
        group.setName("test-group");
        List<GroupRepresentation> groups = List.of(group);


        UserResponse response = userMapper.userRepresentationToUserResponse(
                userRepresentation, roles, groups);


        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals(1, response.getRoles().size());
        assertEquals("user", response.getRoles().get(0));
        assertEquals(1, response.getGroups().size());
        assertEquals("test-group", response.getGroups().get(0));
    }
}