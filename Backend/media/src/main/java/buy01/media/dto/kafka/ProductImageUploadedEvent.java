package buy01.media.dto.kafka;

import java.util.List;

public record ProductImageUploadedEvent(
    String userId,
    String productId, 
    List<String> imageUrls
)
{}