package buy01.user.service.Auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.Conflict;
import buy01.user.config.Exceptions.MyExeptions.badRequest;
import buy01.user.config.Jwt.Jwt;
import buy01.user.dto.Auth.authResponse;
import buy01.user.dto.Auth.registerRequest;
import buy01.user.dto.User.Userdto;
import buy01.user.model.Roles;
// import buy01.user.dto.kafka.MediaUploadEvent;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;
// import buy01.user.service.kafka.MediaEventProducer;

@Service
public class registerService {
    // private static final Logger log = LoggerFactory.getLogger(registerService.class);
    private final userRepository repository;
    private final Jwt jwtService;
    // private final MediaEventProducer mediaEventProducer;

    public registerService(userRepository repository, Jwt jwtService/* , MediaEventProducer mediaEventProducer*/) {
        this.repository = repository;
        this.jwtService = jwtService;
        // this.mediaEventProducer = mediaEventProducer;
    }

    public authResponse signUp(registerRequest request) {
        if (Roles.ADMIN.toString().equals(request.getRole())) {
            throw new badRequest("Admin role is not allowed for registration");
        }
        try {
            userEntity user = new userEntity();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
            user.setRole(request.getRole());
            userEntity savedUser = repository.save(user);
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
            return new authResponse(token, "user registered successfully", userdto);
        } catch (DuplicateKeyException e) {
            throw new Conflict("The user already exists");
        } catch(Exception e) {
            throw new badRequest("user can't registerd" + e.getMessage());
        }
    }
}
