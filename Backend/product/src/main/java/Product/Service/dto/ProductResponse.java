package Product.Service.dto;

import java.util.List;
import Product.Service.model.Category;

public record ProductResponse(
    String id,
    String name,
    String description,
    double price,
    Integer quantity,
    String userId,
    Category category,
    List<String> imageUrls
) {

}