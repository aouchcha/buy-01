package buy01.user.service.usersService;

import java.util.List;

import org.springframework.stereotype.Service;

import buy01.user.dto.User.Userdto;
import buy01.user.repository.userRepository;

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
}
