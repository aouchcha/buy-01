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
import buy01.user.config.Exceptions.MyExeptions.badRequest;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.authResponse;
import buy01.user.dto.Auth.registerRequest;
import buy01.user.model.Roles;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    private userRepository repository;
    private Jwt jwtService;
    private registerService registerService;

    @BeforeEach
    void setUp() {
        repository = mock(userRepository.class);
        jwtService = mock(Jwt.class);
        registerService = new registerService(repository, jwtService);
    }

    @Test
    void signUp_withValidData_returnsToken() {
        registerRequest request = new registerRequest(
                "jane.doe@example.com", "SecurePass1", "Jane", "Doe", Roles.BUYER.toString());

        userEntity saved = new userEntity();
        saved.setId("generated-id");
        saved.setFirstName("Jane");
        saved.setLastName("Doe");
        saved.setEmail("jane.doe@example.com");
        saved.setRole(Roles.BUYER.toString());

        when(repository.save(any(userEntity.class))).thenReturn(saved);
        when(jwtService.GenerateToken(anyString(), anyString(), anyString())).thenReturn("generated-jwt-token");

        authResponse response = registerService.signUp(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-jwt-token");
        assertThat(response.getMessage()).isEqualTo("user registered successfully");
        assertThat(response.getUser().getEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void signUp_withAdminRole_throwsBadRequest() {
        registerRequest request = new registerRequest(
                "admin@example.com", "SecurePass1", "Admin", "User", Roles.ADMIN.toString());

        assertThatThrownBy(() -> registerService.signUp(request))
                .isInstanceOf(badRequest.class);

        verify(repository, never()).save(any(userEntity.class));
    }

    @Test
    void signUp_withDuplicateEmail_throwsConflict() {
        registerRequest request = new registerRequest(
                "duplicate@example.com", "SecurePass1", "Jane", "Doe", Roles.BUYER.toString());

        when(repository.save(any(userEntity.class))).thenThrow(new DuplicateKeyException("duplicate key"));

        assertThatThrownBy(() -> registerService.signUp(request))
                .isInstanceOf(Conflict.class);
    }

    @Test
    void signUp_whenSaveThrowsOtherException_throwsBadRequest() {
        registerRequest request = new registerRequest(
                "jane.doe@example.com", "SecurePass1", "Jane", "Doe", Roles.BUYER.toString());

        when(repository.save(any(userEntity.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> registerService.signUp(request))
                .isInstanceOf(badRequest.class);
    }
}
