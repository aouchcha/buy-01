package buy01.user.service.Auth;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.Unauthorized;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.AuthRequest;
import buy01.user.dto.Auth.AuthResponse;
import buy01.user.dto.User.Userdto;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;

@Service
public class LoginService {
    private final Jwt jwt;
    private final UserRepository repository;

    public LoginService(Jwt jwt, UserRepository repository) {
        this.jwt = jwt;
        this.repository = repository;
    }

    public AuthResponse login(AuthRequest request) {
        final String email = request.getEmail();
        final String password = request.getPassword();
        final UserEntity user = repository.findByEmail(email);
        if (user == null) {
            throw new Unauthorized("bad cridentials user is null");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new Unauthorized("bad cridentials password not match");
        }
        String token = jwt.GenerateToken(user.getEmail(), user.getRole(), user.getId());
        Userdto userdto = new Userdto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getProfilePictureUrl(), user.getRole());
        return new AuthResponse(token, "login successful", userdto);
    }
}
