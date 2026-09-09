package service.orders.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class OrderItemResponse {
    private final String productId;
    private final String productName;
    private final String sellerId;
    private final double price;
    private final int quantity;
    private final double totalPrice;

    @JsonCreator
    public OrderItemResponse(
            @JsonProperty("productId") String productId,
            @JsonProperty("productName") String productName,
            @JsonProperty("sellerId") String sellerId,
            @JsonProperty("price") double price,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("totalPrice") double totalPrice) {
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public String productId() {
        return productId;
    }

    public String productName() {
        return productName;
    }

    public String sellerId() {
        return sellerId;
    }

    public double price() {
        return price;
    }

    public int quantity() {
        return quantity;
    }

    public double totalPrice() {
        return totalPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemResponse)) return false;
        OrderItemResponse that = (OrderItemResponse) o;
        return Double.compare(price, that.price) == 0 && quantity == that.quantity
                && Double.compare(totalPrice, that.totalPrice) == 0 && Objects.equals(productId, that.productId)
                && Objects.equals(productName, that.productName) && Objects.equals(sellerId, that.sellerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, sellerId, price, quantity, totalPrice);
    }

    @Override
    public String toString() {
        return "OrderItemResponse[productId=" + productId + ", productName=" + productName + ", sellerId=" + sellerId
                + ", price=" + price + ", quantity=" + quantity + ", totalPrice=" + totalPrice + "]";
    }
}
