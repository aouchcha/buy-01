package buy01.media.config.Exceptions.GlobalExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import buy01.media.config.Exceptions.MyExeptions.MyForbiden;

@RestControllerAdvice
public class MyForbiddenHandelr {
    @ExceptionHandler(MyForbiden.class)
    // @Status
    public ResponseEntity<String> handlForbidden(MyForbiden ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
