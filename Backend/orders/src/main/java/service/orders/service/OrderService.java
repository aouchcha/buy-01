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
import service.orders.client.ProductClient;
import service.orders.dto.CreateOrderRequest;
import service.orders.dto.OrderItemResponse;
import service.orders.dto.OrdersResponse;
import service.orders.dto.stockRequests;
import service.orders.models.Cart;

@Service
@AllArgsConstructor
public class OrderService {

    private static final String ORDER_NOT_FOUND = "Order not found: ";

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductClient productClient;

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

        productClient.updateProductStock(toStockRequests(cartItems));

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
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND + orderId));
        return toResponse(order);
    }

    private List<stockRequests> toStockRequests(List<CartItems> cartItems) {
        return cartItems.stream()
                .map(item -> new stockRequests(item.getProductId(), item.getQuantity()))
                .toList();
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
                items);
    }

    public void deleteOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND + orderId));
        if (order.getStatus() == OrderStatus.DELIVERED && order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only pending or confirmed orders can be deleted");
        }
        productClient.restockProductStock(toStockRequests(order.getCartItems()));
        orderRepository.delete(order);
    }

    public OrdersResponse cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND + orderId));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed orders can be cancelled");
        }
        productClient.restockProductStock(toStockRequests(order.getCartItems()));
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        return toResponse(order);
    }
}
