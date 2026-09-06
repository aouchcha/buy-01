package buy01.user.config.Helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import buy01.user.dto.User.Userdto;
import buy01.user.model.Roles;
import buy01.user.model.UserEntity;

class MapperTest {

    @Test
    void mappToUSerDto_convertsAllFieldsCorrectly() {
        UserEntity user = new UserEntity();
        user.setId("user-id-1");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setProfilePictureUrl("https://cdn.example.com/avatar.png");
        user.setRole(Roles.SELLER.toString());
        user.setPassword("should-not-be-exposed");

        Userdto dto = Mapper.MappToUSerDto(user);

        assertThat(dto.getId()).isEqualTo("user-id-1");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(dto.getProfilePictureUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(dto.getRole()).isEqualTo(Roles.SELLER.toString());
    }
}
