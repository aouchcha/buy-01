package service.orders.dto;

public record OrderItemResponse(
        String productId,
        String productName,
        String sellerId,
        double price,
        int quantity,
        double totalPrice) {
}
