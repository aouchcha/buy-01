package service.orders.dto;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class OrdersResponse {
    private final String id;
    private final String userId;
    private final String fullName;
    private final String phoneNumber;
    private final String city;
    private final String address;
    private final String postalCode;
    private final String status;
    private final String paymentMethod;
    private final long createdAt;
    private final double totalAmount;
    private final List<OrderItemResponse> cartItems;

    @JsonCreator
    public OrdersResponse(
            @JsonProperty("id") String id,
            @JsonProperty("userId") String userId,
            @JsonProperty("fullName") String fullName,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("city") String city,
            @JsonProperty("address") String address,
            @JsonProperty("postalCode") String postalCode,
            @JsonProperty("status") String status,
            @JsonProperty("paymentMethod") String paymentMethod,
            @JsonProperty("createdAt") long createdAt,
            @JsonProperty("totalAmount") double totalAmount,
            @JsonProperty("cartItems") List<OrderItemResponse> cartItems) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.address = address;
        this.postalCode = postalCode;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.cartItems = cartItems;
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String fullName() {
        return fullName;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String city() {
        return city;
    }

    public String address() {
        return address;
    }

    public String postalCode() {
        return postalCode;
    }

    public String status() {
        return status;
    }

    public String paymentMethod() {
        return paymentMethod;
    }

    public long createdAt() {
        return createdAt;
    }

    public double totalAmount() {
        return totalAmount;
    }

    public List<OrderItemResponse> cartItems() {
        return cartItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdersResponse)) return false;
        OrdersResponse that = (OrdersResponse) o;
        return createdAt == that.createdAt && Double.compare(totalAmount, that.totalAmount) == 0
                && Objects.equals(id, that.id) && Objects.equals(userId, that.userId)
                && Objects.equals(fullName, that.fullName) && Objects.equals(phoneNumber, that.phoneNumber)
                && Objects.equals(city, that.city) && Objects.equals(address, that.address)
                && Objects.equals(postalCode, that.postalCode) && Objects.equals(status, that.status)
                && Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(cartItems, that.cartItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, fullName, phoneNumber, city, address, postalCode, status, paymentMethod,
                createdAt, totalAmount, cartItems);
    }

    @Override
    public String toString() {
        return "OrdersResponse[id=" + id + ", userId=" + userId + ", fullName=" + fullName + ", phoneNumber="
                + phoneNumber + ", city=" + city + ", address=" + address + ", postalCode=" + postalCode
                + ", status=" + status + ", paymentMethod=" + paymentMethod + ", createdAt=" + createdAt
                + ", totalAmount=" + totalAmount + ", cartItems=" + cartItems + "]";
    }
}
