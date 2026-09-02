package buy01.user.handler.Auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.user.dto.Auth.AuthResponse;
import buy01.user.dto.Auth.LoginRequest;
import buy01.user.dto.Auth.RegisterRequest;
import buy01.user.service.Auth.LoginService;
import buy01.user.service.Auth.RegisterService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class Authentication {
    private final RegisterService RegisterService;
    private final LoginService LoginService;

    public Authentication(RegisterService RegisterService, LoginService LoginService) {
        this.RegisterService = RegisterService;
        this.LoginService = LoginService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = RegisterService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = LoginService.login(request);
        return ResponseEntity.ok(response);
    }
}
