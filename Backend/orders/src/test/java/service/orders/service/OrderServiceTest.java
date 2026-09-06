package service.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import service.orders.client.ProductClient;
import service.orders.dto.Analytics;
import service.orders.repository.OrderRepository;
import service.orders.repository.OrderStatsRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String SELLER_ID = "seller-1";

    private OrderRepository orderRepository;
    private CartService cartService;
    private ProductClient productClient;
    private OrderStatsRepository orderStatsRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        cartService = mock(CartService.class);
        productClient = mock(ProductClient.class);
        orderStatsRepository = mock(OrderStatsRepository.class);
        orderService = new OrderService(orderRepository, cartService, productClient, orderStatsRepository);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        SELLER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER"))));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAnalytics_forSeller_usesOrderStatsRepositoryForRevenue() {
        when(orderStatsRepository.getBestSellingProducts(anyString(), anyLong(), anyInt())).thenReturn(List.of());
        when(orderStatsRepository.getSellerRevenue(anyString(), anyLong())).thenReturn(1234.5);

        Analytics analytics = orderService.getAnalytics("month");

        assertThat(analytics.getTotal()).isEqualTo(1234.5);
        verify(orderStatsRepository).getSellerRevenue(eq(SELLER_ID), anyLong());
    }

    @Test
    void getAnalytics_forSeller_whenNoDeliveredOrders_returnsZero() {
        when(orderStatsRepository.getBestSellingProducts(anyString(), anyLong(), anyInt())).thenReturn(List.of());
        when(orderStatsRepository.getSellerRevenue(anyString(), anyLong())).thenReturn(0.0);

        Analytics analytics = orderService.getAnalytics("today");

        assertThat(analytics.getTotal()).isZero();
    }
}
