package service.orders.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import service.orders.models.Order;
import service.orders.models.OrderStatus;
import service.orders.repository.OrderRepository;

@Service
public class OrderStatusSimulationService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusSimulationService.class);

    private final OrderRepository orderRepository;
    private final long confirmedDelaySeconds;
    private final long shippedDelaySeconds;
    private final long deliveredDelaySeconds;

    public OrderStatusSimulationService(
            OrderRepository orderRepository,
            @Value("${order.simulation.confirmed-delay}") long confirmedDelaySeconds,
            @Value("${order.simulation.shipped-delay}") long shippedDelaySeconds,
            @Value("${order.simulation.delivered-delay}") long deliveredDelaySeconds) {
        this.orderRepository = orderRepository;
        this.confirmedDelaySeconds = confirmedDelaySeconds;
        this.shippedDelaySeconds = shippedDelaySeconds;
        this.deliveredDelaySeconds = deliveredDelaySeconds;
    }

    @Scheduled(fixedRateString = "${order.simulation.poll-interval-ms}")
    public void advanceOrderStatuses() {
        long now = System.currentTimeMillis();
        List<Order> candidates = orderRepository.findByStatusIn(
                List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPED));

        for (Order order : candidates) {
            try {
                applyTransition(order, now);
            } catch (Exception e) {
                log.error("Failed to advance status for order #{}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void applyTransition(Order order, long now) {
        long elapsedSeconds = (now - order.getCreatedAt()) / 1000;
        OrderStatus current = order.getStatus();
        OrderStatus next = null;

        if (current == OrderStatus.PENDING && elapsedSeconds >= confirmedDelaySeconds) {
            next = OrderStatus.CONFIRMED;
        } else if (current == OrderStatus.CONFIRMED
                && elapsedSeconds >= confirmedDelaySeconds + shippedDelaySeconds) {
            next = OrderStatus.SHIPPED;
        } else if (current == OrderStatus.SHIPPED
                && elapsedSeconds >= confirmedDelaySeconds + shippedDelaySeconds + deliveredDelaySeconds) {
            next = OrderStatus.DELIVERED;
        }

        if (next != null) {
            order.setStatus(next);
            orderRepository.save(order);
            log.info("Order #{} status changed: {} -> {}", order.getId(), current, next);
        }
    }
}
