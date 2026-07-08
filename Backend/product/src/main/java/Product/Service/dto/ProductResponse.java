package Product.Service.dto;


public record ProductResponse(
    String id,
    String name,
    String description,
    double price,
    Integer quantity,
    String userId
) {

}