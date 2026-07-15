package buy01.user.service.usersService;

import java.util.List;

import org.springframework.stereotype.Service;

import buy01.user.config.Exceptions.MyExeptions.notFound;
import buy01.user.dto.User.UserUpdateRequest;
import buy01.user.dto.User.Userdto;
import buy01.user.repository.userRepository;
import buy01.user.model.userEntity;


@Service
public class usersService {
    private final userRepository userRepository;

    public usersService(userRepository userRepository) {
        this.userRepository = userRepository;
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

    public Userdto updateMe(String id, UserUpdateRequest request) {
        userEntity user = userRepository.findById(id)
                .orElseThrow(() -> new notFound("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setProfilePictureUrl(request.profilePictureUrl());

        userRepository.save(user);
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
