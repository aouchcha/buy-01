package buy01.user.service.usersService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import buy01.user.config.Exceptions.MyExeptions.Myforbiden;
import buy01.user.config.Exceptions.MyExeptions.NotFound;
import buy01.user.dto.User.UpdateMe;
import buy01.user.dto.User.Userdto;
import buy01.user.model.Roles;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    private static final String CURRENT_USER_ID = "current-user-id";

    private UserRepository UserRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private UsersService UsersService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        UserRepository = mock(UserRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        UsersService = new UsersService(UserRepository, kafkaTemplate);

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(CURRENT_USER_ID);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserEntity buildUser() {
        UserEntity user = new UserEntity();
        user.setId(CURRENT_USER_ID);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setRole(Roles.BUYER.toString());
        return user;
    }

    @Test
    void getProfile_whenUserExists_returnsUserdto() {
        UserEntity user = buildUser();
        when(UserRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));

        Userdto profile = UsersService.getProfile();

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(CURRENT_USER_ID);
        assertThat(profile.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getProfile_whenUserDoesNotExist_throwsMyforbiden() {
        when(UserRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> UsersService.getProfile())
                .isInstanceOf(Myforbiden.class);
    }

    @Test
    void updateProfile_whenUserExists_updatesAndReturnsUserdto() {
        UserEntity user = buildUser();
        UpdateMe request = new UpdateMe();
        request.setFirstName("Updated");
        request.setLastName("Name");

        when(UserRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));
        when(UserRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Userdto result = UsersService.updateProfile(request);

        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getLastName()).isEqualTo("Name");
    }

    @Test
    void updateProfile_whenUserDoesNotExist_throwsNotFound() {
        UpdateMe request = new UpdateMe();
        request.setFirstName("Updated");
        request.setLastName("Name");

        when(UserRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> UsersService.updateProfile(request))
                .isInstanceOf(NotFound.class);
    }

    @Test
    void remove_whenUserExists_deletesUserAndPublishesEvent() {
        UserEntity user = buildUser();
        when(UserRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));

        UsersService.remove(CURRENT_USER_ID);

        verify(UserRepository, times(1)).delete(user);
        verify(kafkaTemplate, times(1)).send(eq("user.deleted"), eq(CURRENT_USER_ID), any());
    }

    @Test
    void remove_whenUserDoesNotExist_throwsNotFound() {
        when(UserRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> UsersService.remove("missing-id"))
                .isInstanceOf(NotFound.class);
    }
}
