package buy01.media.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import buy01.media.dto.Media.MediaResponse;
import buy01.media.service.Media.MediaService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("POST works");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadProductImage(
            @RequestParam("productId") Long productId,
            @RequestParam("file") MultipartFile file) {

        MediaResponse response = mediaService.uploadProductImage(productId, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file) {

        MediaResponse response = mediaService.uploadAvatar(file);
        return ResponseEntity.ok(response);
    }

    // @GetMapping("/health")
    // public ResponseEntity<String> health() {
    // return ResponseEntity.ok("media-service is running");
    // }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("HELLO NEW VERSION");
    }
}