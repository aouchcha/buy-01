package Product.Service.dto.kafka;

public record ProductCreated(
    String productId,
    String ownerId
) {}
