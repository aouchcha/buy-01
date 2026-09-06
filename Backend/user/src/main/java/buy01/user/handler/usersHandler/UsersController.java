package buy01.user.handler.usersHandler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.user.dto.User.UpdateMe;
import buy01.user.dto.User.Userdto;
import buy01.user.service.usersService.UsersService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    final private UsersService UsersService;

    public UsersController(UsersService UsersService) {
        this.UsersService = UsersService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Userdto>> getAllUsers() {
        List<Userdto> users = UsersService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<Userdto> getProfile() {
        Userdto profile = UsersService.getProfile();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Userdto> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(UsersService.getUserById(id));
    }

    @PutMapping("/me")
    public ResponseEntity<Userdto> updateAvatar(@Valid @RequestBody UpdateMe request) {
        Userdto user = UsersService.updateProfile(request);
        return ResponseEntity.ok().body(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> removeUser(@PathVariable String id) {
        UsersService.remove(id);
        return ResponseEntity.ok("User removed with success");
    }

}
