package service.orders.dto;

import java.util.List;

public record OrdersResponse(
        String id,
        String userId,
        String fullName,
        String phoneNumber,
        String city,
        String address,
        String postalCode,
        String status,
        String paymentMethod,
        long createdAt,
        double totalAmount,
        List<OrderItemResponse> cartItems) {
}
