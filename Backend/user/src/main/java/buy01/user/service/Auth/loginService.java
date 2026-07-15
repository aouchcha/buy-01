package buy01.user.service.Auth;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.unauthorized;
import buy01.user.config.Jwt.JwtService;
import buy01.user.dto.Auth.authRequest;
import buy01.user.dto.Auth.authResponse;
import buy01.user.dto.User.Userdto;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@Service
public class loginService {
    private final JwtService jwt;
    private final userRepository repository;

    public loginService(JwtService jwt, userRepository repository) {
        this.jwt = jwt;
        this.repository = repository;
    }

    public authResponse login(authRequest request) {
        final String email = request.getEmail();
        final String password = request.getPassword();
        final userEntity user = repository.findByEmail(email);
        if (user == null) {
            throw new unauthorized("Incorrect email or password");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new unauthorized("Incorrect email or password");
        }
        String token = jwt.generateToken(user.getEmail(), user.getRole(), user.getId());
        Userdto userdto = new Userdto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getProfilePictureUrl(), user.getRole());
        return new authResponse(token, "login successful", userdto);
    }
}
