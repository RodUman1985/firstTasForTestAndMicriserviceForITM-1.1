package com.itm.space.backendresources.service;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    // Данные для временного пользователя
    private String tempUsername;
    private String tempEmail;
    private String tempUserId;

    @BeforeEach
    void setUp() {
        // Генерируем уникальные username и email для временного пользователя
        String unique = UUID.randomUUID().toString().substring(0, 8);
        tempUsername = "integrationuser_" + unique;
        tempEmail = "integrationuser_" + unique + "@example.com";
        tempUserId = null;

        // Создаем временного пользователя перед каждым тестом
        UserRequest request = new UserRequest(
                tempUsername,
                tempEmail,
                "TestPassword1!",
                "Integration",
                "Tester"
        );
        assertDoesNotThrow(() -> userService.createUser(request));

        // Получаем id созданного пользователя для последующего удаления
        List<UserRepresentation> users = keycloak.realm(realm).users().search(tempUsername);
        UserRepresentation user = users.stream()
                .filter(u -> tempUsername.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
        assertNotNull(user, "Созданный пользователь должен быть найден в Keycloak");
        tempUserId = user.getId();
    }

    @AfterEach
    void tearDown() {
        // Удаляем временного пользователя после каждого теста
        if (tempUserId != null) {
            keycloak.realm(realm).users().delete(tempUserId);
            // Не проверяем результат, чтобы не прерывать цепочку тестов в случае ошибок удаления
        }
    }

    @Test
    @Order(1)
    void createUser_validRequest_userCreated() {
        // Проверяем, что пользователь успешно создан в setUp()
        List<UserRepresentation> users = keycloak.realm(realm).users().search(tempUsername);
        assertFalse(users.isEmpty(), "Пользователь должен существовать после создания");
        UserRepresentation user = users.get(0);
        assertEquals(tempUsername, user.getUsername());
        assertEquals(tempEmail, user.getEmail());
    }

    @Test
    @Order(2)
    void createUser_duplicateEmail_throwsException() {
        // Попытка создать пользователя с тем же email должна вызвать исключение
        UserRequest duplicateRequest = new UserRequest(
                "anotheruser_" + UUID.randomUUID().toString().substring(0, 8),
                tempEmail,
                "TestPassword1!",
                "Integration",
                "Tester"
        );
        BackendResourcesException ex = assertThrows(BackendResourcesException.class, () -> userService.createUser(duplicateRequest));
        assertNotNull(ex.getHttpStatus());
    }

    @Test
    @Order(3)
    void getUserById_nonExisting_throwsException() {
        UUID randomId = UUID.randomUUID();
        BackendResourcesException ex = assertThrows(BackendResourcesException.class, () -> userService.getUserById(randomId));
        assertNotNull(ex.getMessage());
        assertEquals(500, ex.getHttpStatus().value());
    }
}