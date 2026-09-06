package buy01.user.service.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import buy01.user.config.Exceptions.MyExeptions.Conflict;
import buy01.user.config.Exceptions.MyExeptions.BadRequest;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.AuthResponse;
import buy01.user.dto.Auth.RegisterRequest;
import buy01.user.model.Roles;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    private UserRepository repository;
    private Jwt jwtService;
    private RegisterService RegisterService;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        jwtService = mock(Jwt.class);
        RegisterService = new RegisterService(repository, jwtService);
    }

    @Test
    void signUp_withValidData_returnsToken() {
        RegisterRequest request = new RegisterRequest(
                "jane.doe@example.com", "SecurePass1", "Jane", "Doe", Roles.BUYER.toString());

        UserEntity saved = new UserEntity();
        saved.setId("generated-id");
        saved.setFirstName("Jane");
        saved.setLastName("Doe");
        saved.setEmail("jane.doe@example.com");
        saved.setRole(Roles.BUYER.toString());

        when(repository.save(any(UserEntity.class))).thenReturn(saved);
        when(jwtService.GenerateToken(anyString(), anyString(), anyString())).thenReturn("generated-jwt-token");

        AuthResponse response = RegisterService.signUp(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-jwt-token");
        assertThat(response.getMessage()).isEqualTo("user registered successfully");
        assertThat(response.getUser().getEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void signUp_withAdminRole_throwsBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "admin@example.com", "SecurePass1", "Admin", "User", Roles.ADMIN.toString());

        assertThatThrownBy(() -> RegisterService.signUp(request))
                .isInstanceOf(BadRequest.class);

        verify(repository, never()).save(any(UserEntity.class));
    }

    @Test
    void signUp_withDuplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest(
                "duplicate@example.com", "SecurePass1", "Jane", "Doe", Roles.BUYER.toString());

        when(repository.save(any(UserEntity.class))).thenThrow(new DuplicateKeyException("duplicate key"));

        assertThatThrownBy(() -> RegisterService.signUp(request))
                .isInstanceOf(Conflict.class);
    }

    @Test
    void signUp_whenSaveThrowsOtherException_throwsBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "jane.doe@example.com", "SecurePass1", "Jane", "Doe", Roles.BUYER.toString());

        when(repository.save(any(UserEntity.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> RegisterService.signUp(request))
                .isInstanceOf(BadRequest.class);
    }
}
