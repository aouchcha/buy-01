package buy01.user.dto.User;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Userdto {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePictureUrl;
    private String role;
}
