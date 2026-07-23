package buy01.user.service.usersService;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.config.Helpers.Mapper;
import buy01.user.dto.User.UpdateAvatar;
import buy01.user.dto.User.Userdto;
// import buy01.user.dto.kafka.MediaUploadEvent;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;
// import buy01.user.service.kafka.MediaEventProducer;

@Service
public class usersService {
    private final userRepository userRepository;
    // private final MediaEventProducer uploadImage;

    public usersService(
            userRepository userRepository
            // ,MediaEventProducer uploadImage
        ) {
        this.userRepository = userRepository;
        // this.uploadImage = uploadImage;
    }

    public List<Userdto> getAllUsers() {
        return userRepository.findAll().stream().map(user -> Mapper.MappToUSerDto(user)).toList();
    }

    public Userdto getProfile() {
        final String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final userEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new notFound("user not found");
        }
        final Userdto profile = Mapper.MappToUSerDto(user);
        return profile;
        // return null;
    }

    public Userdto getUserById(String id) {
        final userEntity user = userRepository.findById(id)
                .orElseThrow(() -> new notFound("user not found"));
        return Mapper.MappToUSerDto(user);
    }

    public void updateProfile(UpdateAvatar request) {
        final String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final userEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new notFound("user not found");
        }
        
        // try {

        //     uploadImage.publishMediaUploadEvent(
        //         new MediaUploadEvent(
        //             user.getId(),
        //             request.getImage().getName(),
        //             request.getImage().getBytes()
        //         )
        //     );
        // } catch (Exception e) {
        //     // TODO: handle exception
        // }
    }
}
