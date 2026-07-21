package buy01.media.config.Exceptions.GlobalExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import buy01.media.config.Exceptions.MyExeptions.MyBadRequest;

@RestControllerAdvice
public class BadRequestHandler {
    @ExceptionHandler(MyBadRequest.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> bad(String message) {
        return ResponseEntity.badRequest().body(message);
    }
}
