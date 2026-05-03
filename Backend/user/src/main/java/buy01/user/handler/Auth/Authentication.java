package buy01.user.handler.Auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.user.dto.Auth.authResponse;
import buy01.user.dto.Auth.loginRequest;
import buy01.user.dto.Auth.registerRequest;
import buy01.user.service.Auth.loginService;
import buy01.user.service.Auth.registerService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class Authentication {
    private final registerService registerService;
    private final loginService loginService;

    public Authentication(registerService registerService, loginService loginService) {
        this.registerService = registerService;
        this.loginService = loginService;
    }

    @PostMapping("/signup")
    public ResponseEntity<authResponse> signUp(@Valid @RequestBody registerRequest request) {
        authResponse response = registerService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<authResponse> login(@Valid @RequestBody loginRequest request) {
        authResponse response = loginService.login(request);
        return ResponseEntity.ok(response);
    }
}
