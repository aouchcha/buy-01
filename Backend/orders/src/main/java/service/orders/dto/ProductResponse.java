package service.orders.dto;

import java.util.List;

public record ProductResponse(
    String id,
    String name,
    String description,
    double price,
    Integer quantity,
    String userId,
    List<String> imageUrls
) {

}
