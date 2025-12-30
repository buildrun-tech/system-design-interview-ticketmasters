package tech.buildrun.service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.buildrun.controller.dto.CreateUserDto;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.CreateEntityException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class UserServiceTest {

    @Inject
    UserService userService;

    @Nested
    class createUser {

        @Test
        void shouldNotCreateUserWhenUserAlreadyExist() {
            // Arrange
            PanacheMock.mock(UserEntity.class);
            var dto = new CreateUserDto("bruno", "bruno@email.com", "123456");

            when(UserEntity.count(anyString(), Mockito.<Object[]>any()))
                    .thenReturn(Long.valueOf(2));

            assertThrows(CreateEntityException.class, () -> userService.createUser(dto));

            PanacheMock.verify(UserEntity.class, never()).persist(any(), any(Object[].class));
        }

        @Test
        void shouldCreateuserWhenUserDoesNotExist() {
            // Arrange
            PanacheMock.mock(UserEntity.class);
            PanacheMock.mock(RoleEntity.class);
            var q = mock(PanacheQuery.class);
            var role = new RoleEntity();
            role.name = "USER";

            var dto = new CreateUserDto("bruno", "bruno@email.com", "123456");

            when(UserEntity.count(anyString(), Mockito.<Object[]>any()))
                    .thenReturn(Long.valueOf(0));
            when(RoleEntity.find(anyString(), Mockito.<Object[]>any()))
                    .thenReturn(q);
            when(q.firstResult())
                    .thenReturn(role);

            // Act
            var userEntity = userService.createUser(dto);

            // Assert
            PanacheMock.verify(UserEntity.class, times(1)).count(any(), any(Object[].class));
            PanacheMock.verify(UserEntity.class, times(1)).persist(any(), any(Object[].class));
            PanacheMock.verify(RoleEntity.class, times(1)).find(anyString(), any(Object[].class));

            assertNotNull(userEntity);
            assertEquals(dto.username(), userEntity.username);
            assertEquals(dto.email(), userEntity.email);
            assertNotEquals(dto.password(), userEntity.password);
        }
    }
}