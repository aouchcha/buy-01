package Product.Service.dto;

public record ItemStockStatus(
    String productId,
    boolean success,
    int requestedQuantity,
    int availableStock,
    String message
) {}