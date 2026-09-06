package buy01.user.service.Auth;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.Conflict;
import buy01.user.config.Exceptions.MyExeptions.BadRequest;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.AuthResponse;
import buy01.user.dto.Auth.RegisterRequest;
import buy01.user.dto.User.Userdto;
import buy01.user.model.Roles;
// import buy01.user.dto.kafka.MediaUploadEvent;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;
// import buy01.user.service.kafka.MediaEventProducer;

@Service
public class RegisterService {
    // private static final Logger log = LoggerFactory.getLogger(RegisterService.class);
    private final UserRepository repository;
    private final Jwt jwtService;
    // private final MediaEventProducer mediaEventProducer;

    public RegisterService(UserRepository repository, Jwt jwtService/* , MediaEventProducer mediaEventProducer*/) {
        this.repository = repository;
        this.jwtService = jwtService;
        // this.mediaEventProducer = mediaEventProducer;
    }

    public AuthResponse signUp(RegisterRequest request) {
        if (Roles.ADMIN.toString().equals(request.getRole())) {
            throw new BadRequest("Admin role is not allowed for registration");
        }
        try {
            UserEntity user = new UserEntity();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
            user.setRole(request.getRole());
            UserEntity savedUser = repository.save(user);
            // if (request.getProfilePicture() != null && !request.getProfilePicture().isEmpty()) {
            //         mediaEventProducer.publishMediaUploadEvent(
            //             new MediaUploadEvent(
            //             savedUser.getId(),
            //             "Avatar/" + request.getProfilePicture().getOriginalFilename(),
            //             request.getProfilePicture().getBytes()
            //         )
            //     );
            //     log.info("Publishing media upload event for user: {}", savedUser.getEmail());
            // }
            String token = jwtService.GenerateToken(savedUser.getEmail(), savedUser.getRole(), savedUser.getId());
            Userdto userdto = new Userdto(savedUser.getId(), savedUser.getFirstName(), savedUser.getLastName(), savedUser.getEmail(), savedUser.getProfilePictureUrl(), savedUser.getRole());
            return new AuthResponse(token, "user registered successfully", userdto);
        } catch (DuplicateKeyException e) {
            throw new Conflict("The user already exists");
        } catch(Exception e) {
            throw new BadRequest("user can't registerd" + e.getMessage());
        }
    }
}
