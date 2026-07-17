package buy01.user.service.usersService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.dto.User.UserUpdateRequest;
import buy01.user.dto.User.Userdto;
import buy01.user.dto.kafka.MediaUploadEvent;
import buy01.user.repository.userRepository;
import buy01.user.model.userEntity;
import buy01.user.service.kafka.MediaEventProducer;


@Service
public class usersService {
    private static final Logger log = LoggerFactory.getLogger(usersService.class);
    private final userRepository userRepository;
    private final MediaEventProducer mediaEventProducer;

    public usersService(userRepository userRepository, MediaEventProducer mediaEventProducer) {
        this.userRepository = userRepository;
        this.mediaEventProducer = mediaEventProducer;
    }

    public List<Userdto> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            Userdto dto = new Userdto();
            dto.setId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            return dto;
        }).toList();
    }

    public Userdto getUser(String id) {
        userEntity user = userRepository.findById(id)
                .orElseThrow(() -> new notFound("User not found"));

        return toDto(user);
    }

    public Userdto updateMe(String id, UserUpdateRequest request, MultipartFile file) {
        userEntity user = userRepository.findById(id)
                .orElseThrow(() -> new notFound("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        userRepository.save(user);

        if (file != null && !file.isEmpty()) {
            try {
                mediaEventProducer.publishMediaUploadEvent(new MediaUploadEvent(
                    id,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
                ));
                log.info("Avatar upload event sent for user: {}", id);
            } catch (Exception e) {
                log.error("Failed to send avatar upload event for user {}: {}", id, e.getMessage());
            }
        }

        return toDto(user);
    }

    private Userdto toDto(userEntity user) {
        return new Userdto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfilePictureUrl(),
                user.getRole());
    }
}
