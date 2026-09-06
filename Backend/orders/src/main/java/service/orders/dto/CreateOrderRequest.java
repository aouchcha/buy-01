package service.orders.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import service.orders.models.PaymentMethod;

    public record CreateOrderRequest(
            @NotNull(message = "shippingAddress is required")
            @Valid ShippingAddressRequest shippingAddress,

            @NotNull(message = "paymentMethod is required") PaymentMethod paymentMethod) {
    }
