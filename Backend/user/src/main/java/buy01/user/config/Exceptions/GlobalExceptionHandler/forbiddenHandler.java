package buy01.user.config.Exceptions.GlobalExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import buy01.user.config.Exceptions.MyExeptions.Myforbiden;

@RestControllerAdvice
public class forbiddenHandler {
    @ExceptionHandler(Myforbiden.class)
    public ResponseEntity<String> handleFrobiden(Myforbiden ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
