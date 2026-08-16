package service.orders.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import service.orders.models.CartItems;
import service.orders.models.Order;
import service.orders.models.OrderStatus;
import service.orders.models.PaymentMethod;
import service.orders.repository.OrderRepository;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrdersController {

    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {

        List<CartItems> cartItems = request.items().stream()
                .map(item -> CartItems.builder()
                        .productId(item.productId())
                        .productName(item.productName())
                        .price(item.price())
                        .quantity(item.quantity())
                        .totalPrice(item.price() * item.quantity())
                        .build())
                .toList();

        double totalAmount = cartItems.stream().mapToDouble(CartItems::getTotalPrice).sum();

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .city(request.city())
                .address(request.address())
                .createdAt(System.currentTimeMillis())
                .cartItems(cartItems)
                .totalAmount(totalAmount)
                .build();

        order = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {

        return orderRepository.findByIdAndUserId(id, userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found"));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    public record CreateOrderItemRequest(
            @NotBlank(message = "productId is required") String productId,

            @NotBlank(message = "productName is required") String productName,

            @NotNull(message = "price is required")
            @Positive(message = "price must be greater than 0") Double price,

            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be greater than 0") Integer quantity) {
    }

    public record CreateOrderRequest(
            @NotBlank(message = "fullName is required")
            @Size(min = 2, max = 100, message = "fullName must be between 2 and 100 characters") String fullName,

            @NotBlank(message = "phoneNumber is required")
            @Pattern(regexp = "^\\+?[0-9 ()-]{8,20}$", message = "phoneNumber must be a valid phone number") String phoneNumber,

            @NotBlank(message = "city is required")
            @Size(min = 2, max = 100, message = "city must be between 2 and 100 characters") String city,

            @NotBlank(message = "address is required")
            @Size(min = 5, max = 255, message = "address must be between 5 and 255 characters") String address,

            @NotEmpty(message = "items must not be empty")
            @Valid List<CreateOrderItemRequest> items) {
    }
}
