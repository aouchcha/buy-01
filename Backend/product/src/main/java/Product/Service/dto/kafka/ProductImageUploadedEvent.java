package Product.Service.dto.kafka;

import java.util.List;

public record ProductImageUploadedEvent(
        String userId,
        String productId,
        List<String> imageUrls
) {}
