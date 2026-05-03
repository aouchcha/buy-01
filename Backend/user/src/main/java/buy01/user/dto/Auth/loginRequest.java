package buy01.user.dto.Auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class loginRequest extends authRequest {
    public loginRequest(String email, String password) {
        super(email, password);
    }
}
