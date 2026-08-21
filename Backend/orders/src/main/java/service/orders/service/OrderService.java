package service.orders.service;

import java.util.List;

import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import service.orders.exception.EmptyCartException;
import service.orders.exception.OrderNotFoundException;
import service.orders.models.CartItems;
import service.orders.models.OrderStatus;
import service.orders.repository.OrderRepository;
import service.orders.models.Order;
import service.orders.dto.CreateOrderRequest;
import service.orders.dto.OrderItemResponse;
import service.orders.dto.OrdersResponse;
import service.orders.models.Cart;


@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    public OrdersResponse createOrder(CreateOrderRequest request, String userId) {

        Cart cart = cartService.getCart(userId);

        List<CartItems> cartItems = cart.getCartItems();

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cannot create an order from an empty cart");
        }

        double totalAmount = cartItems.stream().mapToDouble(CartItems::getTotalPrice).sum();

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .fullName(request.shippingAddress().fullName())
                .phoneNumber(request.shippingAddress().phone())
                .city(request.shippingAddress().city())
                .postalCode(request.shippingAddress().postalCode())
                .address(request.shippingAddress().address())
                .createdAt(System.currentTimeMillis())
                .cartItems(cartItems)
                .totalAmount(totalAmount)
                .build();

        order = orderRepository.save(order);
        cartService.clearCart(userId);
        return toResponse(order);
    }

    public List<OrdersResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }


    public OrdersResponse getOrderByIdAndUserId(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    private OrdersResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getCartItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getSellerId(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getTotalPrice()))
                .toList();

        return new OrdersResponse(
                order.getId(),
                order.getUserId(),
                order.getFullName(),
                order.getPhoneNumber(),
                order.getCity(),
                order.getAddress(),
                order.getPostalCode(),
                order.getStatus().name(),
                order.getPaymentMethod().name(),
                order.getCreatedAt(),
                order.getTotalAmount(),
                items
        );
    }
}
