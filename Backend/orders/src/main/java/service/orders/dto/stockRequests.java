package service.orders.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class stockRequests {
    private final String productId;
    private final int quantity;

    @JsonCreator
    public stockRequests(@JsonProperty("productId") String productId, @JsonProperty("quantity") int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof stockRequests)) return false;
        stockRequests that = (stockRequests) o;
        return quantity == that.quantity && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity);
    }

    @Override
    public String toString() {
        return "stockRequests[productId=" + productId + ", quantity=" + quantity + "]";
    }
}
