package Product.Service.dto.kafka;

public record ProductImageUploadedEvent(
        String productId,
        String imageUrl
) {}
