package buy01.user.service.usersService;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.Myforbiden;
import buy01.user.config.Exceptions.MyExeptions.NotFound;
import buy01.user.config.Helpers.Mapper;
import buy01.user.dto.User.UpdateMe;
import buy01.user.dto.User.Userdto;
import buy01.user.dto.kafka.UserDeleted;
// import buy01.user.dto.kafka.MediaUploadEvent;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;
// import buy01.user.service.kafka.MediaEventProducer;

@Service
public class UsersService {
    private final UserRepository UserRepository;
    private final KafkaTemplate<String, Object> kafka;
    // private final MediaEventProducer uploadImage;

    public UsersService(
            UserRepository UserRepository,
            KafkaTemplate<String, Object> kaTemplate
            // ,MediaEventProducer uploadImage
        ) {
        this.UserRepository = UserRepository;
        this.kafka = kaTemplate;
        // this.uploadImage = uploadImage;
    }

    public List<Userdto> getAllUsers() {
        return UserRepository.findAll().stream().map(user -> Mapper.MappToUSerDto(user)).toList();
    }

    public Userdto getUserById(String id) {
        final UserEntity user = UserRepository.findById(id).orElseThrow(() -> new NotFound("user not found"));
        return Mapper.MappToUSerDto(user);
    }

    public Userdto getProfile() {
        final String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        final UserEntity user = UserRepository.findById(userId).orElseThrow(() -> new Myforbiden("profile doesn't exist"));
        final Userdto profile = Mapper.MappToUSerDto(user);
        return profile;
        // return null;
    }

    public Userdto updateProfile(UpdateMe request) {
        final String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = UserRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new NotFound("user not found");
        }
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user = UserRepository.save(user);
        Userdto dto = Mapper.MappToUSerDto(user); 
       return dto;
    }

    public void remove(String userId) {
        final UserEntity user = UserRepository.findById(userId).orElseThrow(() -> new NotFound("user not found"));
        UserRepository.delete(user);
        UserDeleted event = new UserDeleted(userId);
        kafka.send("user.deleted", user.getId(), event);
    }
}
