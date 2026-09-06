package buy01.user.dto.Auth;

import buy01.user.dto.User.Userdto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String message;
    private Userdto user;
}
