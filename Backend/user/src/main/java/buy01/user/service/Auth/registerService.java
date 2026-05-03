package buy01.user.service.Auth;

import org.apache.catalina.mapper.Mapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.Conflict;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.authResponse;
import buy01.user.dto.Auth.registerRequest;
import buy01.user.dto.User.Userdto;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@Service
public class registerService {
    private final userRepository repository;
    private final Jwt jwtService;

    public registerService(userRepository repository, Jwt jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public authResponse signUp(registerRequest request) {
        try {
            userEntity user = new userEntity();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
            user.setRole(request.getRole());
            userEntity savedUser = repository.save(user);
            String token = jwtService.GenerateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());
            Userdto userdto = new Userdto(savedUser.getId(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getEmail(), savedUser.getProfilePictureUrl(), savedUser.getRole());
            return new authResponse(token, "user registered successfully", userdto);
        } catch (DuplicateKeyException e) {
            throw new Conflict("The user already exists");
        } catch(Exception e) {
            throw new RuntimeException("An error occurred during registration");
        }
    }
}
