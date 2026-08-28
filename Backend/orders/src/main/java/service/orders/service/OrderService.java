package service.orders.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import service.orders.exception.EmptyCartException;
import service.orders.exception.OrderNotFoundException;
import service.orders.exception.ProductNotFoundException;
import service.orders.models.CartItems;
import service.orders.models.OrderStatus;
import service.orders.repository.OrderRepository;
import service.orders.repository.OrderStatsRepository;
import service.orders.models.Order;
import service.orders.dto.Analytics;
import service.orders.dto.BestSellingProductDTO;
import service.orders.dto.CreateOrderRequest;
import service.orders.dto.OrderItemResponse;
import service.orders.dto.OrdersResponse;
import service.orders.models.Cart;


@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderStatsRepository orderStatsRepository;

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


    public void deleteOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed orders can be deleted");
        }
        orderRepository.delete(order);
    }

    public Object getAnalytics(String period) {
        final String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        final String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse(null);
        if (userId == null) {
            throw new ProductNotFoundException("User ID is not available in the security context");
        }
        if (role == null) {
            throw new ProductNotFoundException("User role is not available in the security context");
        }

        List<BestSellingProductDTO> products = new ArrayList<>();
        
        if (role.equals("ROLE_SELLER")) {
            products = getSellerAnalytics(userId, getFromTimestamp(period));
        } else if (role.equals("ROLE_BUYER")) {
            products = getBuyerAnalytics(userId, getFromTimestamp(period));
        }
        Analytics analytics = Analytics.builder()
                .bestSellingProducts(products)
                .totalRevenue(0.0)
                .build();
        return analytics;
    }

    private List<BestSellingProductDTO> getSellerAnalytics(String sellerId, long fromTimestamp) {
 
        return orderStatsRepository.getBestSellingProducts(sellerId, fromTimestamp, 5);
    }

    private List<BestSellingProductDTO> getBuyerAnalytics(String buyerId, long fromTimestamp) {

        return orderStatsRepository.getTopBuyedProductByUser(buyerId, fromTimestamp, 5);
    }
    

    private long getFromTimestamp(String period) {
        long now = System.currentTimeMillis();
        return switch (period) {
            case "today" -> now - 24L * 60 * 60 * 1000;
            case "week"  -> now - 7L * 24 * 60 * 60 * 1000;
            case "month" -> now - 30L * 24 * 60 * 60 * 1000;
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };
    }
}
