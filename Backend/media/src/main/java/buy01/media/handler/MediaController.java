package buy01.media.handler;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.media.dto.media.UploadRequest;
import buy01.media.service.media.Upload;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final Upload uploadService;

    public MediaController(Upload uploadService) {
        this.uploadService  = uploadService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadMedia(@ModelAttribute UploadRequest pictures) {
        uploadService.UploadPics(pictures);
        return ResponseEntity.ok("Uploaded Successfuly");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("media-service is running");
    }
}