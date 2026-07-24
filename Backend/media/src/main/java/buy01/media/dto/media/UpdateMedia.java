package buy01.media.dto.media;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateMedia {
    @NotNull(message = "deleted urls shouldn't be null")
    String[] deletedUrls;
    @NotNull(message = "new images urls shouldn't be null")
    MultipartFile[] newImages;
    String type;
    String userId;
    String productId;
}
