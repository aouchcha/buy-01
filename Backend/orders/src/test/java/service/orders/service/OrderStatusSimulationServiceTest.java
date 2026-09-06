package service.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import service.orders.models.Order;
import service.orders.models.OrderStatus;
import service.orders.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderStatusSimulationServiceTest {

    private static final long CONFIRMED_DELAY = 30L;
    private static final long SHIPPED_DELAY = 60L;
    private static final long DELIVERED_DELAY = 90L;

    private OrderRepository orderRepository;
    private OrderStatusSimulationService simulationService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        simulationService = new OrderStatusSimulationService(
                orderRepository, CONFIRMED_DELAY, SHIPPED_DELAY, DELIVERED_DELAY);
    }

    private Order orderWithAge(OrderStatus status, long ageSeconds) {
        return Order.builder()
                .id("order-1")
                .status(status)
                .createdAt(System.currentTimeMillis() - ageSeconds * 1000)
                .build();
    }

    @Test
    void pendingOrderPastConfirmedDelay_becomesConfirmed() {
        Order order = orderWithAge(OrderStatus.PENDING, CONFIRMED_DELAY + 1);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirmedOrderPastShippedDelay_becomesShipped() {
        Order order = orderWithAge(OrderStatus.CONFIRMED, CONFIRMED_DELAY + SHIPPED_DELAY + 1);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shippedOrderPastDeliveredDelay_becomesDelivered() {
        Order order = orderWithAge(
                OrderStatus.SHIPPED, CONFIRMED_DELAY + SHIPPED_DELAY + DELIVERED_DELAY + 1);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void cancelledOrder_isNeverModified() {
        Order order = orderWithAge(OrderStatus.CANCELLED, CONFIRMED_DELAY + SHIPPED_DELAY + DELIVERED_DELAY + 1000);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        verify(orderRepository, never()).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void deliveredOrder_isNeverModified() {
        Order order = orderWithAge(OrderStatus.DELIVERED, CONFIRMED_DELAY + SHIPPED_DELAY + DELIVERED_DELAY + 1000);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        verify(orderRepository, never()).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void oldPendingOrder_advancesOnlyOneStepPerRun_doesNotSkipStates() {
        Order order = orderWithAge(
                OrderStatus.PENDING, CONFIRMED_DELAY + SHIPPED_DELAY + DELIVERED_DELAY + 1000);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void pendingOrderBeforeConfirmedDelay_isNotChanged() {
        Order order = orderWithAge(OrderStatus.PENDING, CONFIRMED_DELAY - 1);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));

        simulationService.advanceOrderStatuses();

        verify(orderRepository, never()).save(order);
    }

    @Test
    void delaysComeFromConfiguration_notHardcoded() {
        Order order = orderWithAge(OrderStatus.PENDING, 5L);

        OrderStatusSimulationService shortDelayService =
                new OrderStatusSimulationService(orderRepository, 1L, SHIPPED_DELAY, DELIVERED_DELAY);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(order));
        shortDelayService.advanceOrderStatuses();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        Order otherOrder = orderWithAge(OrderStatus.PENDING, 5L);
        OrderStatusSimulationService longDelayService =
                new OrderStatusSimulationService(orderRepository, 1000L, SHIPPED_DELAY, DELIVERED_DELAY);
        when(orderRepository.findByStatusIn(anyList())).thenReturn(List.of(otherOrder));
        longDelayService.advanceOrderStatuses();
        assertThat(otherOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
