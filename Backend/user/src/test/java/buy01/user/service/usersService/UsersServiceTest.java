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

import buy01.user.config.Exceptions.MyExeptions.Conflict;
import buy01.user.config.Exceptions.MyExeptions.Myforbiden;
import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.dto.User.UpdateMe;
import buy01.user.dto.User.Userdto;
import buy01.user.model.Roles;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    private static final String CURRENT_USER_ID = "current-user-id";

    private userRepository userRepository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private usersService usersService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userRepository = mock(userRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        usersService = new usersService(userRepository, kafkaTemplate);

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

    private userEntity buildUser() {
        userEntity user = new userEntity();
        user.setId(CURRENT_USER_ID);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setRole(Roles.BUYER.toString());
        return user;
    }

    @Test
    void getProfile_whenUserExists_returnsUserdto() {
        userEntity user = buildUser();
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));

        Userdto profile = usersService.getProfile();

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(CURRENT_USER_ID);
        assertThat(profile.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getProfile_whenUserDoesNotExist_throwsMyforbiden() {
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersService.getProfile())
                .isInstanceOf(Conflict.class);
    }

    @Test
    void updateProfile_whenUserExists_updatesAndReturnsUserdto() {
        userEntity user = buildUser();
        UpdateMe request = new UpdateMe();
        request.setFirstName("Updated");
        request.setLastName("Name");

        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(userEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Userdto result = usersService.updateProfile(request);

        assertThat(result.getFirstName()).isEqualTo("Updated");
        assertThat(result.getLastName()).isEqualTo("Name");
    }

    @Test
    void updateProfile_whenUserDoesNotExist_throwsNotFound() {
        UpdateMe request = new UpdateMe();
        request.setFirstName("Updated");
        request.setLastName("Name");

        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersService.updateProfile(request))
                .isInstanceOf(notFound.class);
    }

    @Test
    void remove_whenUserExists_deletesUserAndPublishesEvent() {
        userEntity user = buildUser();
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(user));

        usersService.remove(CURRENT_USER_ID);

        verify(userRepository, times(1)).delete(user);
        verify(kafkaTemplate, times(1)).send(eq("user.deleted"), eq(CURRENT_USER_ID), any());
    }

    @Test
    void remove_whenUserDoesNotExist_throwsNotFound() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usersService.remove("missing-id"))
                .isInstanceOf(notFound.class);
    }
}
