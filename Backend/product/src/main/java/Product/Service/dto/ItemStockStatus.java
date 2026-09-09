package Product.Service.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ItemStockStatus {
    private final String productId;
    private final boolean success;
    private final int requestedQuantity;
    private final int availableStock;
    private final String message;

    @JsonCreator
    public ItemStockStatus(
            @JsonProperty("productId") String productId,
            @JsonProperty("success") boolean success,
            @JsonProperty("requestedQuantity") int requestedQuantity,
            @JsonProperty("availableStock") int availableStock,
            @JsonProperty("message") String message) {
        this.productId = productId;
        this.success = success;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
        this.message = message;
    }

    public String productId() {
        return productId;
    }

    public boolean success() {
        return success;
    }

    public int requestedQuantity() {
        return requestedQuantity;
    }

    public int availableStock() {
        return availableStock;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStockStatus)) return false;
        ItemStockStatus that = (ItemStockStatus) o;
        return success == that.success && requestedQuantity == that.requestedQuantity
                && availableStock == that.availableStock && Objects.equals(productId, that.productId)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, success, requestedQuantity, availableStock, message);
    }

    @Override
    public String toString() {
        return "ItemStockStatus[productId=" + productId + ", success=" + success + ", requestedQuantity="
                + requestedQuantity + ", availableStock=" + availableStock + ", message=" + message + "]";
    }
}
