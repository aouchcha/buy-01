package buy01.user.handler.usersHandler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.user.dto.User.Userdto;
import buy01.user.service.usersService.usersService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;




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
   public ResponseEntity<Userdto> getMe() {
       Userdto user = usersService.getUser();
       return ResponseEntity.ok(user);
   }
   
   @PutMapping("/me")
   public ResponseEntity<Userdto> updateMe() {
        Userdto user = usersService.getUser();
       return ResponseEntity.ok(user);
   }
}
