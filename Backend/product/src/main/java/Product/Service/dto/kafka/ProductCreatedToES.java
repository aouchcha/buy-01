package Product.Service.dto.kafka;

import java.util.List;

public record ProductCreatedToES(
    String productId,
    String name,
    String description,
    double price,
    Integer quantity,
    String userId,
    String category,
    List<String> imageUrls
) {}
