package buy01.media.handler;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import buy01.media.dto.media.MediaResponse;
import buy01.media.dto.media.UpdateMedia;
import buy01.media.dto.media.UploadRequest;
import buy01.media.service.media.MediaService;
// import buy01.media.service.media.Upload;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService  = mediaService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<MediaResponse>> uploadMedia(@Valid @ModelAttribute UploadRequest pictures) {
        List<MediaResponse> dtos = mediaService.UploadPics(pictures);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    public ResponseEntity<List<MediaResponse>> getAllMedia() {
        List<MediaResponse> medias = mediaService.getAll();
        return ResponseEntity.ok(medias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> getMedia(@PathVariable String id) {
        MediaResponse dto = mediaService.getMedia(id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable String id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.ok("Media removed Successfully");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("media-service is running");
    }

    @PutMapping("/images")
    public ResponseEntity<List<MediaResponse>> updatePiture(@Valid @ModelAttribute UpdateMedia request) {
        List<MediaResponse> responses = mediaService.updateMedia(request);
        return ResponseEntity.ok(responses);
    }
}