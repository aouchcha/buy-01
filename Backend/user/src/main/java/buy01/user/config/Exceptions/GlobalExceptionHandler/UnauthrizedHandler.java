package buy01.user.config.Exceptions.GlobalExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import buy01.user.config.Exceptions.MyExeptions.unauthorized;

@RestControllerAdvice
public class unauthrizedHandler {
    @ExceptionHandler(unauthorized.class)
    public ResponseEntity<String> handleUnauthorizedException(unauthorized ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}
