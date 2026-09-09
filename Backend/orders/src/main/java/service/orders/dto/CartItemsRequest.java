package service.orders.dto;

import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CartItemsRequest {

    @NotBlank(message = "productId is required")
    private final String productId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private final Integer quantity;

    @JsonCreator
    public CartItemsRequest(@JsonProperty("productId") String productId, @JsonProperty("quantity") Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String productId() {
        return productId;
    }

    public Integer quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItemsRequest)) return false;
        CartItemsRequest that = (CartItemsRequest) o;
        return Objects.equals(productId, that.productId) && Objects.equals(quantity, that.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity);
    }

    @Override
    public String toString() {
        return "CartItemsRequest[productId=" + productId + ", quantity=" + quantity + "]";
    }
}
