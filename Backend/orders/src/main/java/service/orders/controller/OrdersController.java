package service.orders.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import service.orders.service.OrderService;
import service.orders.dto.CreateOrderRequest;
import service.orders.dto.OrdersResponse;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrdersController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrdersResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") String userId) {

        OrdersResponse order = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdersResponse> getOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        OrdersResponse order = orderService.getOrderByIdAndUserId(id, userId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrdersResponse>> getOrders(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        orderService.deleteOrder(id, userId);
        return ResponseEntity.noContent().build();
    }

    
}
