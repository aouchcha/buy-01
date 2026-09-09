package service.orders.dto;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import service.orders.models.PaymentMethod;

public final class CreateOrderRequest {

    @NotNull(message = "shippingAddress is required")
    @Valid
    private final ShippingAddressRequest shippingAddress;

    @NotNull(message = "paymentMethod is required")
    private final PaymentMethod paymentMethod;

    @JsonCreator
    public CreateOrderRequest(
            @JsonProperty("shippingAddress") ShippingAddressRequest shippingAddress,
            @JsonProperty("paymentMethod") PaymentMethod paymentMethod) {
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }

    public ShippingAddressRequest shippingAddress() {
        return shippingAddress;
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateOrderRequest)) return false;
        CreateOrderRequest that = (CreateOrderRequest) o;
        return Objects.equals(shippingAddress, that.shippingAddress) && paymentMethod == that.paymentMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shippingAddress, paymentMethod);
    }

    @Override
    public String toString() {
        return "CreateOrderRequest[shippingAddress=" + shippingAddress + ", paymentMethod=" + paymentMethod + "]";
    }
}
