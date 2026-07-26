package buy01.media.dto.media;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;


@Data
public class UpdateMedia {
    // @NotNull(message = "deleted urls shouldn't be null")
    String[] deletedUrls;
    // @NotNull(message = "new images urls shouldn't be null")
    MultipartFile[] newImages;
    String type;
    String userId;
    String productId;
}
