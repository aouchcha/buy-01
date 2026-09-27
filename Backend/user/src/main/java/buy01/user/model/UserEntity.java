package buy01.user.model;

import javax.validation.constraints.Pattern;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class UserEntity {
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private String id;

    private String firstName;
    private String lastName;
    @Indexed(unique = true)
    private String email;
    private String password;
    private String profilePictureUrl;
    private String role;
}
