package buy01.media.dto.media;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
// @NoArgsConstructor
public class UploadRequest {
    private String userId;
    private String productId; 
    private MultipartFile[] pictures;
}
