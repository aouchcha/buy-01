package buy01.user.service.usersService;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.Myforbiden;
import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.config.Helpers.Mapper;
import buy01.user.dto.User.UpdateMe;
import buy01.user.dto.User.Userdto;
import buy01.user.dto.kafka.UserDeleted;
// import buy01.user.dto.kafka.MediaUploadEvent;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;
// import buy01.user.service.kafka.MediaEventProducer;

@Service
public class usersService {
    private final userRepository userRepository;
    private final KafkaTemplate<String, Object> kafka;
    // private final MediaEventProducer uploadImage;

    public usersService(
            userRepository userRepository,
            KafkaTemplate<String, Object> kaTemplate
            // ,MediaEventProducer uploadImage
        ) {
        this.userRepository = userRepository;
        this.kafka = kaTemplate;
        // this.uploadImage = uploadImage;
    }

    public List<Userdto> getAllUsers() {
        return userRepository.findAll().stream().map(user -> Mapper.MappToUSerDto(user)).toList();
    }

    public Userdto getUserById(String id) {
        final userEntity user = userRepository.findById(id).orElseThrow(() -> new notFound("user not found"));
        return Mapper.MappToUSerDto(user);
    }

    public Userdto getProfile() {
        final String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        final userEntity user = userRepository.findById(userId).orElseThrow(() -> new Myforbiden("profile doesn't exist"));
        final Userdto profile = Mapper.MappToUSerDto(user);
        return profile;
        // return null;
    }

    public Userdto updateProfile(UpdateMe request) {
        final String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        userEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new notFound("user not found");
        }
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user = userRepository.save(user);
        Userdto dto = Mapper.MappToUSerDto(user); 
       return dto;
    }

    public void remove(String userId) {
        final userEntity user = userRepository.findById(userId).orElseThrow(() -> new notFound("user not found"));
        userRepository.delete(user);
        UserDeleted event = new UserDeleted(userId);
        kafka.send("user.deleted", user.getId(), event);
    }
}
