package buy01.user.handler.usersHandler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.user.dto.User.UpdateMe;
import buy01.user.dto.User.Userdto;
import buy01.user.service.usersService.usersService;

@RestController
@RequestMapping("/api/users")
public class usersController {
    final private usersService usersService;

    public usersController(usersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Userdto>> getAllUsers() {
        List<Userdto> users = usersService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<Userdto> getProfile() {
        Userdto profile = usersService.getProfile();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Userdto> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(usersService.getUserById(id));
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateAvatar(@RequestBody UpdateMe request) {
        usersService.updateProfile(request);
        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> removeUser(@PathVariable String id) {
        usersService.remove(id);
        return ResponseEntity.ok("User removed with success");
    }

}
