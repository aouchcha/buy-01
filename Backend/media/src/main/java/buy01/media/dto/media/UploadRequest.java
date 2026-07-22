package buy01.media.dto.media;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
// @NoArgsConstructor
public class UploadRequest {
    private String userId;
    private String productId;
    @NotNull(message = "the type shouldn't be null")
    @NotEmpty(message = "the type shouldn't be empty")
    private String type;
    @NotNull(message = "the pictures array is null")
    private MultipartFile[] pictures;
}
