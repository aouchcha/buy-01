package buy01.user.model;

import org.hibernate.validator.constraints.UUID;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class UserEntity {
    @UUID
    private String id;

    private String firstName;
    private String lastName;
    @Indexed(unique = true)
    private String email;
    private String password;
    private String profilePictureUrl;
    private String role;
}
