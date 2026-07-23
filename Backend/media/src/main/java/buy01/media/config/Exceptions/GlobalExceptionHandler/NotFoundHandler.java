package buy01.media.config.Exceptions.GlobalExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import buy01.media.config.Exceptions.MyExeptions.MyNotFound;

@RestControllerAdvice
public class NotFoundHandler {
    @ExceptionHandler(MyNotFound.class)
    public ResponseEntity<String> handNotFound(MyNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
