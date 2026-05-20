package buy01.media.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("media-service is running");
    }
}