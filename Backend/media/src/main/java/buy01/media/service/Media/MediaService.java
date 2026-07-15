package buy01.media.service.Media;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import buy01.media.dto.Media.MediaResponse;

@Service
public class MediaService {

    public MediaResponse uploadProductImage(Long productId, MultipartFile file) {
        // 1. Validate image
        // 2. Store file (disk, S3, Cloudinary, etc.)
        // 3. Save image URL to product
        // 4. Return response

        return new MediaResponse(
                "Image uploaded successfully",
                "https://picsum.photos/536/354"
        );
    }

    public MediaResponse uploadAvatar(MultipartFile file) {
        // Validate
        // Store
        // Update current user's avatar

        return new MediaResponse(
                "Avatar uploaded successfully",
                "https://picsum.photos/536/354"
        );
    }
}