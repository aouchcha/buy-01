package buy01.media.config.Exceptions.GlobalExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InternalServerError {
    @ExceptionHandler(InternalError.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleInternal(InternalError err) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("internal server error");
    }
}
