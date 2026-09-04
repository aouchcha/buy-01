package buy01.user.service.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import buy01.user.config.Exceptions.MyExeptions.Unauthorized;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.AuthRequest;
import buy01.user.dto.Auth.AuthResponse;
import buy01.user.model.Roles;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private Jwt jwt;
    private UserRepository repository;
    private LoginService LoginService;

    private static final String RAW_PASSWORD = "correctPassword123";
    private UserEntity existingUser;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        repository = mock(UserRepository.class);
        LoginService = new LoginService(jwt, repository);

        existingUser = new UserEntity();
        existingUser.setId("user-id-1");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
        existingUser.setEmail("john.doe@example.com");
        existingUser.setPassword(BCrypt.hashpw(RAW_PASSWORD, BCrypt.gensalt()));
        existingUser.setProfilePictureUrl(null);
        existingUser.setRole(Roles.BUYER.toString());
    }

    @Test
    void login_withCorrectCredentials_returnsToken() {
        AuthRequest request = new AuthRequest(existingUser.getEmail(), RAW_PASSWORD);
        when(repository.findByEmail(existingUser.getEmail())).thenReturn(existingUser);
        when(jwt.GenerateToken(anyString(), anyString(), anyString())).thenReturn("generated-jwt-token");

        AuthResponse response = LoginService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-jwt-token");
        assertThat(response.getMessage()).isEqualTo("login successful");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(response.getUser().getId()).isEqualTo(existingUser.getId());
    }

    @Test
    void login_withUnknownUser_throwsUnauthorized() {
        AuthRequest request = new AuthRequest("unknown@example.com", RAW_PASSWORD);
        when(repository.findByEmail("unknown@example.com")).thenReturn(null);

        assertThatThrownBy(() -> LoginService.login(request))
                .isInstanceOf(Unauthorized.class);
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        AuthRequest request = new AuthRequest(existingUser.getEmail(), "wrongPassword");
        when(repository.findByEmail(existingUser.getEmail())).thenReturn(existingUser);

        assertThatThrownBy(() -> LoginService.login(request))
                .isInstanceOf(Unauthorized.class);
    }
}
