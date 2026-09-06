package buy01.user.dto.Auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class LoginRequest extends AuthRequest {
    public LoginRequest(String email, String password) {
        super(email, password);
    }
}
