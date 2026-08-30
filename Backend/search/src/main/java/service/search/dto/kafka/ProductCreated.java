package service.search.dto.kafka;

import java.time.LocalDateTime;
import java.util.List;

public record ProductCreated(
    String productId,
    String name,
    String description,
    double price,
    Integer quantity,
    String userId,
    String category,
    List<String> imageUrls,
    LocalDateTime createdAt
) {}
