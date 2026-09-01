package service.orders.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

// import jakarta.ws.rs.InternalServerErrorException;
import lombok.AllArgsConstructor;
import service.orders.exception.CartItemNotFoundException;
import service.orders.exception.EmptyCartException;
import service.orders.exception.OrderNotFoundException;
import service.orders.exception.ProductNotFoundException;
import service.orders.models.CartItems;
import service.orders.models.OrderStatus;
import service.orders.repository.CartItemsRepository;
import service.orders.repository.OrderRepository;
import service.orders.repository.OrderStatsRepository;
import service.orders.models.Order;
import service.orders.client.ProductClient;
import service.orders.dto.Analytics;
import service.orders.dto.BestSellingProductDTO;
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
    private final OrderStatsRepository orderStatsRepository;
    private final CartItemsRepository cartItemsRepository;

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
    public Analytics getAnalytics(String period) {
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
        double total = 0.0;
        
        if (role.equals("ROLE_SELLER")) {
            products = getSellerAnalytics(userId, getFromTimestamp(period));
            final List<CartItems> cartItems = cartItemsRepository.findBySellerId(userId);
            if (cartItems == null) {
                throw new CartItemNotFoundException("cart Items for a seller is null");
            }
            total = cartItems.stream().mapToDouble(CartItems::getTotalPrice).sum();
        } else if (role.equals("ROLE_BUYER")) {
            products = getBuyerAnalytics(userId, getFromTimestamp(period));
            final List<Order> orders = orderRepository.findByUserIdAndStatus(userId, "DELIVERED");
            if (orders == null) {
                throw new OrderNotFoundException("orders for a client is null");
            }
            total = orders.stream().mapToDouble(Order::getTotalAmount).sum();
        } else {
            System.out.println(role);
            throw new OrderNotFoundException("The Role is not valid when check for the analytics");
        }
        Analytics analytics = Analytics.builder()
                .bestSellingProducts(products)
                .total(total)
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
