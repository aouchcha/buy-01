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

import buy01.user.config.Exceptions.MyExeptions.unauthorized;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.authRequest;
import buy01.user.dto.Auth.authResponse;
import buy01.user.model.Roles;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private Jwt jwt;
    private userRepository repository;
    private loginService loginService;

    private static final String RAW_PASSWORD = "correctPassword123";
    private userEntity existingUser;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        repository = mock(userRepository.class);
        loginService = new loginService(jwt, repository);

        existingUser = new userEntity();
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
        authRequest request = new authRequest(existingUser.getEmail(), RAW_PASSWORD);
        when(repository.findByEmail(existingUser.getEmail())).thenReturn(existingUser);
        when(jwt.GenerateToken(anyString(), anyString(), anyString())).thenReturn("generated-jwt-token");

        authResponse response = loginService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-jwt-token");
        assertThat(response.getMessage()).isEqualTo("login successful");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(response.getUser().getId()).isEqualTo(existingUser.getId());
    }

    @Test
    void login_withUnknownUser_throwsUnauthorized() {
        authRequest request = new authRequest("unknown@example.com", RAW_PASSWORD);
        when(repository.findByEmail("unknown@example.com")).thenReturn(null);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(unauthorized.class);
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        authRequest request = new authRequest(existingUser.getEmail(), "wrongPassword");
        when(repository.findByEmail(existingUser.getEmail())).thenReturn(existingUser);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(unauthorized.class);
    }
}
