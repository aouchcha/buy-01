package buy01.user.handler.usersHandler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import buy01.user.dto.User.UserUpdateRequest;
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
    public ResponseEntity<Userdto> getMe(@AuthenticationPrincipal Jwt jwt) {
          String userId = jwt.getSubject();
        Userdto user = usersService.getUser(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping(value = "/me", consumes = "multipart/form-data")
    public ResponseEntity<Userdto> updateMe(
            @RequestPart("data") UserUpdateRequest data,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Userdto user = usersService.updateMe(userId, data, file);
        return ResponseEntity.ok(user);
    }
}
