package Product.Service.dto.kafka;

public record ProductImageDeletedEvent(
    String productId,
    String userId,
    String url
) {
    
}
