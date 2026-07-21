package buy01.user.handler.usersHandler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.user.dto.User.UpdateAvatar;
import buy01.user.dto.User.Userdto;
import buy01.user.service.usersService.usersService;

@RestController
@RequestMapping("/api/users")
public class usersController {
    final private usersService usersService;

    public usersController(usersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Userdto>> getAllUsers() {
        List<Userdto> users = usersService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<Userdto> getProfile() {
        Userdto profile = usersService.getProfile();
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateAvatar(@ModelAttribute UpdateAvatar request) {
        usersService.updateProfile(request);
        return ResponseEntity.ok().body(null);
    }
}
