package buy01.media.dto.media;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
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
    @NotEmpty(message = "the pictures array shouldn't be empty")
    //TODO: Add validation for the size of pictures every one should be at max 2MB
    @Size(max = 3, message = "the pictures array shouldn't have more than 3 pictures")
    // @MaxFileSize(maxSize = 2 * 1024 * 1024, message = "each picture should be at max 2MB")
    private MultipartFile[] pictures;
}
